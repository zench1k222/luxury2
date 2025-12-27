package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.*;
import dev.luxury.ui.AutoSellUI;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.AutoSellManager;
import dev.luxury.utils.notifications.NotificationsManager;
import dev.luxury.utils.player.InventoryUtil;
import dev.luxury.utils.player.ServerUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "AutoSell",
        desc = "Автоматическая продажа предметов на аукционе",
        category = Category.Player
)
public class AutoSell extends Module {

    public final SliderSetting sellDelay = new SliderSetting("Задержка (мс)", 100, 50, 500, 50);
    public final BooleanSetting debugMode = new BooleanSetting("Отладка", false);
    public final KeySetting toggleKey = new KeySetting("Вкл/Выкл", GLFW.GLFW_KEY_M);
    public final BooleanSetting autoStart = new BooleanSetting("Авто-старт", false);
    public final BooleanSetting sellEnabled = new BooleanSetting("Продажа включена", true);
    public final BooleanSetting hotbarReplacement = new BooleanSetting("Замена хотбара", true);
    public final SliderSetting priceMargin = new SliderSetting("Наценка %", 10, 0, 100, 5);
    public final ButtonSetting openUiButton = new ButtonSetting("Открыть Интерфейс", () -> {
        if (mc.world != null && mc.player != null) {
            mc.setScreen(new AutoSellUI(this));
        }
    }, 100, 20);

    public boolean active = false;
    public long lastActionTime = 0;
    private final List<SellItem> sellItems = new ArrayList<>();
    private final List<ItemStack> itemsToSell = new ArrayList<>();

    String userName = DiscordRPC.instance.info.userName();
    boolean isDev = userName != null &&
            ("krasivih".equals(userName) || "_znchkx_".equals(userName) || "webimmortal".equals(userName));

