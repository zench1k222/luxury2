package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.*;
import dev.luxury.modules.impl.render.ClientSounds;
import dev.luxury.ui.AutoBuyUI;
import dev.luxury.ui.AutoBuyUI3;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.AutoBuyManager;
import dev.luxury.utils.notifications.NotificationsManager;
import dev.luxury.utils.player.InventoryUtil;
import dev.luxury.utils.player.ServerUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleAnnotation(
        name = "AutoBuy",
        desc = "Автоматическая покупка предметов с аукциона",
        category = Category.Misc
)
public class AutoBuy extends Module {

    public final SliderSetting delay = new SliderSetting("Задержка (мс)", 100, 50, 500, 50);
    public final BooleanSetting debugMode = new BooleanSetting("Отладка", false);
    public final KeySetting toggleKey = new KeySetting("Вкл/Выкл", GLFW.GLFW_KEY_N);
    public final BooleanSetting autoStart = new BooleanSetting("Авто-старт", false);
    public final BooleanSetting buyEnabled = new BooleanSetting("Покупка включена", true);
    public final SliderSetting refreshDelay = new SliderSetting("Задержка обновления", 500, 100, 2000, 100);
    public final ButtonSetting openUiButton = new ButtonSetting("Открыть Интерфейс", () -> {
        if (mc.world != null && mc.player != null) {
            mc.setScreen(new AutoBuyUI3(this));
        }
    }, 100, 20);

    public boolean active = false;
    public boolean inAuction = false;
    public boolean waitingForRefresh = false;
    public boolean justOpenedAuction = false;
    public int refreshSlot = 4;
    public long lastActionTime = 0;
    public int currentPage = 1;
    public long lastOpenTime = 0;
    private long purchaseConfirmationTime = 0;
    private boolean waitingForConfirmation = false;
    private Runnable pendingConfirmationAction = null;


    public static final List<BuyItem> buyItems = new ArrayList<>();

    private static final Pattern PRICE_PATTERN = Pattern.compile("Цена:\\s*([\\d,.]+)");
    private static final Pattern PRICE_PATTERN_EN = Pattern.compile("Price:\\s*([\\d,.]+)");
    private static final Pattern PRICE_PATTERN_SIMPLE = Pattern.compile("([\\d,.]+)\\s*монет");

    String userName = DiscordRPC.instance.info.userName();
    boolean isDev = userName != null &&
            ("krasivih".equals(userName) || "_znchkx_".equals(userName) || "webimmortal".equals(userName));

    public AutoBuy() {
        if (isDev) {
            addSettings(delay, debugMode, toggleKey, autoStart, buyEnabled, refreshDelay, openUiButton);
        }
        if (!isDev) {
            addSettings(delay, toggleKey, autoStart, buyEnabled, refreshDelay, openUiButton);
        }
        AutoBuyManager.init();

        AutoBuyManager manager = AutoBuyManager.getInstance();
        if (manager != null) {
            List<AutoBuyUI.BuyItem> uiItems = manager.loadAutoBuyItems();
            if (uiItems != null && !uiItems.isEmpty()) {
                List<AutoBuy.BuyItem> convertedItems = new ArrayList<>();
                for (AutoBuyUI.BuyItem item : uiItems) {
                    convertedItems.add(new AutoBuy.BuyItem(item.id, item.maxPricePerUnit, item.quantity, item.enabled));
                }
                setBuyItems(convertedItems);
            }
        }
    }

