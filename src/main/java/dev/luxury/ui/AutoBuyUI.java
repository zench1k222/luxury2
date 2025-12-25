package dev.luxury.ui;

import dev.luxury.modules.impl.AutoBuy;
import dev.luxury.utils.managers.ConfigManager;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.luxury.modules.impl.targetesp.mode.Circle.mc;

public class AutoBuyUI extends Screen {

    private final AutoBuy autoBuyModule;
    private final List<AutoBuy.BuyItem> buyItems;

    private float scrollOffset = 0;
    private boolean draggingScroll = false;
    private float lastMouseY = 0;

    private float panelX, panelY, panelWidth, panelHeight;
    private float scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight;
    private float scrollbarThumbY = 0;
    private float scrollbarThumbHeight = 0;

    private String newItemId = "";
    private String newItemPrice = "";
    private boolean addingNewItem = false;
    private int editingItemIndex = -1;
    private String newItemQuantity = "";

    private boolean typingInIdField = true;
    private boolean typingInPriceField = false;
    private boolean typingInQuantityField = false;

    public AutoBuyUI(AutoBuy autoBuyModule) {
        super(Text.of("AutoBuy UI"));
        this.autoBuyModule = autoBuyModule;
        this.buyItems = autoBuyModule.getBuyItems();
    }