    public AutoSell() {
        if (isDev) {
            addSettings(sellDelay, debugMode, toggleKey, autoStart, sellEnabled, hotbarReplacement, priceMargin, openUiButton);
        } else {
            addSettings(sellDelay, toggleKey, autoStart, sellEnabled, hotbarReplacement, priceMargin, openUiButton);
        }

        AutoSellManager.init();

        AutoSellManager manager = AutoSellManager.getInstance();
        if (manager != null) {
            List<AutoSellUI.SellItem> uiItems = manager.loadAutoSellItems();
            if (uiItems != null && !uiItems.isEmpty()) {
                List<SellItem> convertedItems = new ArrayList<>();
                for (AutoSellUI.SellItem item : uiItems) {
                    convertedItems.add(new SellItem(item.id, item.sellPrice, item.enabled));
                }
                setSellItems(convertedItems);
            }
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
                ChatUtil.sendError("AutoSell выключен");
            }
            return;
        }

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS && active) {
            disableModule();
            return;
        }

        if (!active) return;

        long now = System.currentTimeMillis();

        if (now - lastActionTime < sellDelay.getIntValue()) {
            return;
        }

        if (itemsToSell.isEmpty()) {
            scanInventoryForSellItems();
            if (itemsToSell.isEmpty()) {
                if (debugMode.get()) {
                    ChatUtil.sendChat("§eНет предметов для продажи");
                }
                disableModule();
                return;
            }
        }

        processNextItem();
    }

    private void enableModule() {
        active = true;
        lastActionTime = 0;
        itemsToSell.clear();
        scanInventoryForSellItems();
        ChatUtil.sendChat("§aAutoSell включен! Найдено предметов для продажи: " + itemsToSell.size());
    }

    private void disableModule() {
        active = false;
        itemsToSell.clear();
        ChatUtil.sendChat("§cAutoSell выключен");
    }

    private void scanInventoryForSellItems() {
        itemsToSell.clear();

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            for (SellItem sellItem : sellItems) {
                if (sellItem.enabled && isItemIdMatch(stack, sellItem.id)) {
                    itemsToSell.add(stack.copy());
                    break;
                }
            }
        }

        if (debugMode.get()) {
            ChatUtil.sendChat("§eНайдено предметов для продажи: " + itemsToSell.size());
        }
    }

    private boolean isItemIdMatch(ItemStack stack, String targetId) {
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
        return false;
    }

    private void processNextItem() {
        if (itemsToSell.isEmpty()) return;

        ItemStack currentItem = itemsToSell.get(0);

        int emptyHotbarSlot = findEmptyHotbarSlot();
        if (emptyHotbarSlot == -1 && hotbarReplacement.get()) {
            emptyHotbarSlot = findReplaceableHotbarSlot();
        }

        if (emptyHotbarSlot == -1) {
            ChatUtil.sendError("Нет свободного слота в хотбаре!");
            disableModule();
            return;
        }

        int inventorySlot = findItemInInventory(currentItem);
        if (inventorySlot == -1) {
            itemsToSell.remove(0);
            if (debugMode.get()) {
                ChatUtil.sendChat("§cПредмет не найден в инвентаре: " + currentItem.getName().getString());
            }
            lastActionTime = System.currentTimeMillis();
            return;
        }

        moveItemToHotbar(inventorySlot, emptyHotbarSlot);

        for (SellItem sellItem : sellItems) {
            if (isItemIdMatch(currentItem, sellItem.id)) {
                long price = calculatePrice(sellItem.sellPrice, currentItem.getCount());

                new Thread(() -> {
                    try {
                        Thread.sleep(sellDelay.getIntValue());

                        if (mc.player != null && active) {
                            String command = "/ah sell " + price;
                            mc.player.networkHandler.sendChatMessage(command);

                            if (debugMode.get()) {
                                ChatUtil.sendChat("§aОтправляю команду: " + command);
                            }

                            NotificationsManager.getInstance().addNotification(
                                    String.format("§aВыставлен на продажу: %s x%d за %d монет",
                                            currentItem.getName().getString(),
                                            currentItem.getCount(),
                                            price),
                                    3000
                            );

                            itemsToSell.remove(0);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                break;
            }
        }

        lastActionTime = System.currentTimeMillis();
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findReplaceableHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                boolean isSellItem = false;
                for (SellItem sellItem : sellItems) {
                    if (isItemIdMatch(stack, sellItem.id)) {
                        isSellItem = true;
                        break;
                    }
                }
                if (!isSellItem) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findItemInInventory(ItemStack target) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, target)) {
                return i;
            }
        }
        return -1;
    }

    private void moveItemToHotbar(int fromSlot, int toSlot) {
        if (mc.interactionManager != null) {
            ScreenHandler handler = mc.player.currentScreenHandler;

            mc.interactionManager.clickSlot(
                    handler.syncId,
                    fromSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            mc.interactionManager.clickSlot(
                    handler.syncId,
                    toSlot < 9 ? toSlot + 36 : toSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            if (debugMode.get()) {
                ChatUtil.sendChat("§eПеремещаю предмет в хотбар слот " + (toSlot + 1));
            }
        }
    }

    private long calculatePrice(long basePrice, int count) {
        long total = basePrice * count;
        double margin = priceMargin.getValue() / 100.0;
        return (long) (total * (1 + margin));
    }

    public List<SellItem> getSellItems() {
        return sellItems;
    }

    public void setSellItems(List<SellItem> items) {
        sellItems.clear();
        sellItems.addAll(items);
        saveItemsToConfig();
    }

    public void addSellItem(String id, long sellPrice, boolean enabled) {
        sellItems.add(new SellItem(id, sellPrice, enabled));
        saveItemsToConfig();
    }

    public void removeSellItem(int index) {
        if (index >= 0 && index < sellItems.size()) {
            sellItems.remove(index);
            saveItemsToConfig();
        }
    }

    private void saveItemsToConfig() {
        AutoSellManager manager = AutoSellManager.getInstance();
        if (manager != null) {
            List<AutoSellUI.SellItem> uiItems = new ArrayList<>();
            for (SellItem item : sellItems) {
                if (item.sellPrice > Integer.MAX_VALUE) {
                    ChatUtil.sendError("Цена " + item.sellPrice + " слишком большая для сохранения!");
                    continue;
                }
                uiItems.add(new AutoSellUI.SellItem(item.id, (int) item.sellPrice, item.enabled));
            }
            manager.saveAutoSellItems(uiItems);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoStart.get()) {
            enableModule();
        } else {
            ChatUtil.sendChat("§eAutoSell включен. Нажмите " +
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

    public static class SellItem {
        public String id;
        public long sellPrice;
        public boolean enabled;

        public SellItem(String id, long sellPrice, boolean enabled) {
            this.id = id;
            this.sellPrice = sellPrice;
            this.enabled = enabled;
        }
    }
}