    private void saveItemsToConfig() {
        AutoBuyManager manager = AutoBuyManager.getInstance();
        if (manager != null) {
            List<AutoBuyUI.BuyItem> uiItems = new ArrayList<>();
            for (BuyItem item : buyItems) {
                if (item.maxPricePerUnit > Integer.MAX_VALUE) {
                    ChatUtil.sendError("Цена " + item.maxPricePerUnit + " слишком большая для сохранения!");
                    continue;
                }
                uiItems.add(new AutoBuyUI.BuyItem(item.id, (int) item.maxPricePerUnit, item.quantity, item.enabled));
            }
            manager.saveAutoBuyItems(uiItems);
        }
    }


    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (!ServerUtil.isConnected("reallyworld") && !ServerUtil.isConnected("playrw")) {
            if (active) disableModule();
            return;
        }

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), toggleKey.getValue()) == GLFW.GLFW_PRESS) {
            if (!active) {
                enableModule();
            } else {
                disableModule();
                ChatUtil.sendError("АвтоБай выключен");
            }
            return;
        }

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS && active) {
            disableModule();
            return;
        }

        if (!active) return;

        long now = System.currentTimeMillis();

        if (justOpenedAuction) {
            if (now - lastOpenTime > 1000) {
                justOpenedAuction = false;
            }
            return;
        }

        if (waitingForRefresh) {
            if (now - lastActionTime > refreshDelay.getIntValue()) {
                waitingForRefresh = false;
            }
            return;
        }

        if (!inAuction) {
            if (now - lastActionTime > 1500) {
                openAuction();
                lastActionTime = now;
            }
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
            handleAuctionGUI(screen);
        } else {
            inAuction = false;
            currentPage = 1;
        }
        if (waitingForConfirmation && System.currentTimeMillis() - purchaseConfirmationTime >= 400) {
            waitingForConfirmation = false;
            if (pendingConfirmationAction != null) {
                pendingConfirmationAction.run();
                pendingConfirmationAction = null;
            }
        }
    }

    private void enableModule() {
        active = true;
        inAuction = false;
        waitingForRefresh = false;
        justOpenedAuction = false;
        lastActionTime = 0;
        currentPage = 1;
        lastOpenTime = 0;
        ChatUtil.sendChat("§aAutoBuy включен!");
    }

    private void disableModule() {
        active = false;
        inAuction = false;
        waitingForRefresh = false;
        justOpenedAuction = false;
        currentPage = 1;
        if (mc.currentScreen != null && mc.player != null) {
            mc.player.closeHandledScreen();
        }
    }

    private void openAuction() {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand("ah");
            inAuction = true;
            justOpenedAuction = true;
            lastOpenTime = System.currentTimeMillis();
            currentPage = 1;
            if (debugMode.get()) {
                ChatUtil.sendChat("§eОткрываю аукцион...");
            }
        }
    }

    private void handleAuctionGUI(GenericContainerScreen screen) {
        if (screen.getScreenHandler().slots.size() < 53) {
            if (debugMode.get()) {
                ChatUtil.sendChat("§cНедостаточно слотов: " + screen.getScreenHandler().slots.size());
            }
            return;
        }

        if (waitingForConfirmation) {
            if (System.currentTimeMillis() - purchaseConfirmationTime >= 400) {
                waitingForConfirmation = false;
                if (pendingConfirmationAction != null) {
                    pendingConfirmationAction.run();
                    pendingConfirmationAction = null;
                }
            }
            return;
        }

        boolean foundItem = false;

        for (int i = 0; i < 53; i++) {
            if (i == refreshSlot) continue;

            ItemStack stack = screen.getScreenHandler().getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            String itemName = stack.getName().getString();
            if (itemName.trim().isEmpty()) continue;

            if (shouldSkipItem(itemName)) continue;

            int stackCount = stack.getCount();

            boolean shouldBuy = false;
            long targetPricePerUnit = 0;
            int requiredQuantity = 0;

            for (BuyItem buyItem : buyItems) {
                if (buyItem.enabled && isItemIdMatch(itemName, stack, buyItem.id)) {
                    if (stackCount >= buyItem.quantity) {
                        shouldBuy = true;
                        targetPricePerUnit = buyItem.maxPricePerUnit;
                        requiredQuantity = buyItem.quantity;
                        break;
                    }
                }
            }

            if (!shouldBuy) continue;

            List<Text> tooltip = getItemTooltip(stack);
            if (tooltip.isEmpty()) continue;

            long lotPrice = findPriceInTooltip(tooltip);

            if (lotPrice > 0) {
                long pricePerUnitInLot = lotPrice / stackCount;

                if (pricePerUnitInLot <= targetPricePerUnit) {
                    long maxPriceForRequiredQuantity = (long) targetPricePerUnit * requiredQuantity;
                    long actualPriceForRequiredQuantity = pricePerUnitInLot * requiredQuantity;

                    if (actualPriceForRequiredQuantity <= maxPriceForRequiredQuantity) {
                        InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);

                        // уведомления
                        ChatUtil.sendChat(String.format("§aКуплено: %s x%d за %d монет (за штуку: %d)",
                                itemName, stackCount, lotPrice, pricePerUnitInLot));
                        NotificationsManager.getInstance().addNotification(String.format("§aКуплено: %s x%d за %d монет (за штуку: %d)",
                                itemName, stackCount, lotPrice, pricePerUnitInLot), 3000);
                        ClientSounds.instance.playEnableSound();

                        purchaseConfirmationTime = System.currentTimeMillis();
                        waitingForConfirmation = true;
                        pendingConfirmationAction = () -> {
                            handlePurchaseConfirmation();
                        };

                        foundItem = true;
                        break;
                    } else if (debugMode.get()) {
                        ChatUtil.sendChat(String.format("§cЦена за %d шт %d > лимита %d для: %s",
                                requiredQuantity, actualPriceForRequiredQuantity,
                                maxPriceForRequiredQuantity, itemName));
                    }
                } else if (debugMode.get()) {
                    ChatUtil.sendChat(String.format("§cЦена за штуку %d > лимита %d для: %s",
                            pricePerUnitInLot, targetPricePerUnit, itemName));
                }
            } else if (debugMode.get()) {
                ChatUtil.sendChat("§cНе найдена цена для: " + itemName);
            }
        }

        if (!foundItem && !waitingForRefresh && !waitingForConfirmation) {
            refreshPage(screen);
        }
    }

    private void scheduleTask(Runnable task, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void refreshPage(GenericContainerScreen screen) {
        if (screen.getScreenHandler().slots.size() > refreshSlot) {
            ItemStack refreshItem = screen.getScreenHandler().getSlot(refreshSlot).getStack();
            if (!refreshItem.isEmpty()) {
                InventoryUtil.clickSlot(refreshSlot, 0, SlotActionType.PICKUP);
                waitingForRefresh = true;
                lastActionTime = System.currentTimeMillis();
                currentPage++;

                if (debugMode.get()) {
                    ChatUtil.sendChat("§eОбновляю страницу " + currentPage + "...");
                }
            } else if (debugMode.get()) {
                ChatUtil.sendChat("§cСлот обновления пуст! Закрываю аукцион.");
                inAuction = false;
            }
        }
    }

    private long findPriceInTooltip(List<Text> tooltip) {
        for (Text text : tooltip) {
            String line = text.getString();

            if (line.contains("Цена:") || line.contains("Price:")) {
                long price = extractPriceFromLine(line);
                if (price > 0) {
                    if (debugMode.get()) {
                        ChatUtil.sendChat("§7  Найдена цена: " + price + " в строке: " + Formatting.strip(line));
                    }
                    return price;
                }
            }
        }

        for (Text text : tooltip) {
            String line = text.getString();
            long price = extractPriceFromLine(line);
            if (price > 0) {
                if (debugMode.get()) {
                    ChatUtil.sendChat("§7  Найдена цена (общий поиск): " + price + " в строке: " + Formatting.strip(line));
                }
                return price;
            }
        }

        return -1;
    }

    private void handlePurchaseConfirmation() {
        if (mc.currentScreen instanceof GenericContainerScreen chestScreen) {
            ScreenHandler handler = chestScreen.getScreenHandler();

            Slot slot = handler.slots.get(20);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                Text name = stack.getName();
                if (name.getString().contains("Купить") || name.getString().contains("Подтвердить")) {
                    clickSlot(20);
                }
            }
        }
    }

    private void clickSlot(int slotIndex) {
        if (mc.interactionManager != null && mc.player != null) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            mc.interactionManager.clickSlot(
                    handler.syncId,
                    slotIndex,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );
        }
    }


    private List<Text> getItemTooltip(ItemStack stack) {
        try {
            Item.TooltipContext context = Item.TooltipContext.create(mc.world.getRegistryManager());
            net.minecraft.item.tooltip.TooltipType tooltipType = net.minecraft.item.tooltip.TooltipType.BASIC;
            return stack.getTooltip(context, mc.player, tooltipType);
        } catch (Exception e) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(stack.getName());
            return tooltip;
        }
    }

    private boolean shouldSkipItem(String itemName) {
        if (itemName.isEmpty()) return true;

        String lower = itemName.toLowerCase();
        return lower.contains("ваши товары") ||
                lower.contains("завершенные товары") ||
                lower.contains("следующая страница") ||
                lower.contains("предыдущая страница") ||
                lower.contains("помощь по аукциону") ||
                lower.contains("как продать товар") ||
                lower.contains("сортировка") ||
                lower.contains("категории") ||
                lower.contains("search") ||
                lower.contains("next page") ||
                lower.contains("previous page") ||
                lower.contains("назад") ||
                lower.contains("refresh") ||
                lower.contains("обновить") ||
                lower.contains("auction help") ||
                lower.contains("как пользоваться") ||
                lower.contains("sort") ||
                lower.contains("category") ||
                lower.contains("фильтр") ||
                lower.contains("filter");
    }

    private long extractPriceFromLine(String line) {
        try {
            String cleanLine = Formatting.strip(line);
            if (cleanLine == null) {
                cleanLine = line.replaceAll("§[0-9a-fk-or]", "");
            }

            cleanLine = cleanLine.trim();

            if (debugMode.get() && (cleanLine.contains("Цена:") || cleanLine.contains("Price:") || cleanLine.matches(".*\\d+.*"))) {
                ChatUtil.sendChat("§7Проверяю строку: '" + cleanLine + "'");
            }

            long price = tryPattern(PRICE_PATTERN, cleanLine);
            if (price > 0) return price;

            price = tryPattern(PRICE_PATTERN_EN, cleanLine);
            if (price > 0) return price;

            price = tryPattern(PRICE_PATTERN_SIMPLE, cleanLine);
            if (price > 0) return price;

            if (cleanLine.contains("Цена:")) {
                String afterPrice = cleanLine.substring(cleanLine.indexOf("Цена:") + 5).trim();
                Matcher matcher = Pattern.compile("([\\d,.]+)").matcher(afterPrice);
                if (matcher.find()) {
                    String priceStr = matcher.group(1).replace(",", "").replace(".", "").trim();
                    if (!priceStr.isEmpty()) {
                        return Long.parseLong(priceStr);
                    }
                }
            }

            if (cleanLine.contains("Price:")) {
                String afterPrice = cleanLine.substring(cleanLine.indexOf("Price:") + 6).trim();
                Matcher matcher = Pattern.compile("([\\d,.]+)").matcher(afterPrice);
                if (matcher.find()) {
                    String priceStr = matcher.group(1).replace(",", "").replace(".", "").trim();
                    if (!priceStr.isEmpty()) {
                        return Long.parseLong(priceStr);
                    }
                }
            }

            Matcher numberMatcher = Pattern.compile("([\\d,.]+)").matcher(cleanLine);
            if (numberMatcher.find()) {
                String priceStr = numberMatcher.group(1).replace(",", "").replace(".", "").trim();
                if (!priceStr.isEmpty() && priceStr.length() <= 30) {
                    long parsedPrice = Long.parseLong(priceStr);
                    if (parsedPrice >= 10) {
                        return parsedPrice;
                    }
                }
            }

        } catch (NumberFormatException e) {
            if (debugMode.get()) {
                ChatUtil.sendChat("§cОшибка парсинга цены из: " + line + " (" + e.getMessage() + ")");
            }
        }
        return -1;
    }

    private long tryPattern(Pattern pattern, String line) {
        try {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", "").replace(".", "").trim();
                if (!priceStr.isEmpty()) {
                    return Long.parseLong(priceStr);
                }
            }
        } catch (Exception e) {
            if (debugMode.get()) {
                ChatUtil.sendChat("§cОшибка в tryPattern: " + e.getMessage());
            }
        }
        return -1;
    }

    private boolean isItemIdMatch(String itemName, ItemStack stack, String targetId) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId != null) {
            String fullId = itemId.toString();
            String shortId = itemId.getPath();

            if (fullId.equalsIgnoreCase(targetId) ||
                    fullId.equalsIgnoreCase("minecraft:" + targetId) ||
                    shortId.equalsIgnoreCase(targetId)) {
                return true;
            }
        }

        return itemName.toLowerCase().contains(targetId.toLowerCase());
    }

    public List<BuyItem> getBuyItems() {
        return buyItems;
    }

    public void setBuyItems(List<BuyItem> items) {
        buyItems.clear();
        buyItems.addAll(items);
        saveItemsToConfig();
    }

    public void addBuyItem(String id, long maxPricePerUnit, int quantity, boolean enabled) {
        buyItems.add(new BuyItem(id, maxPricePerUnit, quantity, enabled));
        saveItemsToConfig();
    }

    public void removeBuyItem(int index) {
        if (index >= 0 && index < buyItems.size()) {
            buyItems.remove(index);
            saveItemsToConfig();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoStart.get()) {
            enableModule();
        } else {
            ChatUtil.sendChat("§eAutoBuy включен. Нажмите " +
                    GLFW.glfwGetKeyName(toggleKey.getValue(), 0) + " для старта.");
        }

        if (!ServerUtil.isConnected("reallyworld") && !ServerUtil.isConnected("playrw")) {
            disable();
            ChatUtil.sendError("Работает только на ReallyWorld");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        disableModule();
    }

    public static class BuyItem {
        public String id;
        public long maxPricePerUnit;
        public int quantity;
        public boolean enabled;

        public BuyItem(String id, long maxPricePerUnit, int quantity, boolean enabled) {
            this.id = id;
            this.maxPricePerUnit = maxPricePerUnit;
            this.quantity = quantity;
            this.enabled = enabled;
        }
    }
}