    private String formatPrice(long price) {
        if (price >= 1_000_000_000L) {
            return String.format("%.1fм", price / 1_000_000_000.0);
        } else if (price >= 1_000_000L) {
            return String.format("%.1fкк", price / 1_000_000.0);
        } else if (price >= 1_000L) {
            return String.format("%.1fk", price / 1_000.0);
        } else {
            return String.valueOf(price);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float screenWidth = this.width;
        float screenHeight = this.height;

        panelWidth = Math.min(500, screenWidth - 40);
        panelHeight = Math.min(600, screenHeight - 40);
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        Color primaryColor = new Color(75, 35, 90);
        Color secondaryColor = new Color(45, 27, 105);
        Color accentColor = new Color(212, 175, 55);
        Color backgroundColor = new Color(20, 20, 30, 230);
        Color panelColor = new Color(30, 20, 40, 240);

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(15), panelColor.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(15), accentColor.getRGB(), 2.0f, 1.0f, 1.0f, false);

        FontHelper.monsterrat[24].drawCenteredString(context, "AutoBuy - Управление предметами",
                panelX + panelWidth / 2, panelY + 20, accentColor.getRGB());

        FontHelper.monsterrat[14].drawCenteredString(context, "Добавляйте предметы для автоматической покупки",
                panelX + panelWidth / 2, panelY + 45, Color.WHITE.getRGB());

        float listX = panelX + 20;
        float listY = panelY + 70;
        float listWidth = panelWidth - 40;
        float listHeight = panelHeight - 220;

        RenderUtil.drawRoundedRect(context.getMatrices(), listX, listY, listWidth, listHeight,
                new Vector4f(10, 10, 10, 10), new Color(10, 5, 20, 200).getRGB());

        float iconX = listX + 15;
        float col1X = listX + 40;
        float col2X = listX + listWidth - 240;
        float col3X = listX + listWidth - 170;
        float col4X = listX + listWidth - 100;
        float col5X = listX + listWidth - 30;

        FontHelper.monsterrat[14].drawFontLeft(context, "Предмет", col1X, listY + 12, accentColor.getRGB());
        FontHelper.monsterrat[14].drawFontLeft(context, "Цена/шт", col2X, listY + 12, accentColor.getRGB());
        FontHelper.monsterrat[14].drawFontLeft(context, "Кол-во", col3X, listY + 12, accentColor.getRGB());
        FontHelper.monsterrat[14].drawFontLeft(context, "Всего", col4X, listY + 12, accentColor.getRGB());
        FontHelper.monsterrat[14].drawFontLeft(context, "Статус", col5X, listY + 12, accentColor.getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(), listX, listY + 35, listWidth, 1,
                new Vector4f(0), accentColor.getRGB());

        float scrollAreaY = listY + 40;
        float scrollAreaHeight = listHeight - 45;

        RenderUtil.enableScissor((int)listX, (int)scrollAreaY, (int)listWidth, (int)scrollAreaHeight);

        float itemY = scrollAreaY - scrollOffset;
        int itemIndex = 0;

        for (AutoBuy.BuyItem item : buyItems) {
            if (itemY + 30 > scrollAreaY && itemY < scrollAreaY + scrollAreaHeight) {
                int rowColor = itemIndex % 2 == 0 ?
                        new Color(40, 30, 50, 120).getRGB() :
                        new Color(35, 25, 45, 80).getRGB();
                if (editingItemIndex == itemIndex) {
                    rowColor = new Color(45, 35, 75, 180).getRGB();
                }

                RenderUtil.drawRoundedRect(context.getMatrices(), listX + 5, itemY, listWidth - 10, 30,
                        new Vector4f(6), rowColor);

                Item mcItem = getItemFromId(item.id);
                if (mcItem != null && mcItem != Items.AIR) {
                    ItemStack stack = new ItemStack(mcItem, Math.min(item.quantity, 64));
                    RenderUtil.drawItemStack(context, stack, iconX, itemY + 7, 16);
                } else {
                    FontHelper.icons[14].drawFontLeft(context, "?", iconX + 4, itemY + 10, 0xFFFF5555);
                }

                FontHelper.monsterrat[14].drawFontLeft(context, item.id, col1X, itemY + 10, Color.WHITE.getRGB());

                String priceText = formatPrice(item.maxPricePerUnit) + "/шт";
                FontHelper.monsterrat[14].drawFontLeft(context, priceText, col2X, itemY + 10, 0xFFD4AF37);

                String quantityText = "x" + item.quantity;
                FontHelper.monsterrat[14].drawFontLeft(context, quantityText, col3X, itemY + 10, 0xFF00AAFF);

                long totalPrice = item.maxPricePerUnit * item.quantity;
                String totalPriceText = formatPrice(totalPrice);
                FontHelper.monsterrat[14].drawFontLeft(context, totalPriceText, col4X, itemY + 10, 0xFFAA00AA);

                String statusText = item.enabled ? "§a✓" : "§c✗";
                int statusColor = item.enabled ? 0xFF00FF00 : 0xFFFF0000;
                FontHelper.monsterrat[14].drawFontLeft(context, statusText, col5X, itemY + 10, statusColor);

                float editX = col5X - 30;
                int editColor = isMouseOverEditButton(mouseX, mouseY, editX, itemY) ? accentColor.getRGB() : 0xFF888888;
                FontHelper.icons[14].drawFontLeft(context, "Q", editX, itemY + 10, editColor);

                float deleteX = col5X + 20;
                int deleteColor = isMouseOverDeleteButton(mouseX, mouseY, deleteX, itemY) ? 0xFFFF5555 : 0xFFFF0000;
                FontHelper.icons[14].drawFontLeft(context, "R", deleteX, itemY + 10, deleteColor);
            }

            itemY += 35;
            itemIndex++;
        }

        RenderUtil.disableScissor();

        float totalContentHeight = buyItems.size() * 35;
        if (totalContentHeight > scrollAreaHeight) {
            scrollbarWidth = 6;
            scrollbarX = listX + listWidth - scrollbarWidth - 5;
            scrollbarY = scrollAreaY;
            scrollbarHeight = scrollAreaHeight;

            RenderUtil.drawRoundedRect(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight,
                    new Vector4f(3), new Color(0, 0, 0, 100).getRGB());

            float thumbHeight = Math.max(30, (scrollAreaHeight / totalContentHeight) * scrollAreaHeight);
            float maxScroll = Math.max(0, totalContentHeight - scrollAreaHeight);
            float scrollPercent = maxScroll > 0 ? scrollOffset / maxScroll : 0;
            scrollbarThumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollPercent;
            scrollbarThumbHeight = thumbHeight;

            int thumbColor = draggingScroll ? accentColor.getRGB() : 0xFFAAAAAA;
            if (isMouseOverScrollbar(mouseX, mouseY)) {
                thumbColor = accentColor.brighter().getRGB();
            }
            RenderUtil.drawRoundedRect(context.getMatrices(), scrollbarX, scrollbarThumbY, scrollbarWidth, scrollbarThumbHeight,
                    new Vector4f(3), thumbColor);
        }

        float editY = listY + listHeight + 15;

        if (addingNewItem || editingItemIndex != -1) {
            RenderUtil.drawRoundedRect(context.getMatrices(), listX, editY, listWidth, 120,
                    new Vector4f(10), new Color(45, 27, 105, 150).getRGB());

            String title = editingItemIndex != -1 ? "Редактирование предмета" : "Добавление нового предмета";
            FontHelper.monsterrat[16].drawCenteredString(context, title,
                    panelX + panelWidth / 2, editY + 10, Color.WHITE.getRGB());

            float fieldY = editY + 35;
            FontHelper.monsterrat[14].drawFontLeft(context, "ID предмета:", listX + 15, fieldY, Color.WHITE.getRGB());

            float inputWidth = listWidth - 180;
            float idInputX = listX + 100;
            int idFieldColor = typingInIdField ? new Color(212, 175, 55, 128).getRGB() : new Color(0, 0, 0, 128).getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), idInputX, fieldY - 2, inputWidth, 22,
                    new Vector4f(4), idFieldColor);

            String displayId = newItemId.isEmpty() ? "Введите ID предмета..." : newItemId;
            int idTextColor = newItemId.isEmpty() ? 0xFF666666 : Color.WHITE.getRGB();
            FontHelper.monsterrat[14].drawFontLeft(context, displayId, idInputX + 5, fieldY, idTextColor);

            if (typingInIdField) {
                float cursorX = idInputX + 5 + FontHelper.monsterrat[14].getWidth(newItemId);
                long cursorTime = System.currentTimeMillis() % 1000;
                if (cursorTime < 500) {
                    RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY, 1, 14,
                            new Vector4f(0), Color.WHITE.getRGB());
                }
            }

            fieldY += 28;
            FontHelper.monsterrat[14].drawFontLeft(context, "Цена за штуку:", listX + 15, fieldY, Color.WHITE.getRGB());

            float priceInputX = listX + 100;
            int priceFieldColor = typingInPriceField ? new Color(212, 175, 55, 128).getRGB() : new Color(0, 0, 0, 128).getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), priceInputX, fieldY - 2, inputWidth, 22,
                    new Vector4f(4), priceFieldColor);

            String displayPrice = newItemPrice.isEmpty() ? "Введите цену..." : newItemPrice;
            int priceTextColor = newItemPrice.isEmpty() ? 0xFF666666 : Color.WHITE.getRGB();
            FontHelper.monsterrat[14].drawFontLeft(context, displayPrice, priceInputX + 5, fieldY, priceTextColor);

            if (typingInPriceField) {
                float cursorX = priceInputX + 5 + FontHelper.monsterrat[14].getWidth(newItemPrice);
                long cursorTime = System.currentTimeMillis() % 1000;
                if (cursorTime < 500) {
                    RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY, 1, 14,
                            new Vector4f(0), Color.WHITE.getRGB());
                }
            }

            fieldY += 28;
            FontHelper.monsterrat[14].drawFontLeft(context, "Количество:", listX + 15, fieldY, Color.WHITE.getRGB());

            float quantityInputX = listX + 100;
            int quantityFieldColor = typingInQuantityField ? new Color(212, 175, 55, 128).getRGB() : new Color(0, 0, 0, 128).getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), quantityInputX, fieldY - 2, inputWidth, 22,
                    new Vector4f(4), quantityFieldColor);

            String displayQuantity = newItemQuantity.isEmpty() ? "Введите количество..." : newItemQuantity;
            int quantityTextColor = newItemQuantity.isEmpty() ? 0xFF666666 : Color.WHITE.getRGB();
            FontHelper.monsterrat[14].drawFontLeft(context, displayQuantity, quantityInputX + 5, fieldY, quantityTextColor);

            if (typingInQuantityField) {
                float cursorX = quantityInputX + 5 + FontHelper.monsterrat[14].getWidth(newItemQuantity);
                long cursorTime = System.currentTimeMillis() % 1000;
                if (cursorTime < 500) {
                    RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY, 1, 14,
                            new Vector4f(0), Color.WHITE.getRGB());
                }
            }

            float buttonY = editY + 120;
            float buttonWidth = 120;
            float cancelX = panelX + (panelWidth - buttonWidth * 2 - 20) / 2;
            float saveX = cancelX + buttonWidth + 20;

            int cancelColor = isMouseOverCancelButton(mouseX, mouseY, cancelX, buttonY) ?
                    new Color(178, 34, 34).getRGB() : new Color(139, 0, 0).getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), cancelX, buttonY, buttonWidth, 32,
                    new Vector4f(8), cancelColor);
            FontHelper.monsterrat[16].drawCenteredString(context, "Отмена", cancelX + buttonWidth / 2, buttonY + 9, Color.WHITE.getRGB());

            String saveText = editingItemIndex != -1 ? "Сохранить" : "Добавить";
            int saveColor = isMouseOverSaveButton(mouseX, mouseY, saveX, buttonY) ?
                    new Color(50, 205, 50).getRGB() : new Color(0, 100, 0).getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), saveX, buttonY, buttonWidth, 32,
                    new Vector4f(8), saveColor);
            FontHelper.monsterrat[16].drawCenteredString(context, saveText, saveX + buttonWidth / 2, buttonY + 9, Color.WHITE.getRGB());

        } else {
            float addButtonY = editY;
            float addButtonWidth = 220;
            float addButtonX = panelX + (panelWidth - addButtonWidth) / 2;

            int addButtonColor = isMouseOverAddButton(mouseX, mouseY, addButtonX, addButtonY) ?
                    primaryColor.brighter().getRGB() : primaryColor.getRGB();
            RenderUtil.drawRoundedRect(context.getMatrices(), addButtonX, addButtonY, addButtonWidth, 40,
                    new Vector4f(10), addButtonColor);

            FontHelper.icons[20].drawCenteredString(context, "+", addButtonX + 25, addButtonY + 10, Color.WHITE.getRGB());

            FontHelper.monsterrat[16].drawFontLeft(context, "Добавить новый предмет", addButtonX + 50, addButtonY + 12.5F, Color.WHITE.getRGB());
        }

        float statsY = panelY + panelHeight - 35;
        int enabledCount = 0;
        int totalItems = 0;
        int totalPrice = 0;

        for (AutoBuy.BuyItem item : buyItems) {
            if (item.enabled) enabledCount++;
            totalItems += item.quantity;
            totalPrice += item.maxPricePerUnit * item.quantity;
        }

        String statsText = String.format("Предметов: %d (%d вкл) | Всего: x%d | Сумма: %s",
                buyItems.size(), enabledCount, totalItems, formatPrice(totalPrice));
        FontHelper.monsterrat[12].drawCenteredString(context, statsText,
                panelX + panelWidth / 2, statsY, new Color(200, 200, 200).getRGB());

        float closeButtonSize = 20;
        float closeX = panelX + panelWidth - closeButtonSize - 10;
        float closeY = panelY + 10;

        int closeColor = isMouseOverCloseButton(mouseX, mouseY, closeX, closeY, closeButtonSize) ?
                0xFFFF5555 : 0xFFFF0000;
        RenderUtil.drawRoundedRect(context.getMatrices(), closeX, closeY, closeButtonSize, closeButtonSize,
                new Vector4f(5), closeColor);
        FontHelper.icons[12].drawCenteredString(context, "✕", closeX + closeButtonSize / 2, closeY + 4, Color.WHITE.getRGB());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float closeButtonSize = 20;
            float closeX = panelX + panelWidth - closeButtonSize - 10;
            float closeY = panelY + 10;

            if (isMouseOverCloseButton((float)mouseX, (float)mouseY, closeX, closeY, closeButtonSize)) {
                saveAndClose();
                return true;
            }

            float listX = panelX + 20;
            float listY = panelY + 70;
            float listWidth = panelWidth - 40;
            float listHeight = panelHeight - 220;
            float scrollAreaY = listY + 40;

            float itemY = scrollAreaY - scrollOffset;
            int itemIndex = 0;

            for (AutoBuy.BuyItem item : buyItems) {
                if (itemY + 30 > scrollAreaY && itemY < scrollAreaY + listHeight - 45) {
                    float statusX = listX + listWidth - 100;
                    if (mouseX >= statusX && mouseX <= statusX + 30 &&
                            mouseY >= itemY && mouseY <= itemY + 30) {
                        item.enabled = !item.enabled;
                        saveToConfig();
                        return true;
                    }

                    float editX = listX + listWidth - 130;
                    if (isMouseOverEditButton((float)mouseX, (float)mouseY, editX, itemY)) {
                        startEditingItem(itemIndex);
                        return true;
                    }

                    float deleteX = listX + listWidth - 30;
                    if (isMouseOverDeleteButton((float)mouseX, (float)mouseY, deleteX, itemY)) {
                        removeItem(itemIndex);
                        return true;
                    }
                }

                itemY += 35;
                itemIndex++;
            }

            if (isMouseOverScrollbar((float)mouseX, (float)mouseY)) {
                draggingScroll = true;
                lastMouseY = (float) mouseY;
                return true;
            }

            if (!addingNewItem && editingItemIndex == -1) {
                float addButtonY = listY + listHeight + 15;
                float addButtonWidth = 220;
                float addButtonX = panelX + (panelWidth - addButtonWidth) / 2;

                if (isMouseOverAddButton((float)mouseX, (float)mouseY, addButtonX, addButtonY)) {
                    startAddingNewItem();
                    return true;
                }
            }

            if (addingNewItem || editingItemIndex != -1) {
                float listX2 = panelX + 20;
                float editY = listY + listHeight + 15;
                float inputWidth = panelWidth - 180 - 40;

                float idInputX = listX2 + 100;
                float idInputY = editY + 35;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= idInputY - 2 && mouseY <= idInputY + 20) {
                    typingInIdField = true;
                    typingInPriceField = false;
                    typingInQuantityField = false;
                    return true;
                }

                float priceInputY = editY + 63;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= priceInputY - 2 && mouseY <= priceInputY + 20) {
                    typingInIdField = false;
                    typingInPriceField = true;
                    typingInQuantityField = false;
                    return true;
                }

                float quantityInputY = editY + 91;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= quantityInputY - 2 && mouseY <= quantityInputY + 20) {
                    typingInIdField = false;
                    typingInPriceField = false;
                    typingInQuantityField = true;
                    return true;
                }

                float buttonY = editY + 120;
                float buttonWidth = 120;
                float cancelX = panelX + (panelWidth - buttonWidth * 2 - 20) / 2;

                if (isMouseOverCancelButton((float)mouseX, (float)mouseY, cancelX, buttonY)) {
                    cancelEditing();
                    return true;
                }

                float saveX = cancelX + buttonWidth + 20;
                if (isMouseOverSaveButton((float)mouseX, (float)mouseY, saveX, buttonY)) {
                    saveItem();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScroll && button == 0) {
            float listY = panelY + 70;
            float listHeight = panelHeight - 180;
            float scrollAreaY = listY + 40;
            float scrollAreaHeight = listHeight - 45;
            float totalContentHeight = buyItems.size() * 35;
            float maxScroll = Math.max(0, totalContentHeight - scrollAreaHeight);

            if (maxScroll > 0) {
                float delta = (float) (mouseY - lastMouseY);
                float scrollPercent = delta / scrollAreaHeight;
                scrollOffset += scrollPercent * totalContentHeight;
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
                lastMouseY = (float) mouseY;
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScroll) {
            draggingScroll = false;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float listY = panelY + 70;
        float listHeight = panelHeight - 180;
        float scrollAreaY = listY + 40;
        float scrollAreaHeight = listHeight - 45;
        float totalContentHeight = buyItems.size() * 35;
        float maxScroll = Math.max(0, totalContentHeight - scrollAreaHeight);

        if (maxScroll > 0) {
            scrollOffset -= verticalAmount * 25;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addingNewItem || editingItemIndex != -1) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveItem();
                return true;
            }
            else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelEditing();
                return true;
            }
            else if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (typingInIdField) {
                    typingInIdField = false;
                    typingInPriceField = true;
                    typingInQuantityField = false;
                } else if (typingInPriceField) {
                    typingInIdField = false;
                    typingInPriceField = false;
                    typingInQuantityField = true;
                } else {
                    typingInIdField = true;
                    typingInPriceField = false;
                    typingInQuantityField = false;
                }
                return true;
            }
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (typingInIdField && !newItemId.isEmpty()) {
                    newItemId = newItemId.substring(0, newItemId.length() - 1);
                    return true;
                } else if (typingInPriceField && !newItemPrice.isEmpty()) {
                    newItemPrice = newItemPrice.substring(0, newItemPrice.length() - 1);
                    return true;
                } else if (typingInQuantityField && !newItemQuantity.isEmpty()) {
                    newItemQuantity = newItemQuantity.substring(0, newItemQuantity.length() - 1);
                    return true;
                }
            }
            else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboard = mc.keyboard.getClipboard();
                if (!clipboard.isEmpty()) {
                    if (typingInIdField) {
                        newItemId += clipboard;
                    } else if (typingInPriceField) {
                        String digitsOnly = clipboard.replaceAll("[^0-9]", "");
                        newItemPrice += digitsOnly;
                    } else if (typingInQuantityField) {
                        String digitsOnly = clipboard.replaceAll("[^0-9]", "");
                        newItemQuantity += digitsOnly;
                    }
                    return true;
                }
            }
            else if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                return true;
            }
            else if ((keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) &&
                    !typingInIdField && !typingInPriceField && !typingInQuantityField) {
                return true;
            }
        }
        else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveAndClose();
            return true;
        }
        else if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            startAddingNewItem();
            return true;
        }
        else if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            saveToConfig();
            ChatUtil.sendChat("§aНастройки сохранены!");
            return true;
        }
        else if (keyCode == GLFW.GLFW_KEY_O && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (ConfigManager.getInstance() != null) {
                ConfigManager.getInstance().openConfigsFolder();
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (addingNewItem || editingItemIndex != -1) {
            if (typingInIdField) {
                if (Character.isLetterOrDigit(chr) || chr == '_' || chr == ':') {
                    newItemId += chr;
                }
            } else if (typingInPriceField) {
                if (Character.isDigit(chr)) {
                    newItemPrice += chr;
                }
            } else if (typingInQuantityField) {
                if (Character.isDigit(chr)) {
                    newItemQuantity += chr;
                }
            }
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    private void startAddingNewItem() {
        addingNewItem = true;
        editingItemIndex = -1;
        newItemId = "";
        newItemPrice = "";
        newItemQuantity = "";
        typingInIdField = true;
        typingInPriceField = false;
        typingInQuantityField = false;
    }

    private void startEditingItem(int index) {
        if (index >= 0 && index < buyItems.size()) {
            editingItemIndex = index;
            addingNewItem = false;
            AutoBuy.BuyItem item = buyItems.get(index);
            newItemId = item.id;
            newItemPrice = String.valueOf(item.maxPricePerUnit);
            newItemQuantity = String.valueOf(item.quantity);
            typingInIdField = true;
            typingInPriceField = false;
            typingInQuantityField = false;
        }
    }



    private void saveItem() {
        if (newItemId.isEmpty() || newItemPrice.isEmpty() || newItemQuantity.isEmpty()) {
            ChatUtil.sendError("Заполните все поля!");
            return;
        }

        try {
            int price = Integer.parseInt(newItemPrice);
            int quantity = Integer.parseInt(newItemQuantity);

            if (price <= 0) {
                ChatUtil.sendError("Цена должна быть положительной!");
                return;
            }

            if (quantity <= 0) {
                ChatUtil.sendError("Количество должно быть положительным!");
                return;
            }

            Item mcItem = getItemFromId(newItemId);
            if (mcItem == null || mcItem == Items.AIR) {
                ChatUtil.sendError("Предмет с ID '" + newItemId + "' не найден!");
                return;
            }

            if (editingItemIndex != -1) {
                AutoBuy.BuyItem item = buyItems.get(editingItemIndex);
                item.id = newItemId;
                item.maxPricePerUnit = price;
                item.quantity = quantity;
                ChatUtil.sendChat("§aПредмет обновлен!");
            } else {
                buyItems.add(new AutoBuy.BuyItem(newItemId, price, quantity, true));
                ChatUtil.sendChat("§aПредмет добавлен!");
            }

            saveToConfig();
            cancelEditing();

        } catch (NumberFormatException e) {
            ChatUtil.sendError("Неверный формат числа!");
        }
    }

    private void removeItem(int index) {
        if (index >= 0 && index < buyItems.size()) {
            buyItems.remove(index);
            ChatUtil.sendChat("§cПредмет удален!");

            if (index == editingItemIndex) {
                cancelEditing();
            }

            saveToConfig();
        }
    }

    private void cancelEditing() {
        addingNewItem = false;
        editingItemIndex = -1;
        newItemId = "";
        newItemPrice = "";
        newItemQuantity = "";
        typingInIdField = true;
        typingInPriceField = false;
        typingInQuantityField = false;
    }

    private void saveAndClose() {
        saveToConfig();
        this.close();
    }

    private void saveToConfig() {
        if (ConfigManager.getInstance() != null) {
            List<AutoBuyUI.BuyItem> uiItems = new ArrayList<>();
            for (AutoBuy.BuyItem item : buyItems) {
                if (item.maxPricePerUnit > Integer.MAX_VALUE) {
                    ChatUtil.sendError("Цена " + item.maxPricePerUnit + " для " + item.id +
                            " слишком большая для сохранения!");
                    continue;
                }
                uiItems.add(new AutoBuyUI.BuyItem(item.id, (int) item.maxPricePerUnit, item.quantity, item.enabled));
            }
            ConfigManager.getInstance().saveAutoBuyItems(uiItems);
        }
    }

    private boolean isMouseOverScrollbar(float mouseX, float mouseY) {
        if (scrollbarThumbHeight == 0) return false;

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    private boolean isMouseOverEditButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        float iconSize = 12;
        float iconYOffset = 10;

        return mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                mouseY >= buttonY + iconYOffset && mouseY <= buttonY + iconYOffset + iconSize;
    }

    private boolean isMouseOverDeleteButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        float iconSize = 12;
        float iconYOffset = 10;

        return mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                mouseY >= buttonY + iconYOffset && mouseY <= buttonY + iconYOffset + iconSize;
    }

    private boolean isMouseOverAddButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        return mouseX >= buttonX && mouseX <= buttonX + 200 &&
                mouseY >= buttonY && mouseY <= buttonY + 40;
    }

    private boolean isMouseOverCancelButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        return mouseX >= buttonX && mouseX <= buttonX + 120 &&
                mouseY >= buttonY && mouseY <= buttonY + 32;
    }

    private boolean isMouseOverSaveButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        return mouseX >= buttonX && mouseX <= buttonX + 120 &&
                mouseY >= buttonY && mouseY <= buttonY + 32;
    }

    private boolean isMouseOverCloseButton(float mouseX, float mouseY, float buttonX, float buttonY, float size) {
        return mouseX >= buttonX && mouseX <= buttonX + size &&
                mouseY >= buttonY && mouseY <= buttonY + size;
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.1fM", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    private Item getItemFromId(String id) {
        try {
            Identifier identifier;
            if (id.contains(":")) {
                identifier = Identifier.tryParse(id);
            } else {
                identifier = Identifier.tryParse("minecraft:" + id);
            }

            if (identifier != null) {
                return Registries.ITEM.get(identifier);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static class BuyItem {
        public String id;
        public int maxPricePerUnit;
        public int quantity;
        public boolean enabled;

        public BuyItem(String id, int maxPricePerUnit, int quantity, boolean enabled) {
            this.id = id;
            this.maxPricePerUnit = maxPricePerUnit;
            this.quantity = quantity;
            this.enabled = enabled;
        }

        public BuyItem(String id, int maxPricePerUnit, boolean enabled) {
            this(id, maxPricePerUnit, 1, enabled);
        }
    }
}