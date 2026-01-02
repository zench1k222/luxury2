package dev.luxury.ui;

import dev.luxury.modules.impl.misc.AutoBuy;
import dev.luxury.utils.managers.AutoBuyManager;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.luxury.modules.impl.other.targetesp.mode.Circle.mc;

public class AutoBuyUI3 extends Screen {

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

    private static final int CARD_WIDTH = 125;
    private static final int CARD_HEIGHT = 85;
    private static final int CARD_MARGIN = 10;
    private static final int CARDS_PER_ROW = 4;

    public AutoBuyUI3(AutoBuy autoBuyModule) {
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

        panelWidth = Math.min(600, screenWidth - 40);
        panelHeight = Math.min(700, screenHeight - 40);
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        Color primaryColor = new Color(75, 35, 90);
        Color secondaryColor = new Color(45, 27, 105);
        Color accentColor = new Color(212, 175, 55);
        Color backgroundColor = new Color(20, 20, 30, 230);
        Color panelColor = new Color(30, 20, 40, 240);

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight - 90,
                new Vector4f(15), panelColor.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), panelX, panelY, panelWidth, panelHeight - 90,
                new Vector4f(15), accentColor.getRGB(), 2.0f, 1.0f, 1.0f, false);

        FontHelper.findsans[24].drawCenteredString(context, "AutoBuyUI",
                panelX + panelWidth / 2, panelY + 20, accentColor.getRGB());

        FontHelper.sfprobold[17].drawCenteredString(context, "Добавляйте предметы для автоматической покупки",
                panelX + panelWidth / 2, panelY + 45, Color.WHITE.getRGB());

        float cardsStartX = panelX + 20;
        float cardsStartY = panelY + 70;
        float cardsAreaWidth = panelWidth - 40;
        float cardsAreaHeight = panelHeight - 250;

        RenderUtil.drawRoundedRect(context.getMatrices(), cardsStartX, cardsStartY, cardsAreaWidth, cardsAreaHeight,
                new Vector4f(10), new Color(10, 5, 20, 200).getRGB());

        float scrollAreaY = cardsStartY + 5;
        float scrollAreaHeight = cardsAreaHeight - 10;

        RenderUtil.enableScissor((int)cardsStartX, (int)scrollAreaY, (int)cardsAreaWidth, (int)scrollAreaHeight);

        float cardY = scrollAreaY - scrollOffset;
        int cardIndex = 0;

        for (int i = 0; i < buyItems.size(); i++) {
            AutoBuy.BuyItem item = buyItems.get(i);

            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;
            float cardX = cardsStartX + CARD_MARGIN + col * (CARD_WIDTH + CARD_MARGIN);
            cardY = scrollAreaY + row * (CARD_HEIGHT + CARD_MARGIN) - scrollOffset;

            if (cardY + CARD_HEIGHT > scrollAreaY && cardY < scrollAreaY + scrollAreaHeight) {
                renderCard(context, mouseX, mouseY, cardX, cardY, item, i);
            }

            cardIndex++;
        }

        RenderUtil.disableScissor();

        float totalRows = (float) Math.ceil((double) buyItems.size() / CARDS_PER_ROW);
        float totalContentHeight = totalRows * (CARD_HEIGHT + CARD_MARGIN) - CARD_MARGIN;

        if (totalContentHeight > scrollAreaHeight) {
            scrollbarWidth = 6;
            scrollbarX = cardsStartX + cardsAreaWidth - scrollbarWidth - 5;
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

        float editY = cardsStartY + cardsAreaHeight + 15;

        if (addingNewItem || editingItemIndex != -1) {
            renderEditPanel(context, mouseX, mouseY, editY);
        } else {
            renderAddButton(context, mouseX, mouseY, editY);
        }

        float statsY = panelY + panelHeight - 35;
        int enabledCount = 0;
        int totalItems = 0;
        long totalPrice = 0;

        for (AutoBuy.BuyItem item : buyItems) {
            if (item.enabled) enabledCount++;
            totalItems += item.quantity;
            totalPrice += item.maxPricePerUnit * item.quantity;
        }

        String statsText = String.format("Предметов: %d (%d вкл) | Общая сумма: %s",
                buyItems.size(), enabledCount, formatPrice(totalPrice));
        FontHelper.findsans[15].drawCenteredString(context, statsText,
                panelX + panelWidth / 2, statsY - 77.5F, new Color(250, 250, 250).getRGB());

        float closeButtonSize = 20;
        float closeX = panelX + panelWidth - closeButtonSize - 10;
        float closeY = panelY + 10;

        int closeColor = isMouseOverCloseButton(mouseX, mouseY, closeX, closeY, closeButtonSize) ?
                0xFFFF5555 : 0xFFFF0000;
        RenderUtil.drawRoundedRect(context.getMatrices(), closeX, closeY, closeButtonSize, closeButtonSize,
                new Vector4f(5), closeColor);
        FontHelper.icons[12].drawCenteredString(context, "✕", closeX + closeButtonSize / 2, closeY + 4, Color.WHITE.getRGB());
    }

    private void renderCard(DrawContext context, int mouseX, int mouseY, float x, float y, AutoBuy.BuyItem item, int index) {
        Color cardColor = item.enabled ? new Color(45, 27, 105, 200) : new Color(60, 60, 60, 200);
        Color borderColor = item.enabled ? new Color(212, 175, 55) : new Color(100, 100, 100);

        if (editingItemIndex == index) {
            cardColor = new Color(65, 47, 125, 200);
        }

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, CARD_WIDTH, CARD_HEIGHT,
                new Vector4f(8), cardColor.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), x, y, CARD_WIDTH, CARD_HEIGHT,
                new Vector4f(8), borderColor.getRGB(), 1.5f, 1.0f, 1.0f, false);

        Item mcItem = getItemFromId(item.id);
        float iconX = x + 10;
        float iconY = y + 10;

        if (mcItem != null && mcItem != Items.AIR) {
            ItemStack stack = new ItemStack(mcItem, Math.min(item.quantity, 64));
            RenderUtil.drawItemStack(context, stack, iconX, iconY, 20);
        } else {
            RenderUtil.drawRoundedRect(context.getMatrices(), iconX, iconY, 20, 20,
                    new Vector4f(4), new Color(50, 50, 50).getRGB());
            FontHelper.icons[14].drawCenteredString(context, "?", iconX + 10, iconY + 4, 0xFFFF5555);
        }

        String displayName = item.id;
        if (displayName.length() > 8) {
            displayName = displayName.substring(0, 8) + "...";
        }
        FontHelper.findsans[12].drawFontLeft(context, displayName, x + 35, y + 12, Color.WHITE.getRGB());

        String priceText = formatPrice(item.maxPricePerUnit) + "/шт";
        FontHelper.findsans[12].drawFontLeft(context, "Цена:", x + 10, y + 40, new Color(180, 180, 180).getRGB());
        FontHelper.sfprobold[14].drawFontLeft(context, priceText, x + 10, y + 50, new Color(212, 175, 55).getRGB());

        String quantityText = "x" + item.quantity;
        FontHelper.findsans[12].drawFontLeft(context, "Кол-во:", x + CARD_WIDTH - 70, y + 40, new Color(180, 180, 180).getRGB());
        FontHelper.sfprobold[14].drawFontLeft(context, quantityText, x + CARD_WIDTH - 70, y + 50, new Color(0, 170, 255).getRGB());

        long totalPrice = item.maxPricePerUnit * item.quantity;
        String totalText = formatPrice(totalPrice);
        FontHelper.sfprobold[14].drawCenteredString(context, "Всего: " + totalText,
                x + CARD_WIDTH / 2, y + 68, new Color(250, 170, 250).getRGB());

        float toggleButtonWidth = 40;
        float toggleButtonHeight = 18;
        float toggleButtonX = x + CARD_WIDTH - toggleButtonWidth - 8;
        float toggleButtonY = y + 8;

        int toggleBgColor = item.enabled ?
                new Color(0, 150, 0, 220).getRGB() :
                new Color(150, 0, 0, 220).getRGB();

        RenderUtil.drawRoundedRect(context.getMatrices(),
                toggleButtonX, toggleButtonY,
                toggleButtonWidth, toggleButtonHeight,
                new Vector4f(3), toggleBgColor);

        Color accentColor = new Color(212, 175, 55);
        RenderUtil.drawBorder(context.getMatrices(),
                toggleButtonX, toggleButtonY,
                toggleButtonWidth, toggleButtonHeight,
                new Vector4f(3), accentColor.getRGB(), 1.0f, 1.0f, 1.0f, false);

        boolean isMouseOverToggle = isMouseOverToggleButton(mouseX, mouseY,
                toggleButtonX, toggleButtonY, toggleButtonWidth, toggleButtonHeight);
        if (isMouseOverToggle) {
            RenderUtil.drawBorder(context.getMatrices(),
                    toggleButtonX, toggleButtonY,
                    toggleButtonWidth, toggleButtonHeight,
                    new Vector4f(3), Color.WHITE.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        String statusText = item.enabled ? "ВКЛ" : "ВЫКЛ";
        int statusColor = Color.WHITE.getRGB();
        FontHelper.sfprobold[10].drawCenteredString(context, statusText,
                toggleButtonX + toggleButtonWidth / 2, toggleButtonY + 5, statusColor);

        float buttonSize = 16;
        float editButtonX = x + 5;
        float editButtonY = y + CARD_HEIGHT - buttonSize - 5;
        float deleteButtonX = x + CARD_WIDTH - buttonSize - 5;
        float deleteButtonY = y + CARD_HEIGHT - buttonSize - 5;

        int editColor = isMouseOverEditButton(mouseX, mouseY, editButtonX, editButtonY, buttonSize) ?
                new Color(212, 175, 55).getRGB() : new Color(150, 150, 150).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), editButtonX, editButtonY, buttonSize, buttonSize,
                new Vector4f(3), editColor);
        FontHelper.icons[10].drawCenteredString(context, "R", editButtonX + buttonSize / 2, editButtonY + 5, Color.WHITE.getRGB());

        int deleteColor = isMouseOverDeleteButton(mouseX, mouseY, deleteButtonX, deleteButtonY, buttonSize) ?
                0xFFFF5555 : 0xFFFF0000;
        RenderUtil.drawRoundedRect(context.getMatrices(), deleteButtonX, deleteButtonY, buttonSize, buttonSize,
                new Vector4f(3), deleteColor);
        FontHelper.icons[10].drawCenteredString(context, "Q", deleteButtonX + buttonSize / 2, deleteButtonY + 5, Color.WHITE.getRGB());
    }

    private void renderEditPanel(DrawContext context, int mouseX, int mouseY, float y) {
        float panelStartX = panelX - 230;
        float panelStartY = panelY + 130;
        float panelSize = 180;

        if (panelStartX < 10) {
            panelStartX = 10;
        }

        RenderUtil.drawRoundedRect(context.getMatrices(), panelStartX, panelStartY, panelSize, panelSize,
                new Vector4f(10), new Color(30, 20, 40, 240).getRGB());

        Color accentColor = new Color(212, 175, 55);
        RenderUtil.drawBorder(context.getMatrices(), panelStartX, panelStartY, panelSize, panelSize,
                new Vector4f(10), accentColor.getRGB(), 2.0f, 1.0f, 1.0f, false);

        String title = editingItemIndex != -1 ? "Редактирование" : "Новый предмет";
        FontHelper.findsans[16].drawCenteredString(context, title,
                panelStartX + panelSize / 2, panelStartY + 12, Color.WHITE.getRGB());

        float centerX = panelStartX + panelSize / 2;

        float fieldY = panelStartY + 35;
        FontHelper.findsans[12].drawCenteredString(context, "ID предмета:", centerX, fieldY + 1, Color.WHITE.getRGB());

        fieldY += 13;
        float inputWidth = panelSize - 30;
        float idInputX = panelStartX + 15;
        int idFieldColor = typingInIdField ? new Color(212, 175, 55, 150).getRGB() : new Color(0, 0, 0, 150).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), idInputX, fieldY, inputWidth, 18,
                new Vector4f(4), idFieldColor);

        if (typingInIdField) {
            RenderUtil.drawBorder(context.getMatrices(), idInputX, fieldY, inputWidth, 18,
                    new Vector4f(4), accentColor.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        String displayId = newItemId.isEmpty() ? "end_crystal" : newItemId;
        int idTextColor = newItemId.isEmpty() ? 0xFF888888 : Color.WHITE.getRGB();
        FontHelper.monsterrat[11].drawCenteredString(context, displayId, centerX, fieldY + 5, idTextColor);

        if (typingInIdField) {
            float textWidth = FontHelper.monsterrat[11].getWidth(displayId);
            float cursorX = centerX + textWidth / 2;
            long cursorTime = System.currentTimeMillis() % 1000;
            if (cursorTime < 500) {
                RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY + 3, 1, 12,
                        new Vector4f(0), Color.WHITE.getRGB());
            }
        }

        fieldY += 25;
        FontHelper.findsans[12].drawCenteredString(context, "Цена за шт:", centerX, fieldY + 1, Color.WHITE.getRGB());

        fieldY += 13;
        float priceInputX = panelStartX + 15;
        int priceFieldColor = typingInPriceField ? new Color(212, 175, 55, 150).getRGB() : new Color(0, 0, 0, 150).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), priceInputX, fieldY, inputWidth, 18,
                new Vector4f(4), priceFieldColor);

        if (typingInPriceField) {
            RenderUtil.drawBorder(context.getMatrices(), priceInputX, fieldY, inputWidth, 18,
                    new Vector4f(4), accentColor.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        String displayPrice = newItemPrice.isEmpty() ? "20000" : newItemPrice;
        int priceTextColor = newItemPrice.isEmpty() ? 0xFF888888 : Color.WHITE.getRGB();
        FontHelper.monsterrat[11].drawCenteredString(context, displayPrice, centerX, fieldY + 4, priceTextColor);

        if (typingInPriceField) {
            float textWidth = FontHelper.monsterrat[11].getWidth(displayPrice);
            float cursorX = centerX + textWidth / 2;
            long cursorTime = System.currentTimeMillis() % 1000;
            if (cursorTime < 500) {
                RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY + 3, 1, 12,
                        new Vector4f(0), Color.WHITE.getRGB());
            }
        }

        fieldY += 25;
        FontHelper.findsans[12].drawCenteredString(context, "Количество:", centerX, fieldY, Color.WHITE.getRGB());

        fieldY += 13;
        float quantityInputX = panelStartX + 15;
        int quantityFieldColor = typingInQuantityField ? new Color(212, 175, 55, 150).getRGB() : new Color(0, 0, 0, 150).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), quantityInputX, fieldY, inputWidth, 18,
                new Vector4f(4), quantityFieldColor);

        if (typingInQuantityField) {
            RenderUtil.drawBorder(context.getMatrices(), quantityInputX, fieldY, inputWidth, 18,
                    new Vector4f(4), accentColor.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        String displayQuantity = newItemQuantity.isEmpty() ? "1" : newItemQuantity;
        int quantityTextColor = newItemQuantity.isEmpty() ? 0xFF888888 : Color.WHITE.getRGB();
        FontHelper.monsterrat[11].drawCenteredString(context, displayQuantity, centerX, fieldY + 4, quantityTextColor);

        if (typingInQuantityField) {
            float textWidth = FontHelper.monsterrat[11].getWidth(displayQuantity);
            float cursorX = centerX + textWidth / 2;
            long cursorTime = System.currentTimeMillis() % 1000;
            if (cursorTime < 500) {
                RenderUtil.drawRoundedRect(context.getMatrices(), cursorX, fieldY + 3, 1, 12,
                        new Vector4f(0), Color.WHITE.getRGB());
            }
        }

        float buttonY = panelStartY + panelSize - 30;
        float buttonWidth = (panelSize - 35) / 2;
        float buttonHeight = 22;

        float cancelX = panelStartX + 10;
        float saveX = panelStartX + panelSize - buttonWidth - 10;

        boolean isOverCancel = isMouseOver(mouseX, mouseY, cancelX, buttonY, buttonWidth, buttonHeight);
        int cancelColor = isOverCancel ? new Color(220, 60, 60).getRGB() : new Color(178, 34, 34).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), cancelX, buttonY, buttonWidth, buttonHeight,
                new Vector4f(5), cancelColor);

        if (isOverCancel) {
            RenderUtil.drawBorder(context.getMatrices(), cancelX, buttonY, buttonWidth, buttonHeight,
                    new Vector4f(5), Color.WHITE.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        FontHelper.findsans[12].drawCenteredString(context, "Отмена", cancelX + buttonWidth / 2, buttonY + 6, Color.WHITE.getRGB());

        String saveText = editingItemIndex != -1 ? "Применить" : "Добавить";
        boolean isOverSave = isMouseOver(mouseX, mouseY, saveX, buttonY, buttonWidth, buttonHeight);
        int saveColor = isOverSave ? new Color(70, 225, 70).getRGB() : new Color(50, 180, 50).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), saveX, buttonY, buttonWidth, buttonHeight,
                new Vector4f(5), saveColor);

        if (isOverSave) {
            RenderUtil.drawBorder(context.getMatrices(), saveX, buttonY, buttonWidth, buttonHeight,
                    new Vector4f(5), Color.WHITE.getRGB(), 1.0f, 1.0f, 1.0f, false);
        }

        FontHelper.findsans[12].drawCenteredString(context, saveText, saveX + buttonWidth / 2, buttonY + 6, Color.WHITE.getRGB());
    }

    private boolean isMouseOver(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    private void renderAddButton(DrawContext context, int mouseX, int mouseY, float y) {
        float buttonWidth = 45;
        float buttonX = panelX + (panelWidth - buttonWidth) / 2;

        int buttonColor = isMouseOverAddButton(mouseX, mouseY, buttonX, y) ?
                new Color(0, 170, 0).brighter().getRGB() : new Color(0, 145, 0).getRGB();
        RenderUtil.drawRoundedRect(context.getMatrices(), buttonX, y, buttonWidth, 40,
                new Vector4f(10), buttonColor);

        FontHelper.icons[20].drawCenteredString(context, "+", buttonX + 25, y + 10, Color.WHITE.getRGB());
        FontHelper.findsans[50].drawFontLeft(context, "+", buttonX + 14.5F, y + 4, Color.WHITE.getRGB());
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

            float cardsStartX = panelX + 20;
            float cardsStartY = panelY + 70;
            float scrollAreaY = cardsStartY + 5;

            for (int i = 0; i < buyItems.size(); i++) {
                int row = i / CARDS_PER_ROW;
                int col = i % CARDS_PER_ROW;
                float cardX = cardsStartX + CARD_MARGIN + col * (CARD_WIDTH + CARD_MARGIN);
                float cardY = scrollAreaY + row * (CARD_HEIGHT + CARD_MARGIN) - scrollOffset;

                if (cardY + CARD_HEIGHT > scrollAreaY && cardY < scrollAreaY + (panelHeight - 250 - 10)) {
                    float toggleButtonX = cardX + CARD_WIDTH - 40 - 8;
                    float toggleButtonY = cardY + 8;
                    float toggleButtonWidth = 40;
                    float toggleButtonHeight = 18;

                    if (isMouseOverToggleButton((float)mouseX, (float)mouseY,
                            toggleButtonX, toggleButtonY, toggleButtonWidth, toggleButtonHeight)) {
                        buyItems.get(i).enabled = !buyItems.get(i).enabled;
                        saveToConfig();
                        return true;
                    }

                    float editButtonSize = 16;
                    float editButtonX = cardX + 5;
                    float editButtonY = cardY + CARD_HEIGHT - editButtonSize - 5;
                    if (isMouseOverEditButton((float)mouseX, (float)mouseY, editButtonX, editButtonY, editButtonSize)) {
                        startEditingItem(i);
                        return true;
                    }

                    float deleteButtonX = cardX + CARD_WIDTH - editButtonSize - 5;
                    float deleteButtonY = cardY + CARD_HEIGHT - editButtonSize - 5;
                    if (isMouseOverDeleteButton((float)mouseX, (float)mouseY, deleteButtonX, deleteButtonY, editButtonSize)) {
                        removeItem(i);
                        return true;
                    }

                    if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                            mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                        return true;
                    }
                }
            }

            if (isMouseOverScrollbar((float)mouseX, (float)mouseY)) {
                draggingScroll = true;
                lastMouseY = (float) mouseY;
                return true;
            }

            if (!addingNewItem && editingItemIndex == -1) {
                float addButtonY = cardsStartY + (panelHeight - 250) + 15;
                float addButtonWidth = 45;
                float addButtonX = panelX + (panelWidth - addButtonWidth) / 2;

                if (isMouseOverAddButton((float)mouseX, (float)mouseY, addButtonX, addButtonY)) {
                    startAddingNewItem();
                    return true;
                }
            }

            if (addingNewItem || editingItemIndex != -1) {
                float panelStartX = panelX - 230;
                float panelStartY = panelY + 130;
                float panelSize = 180;

                if (panelStartX < 10) {
                    panelStartX = 10;
                }

                float inputWidth = panelSize - 30;
                float idInputX = panelStartX + 15;

                float idInputY = panelStartY + 53;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= idInputY && mouseY <= idInputY + 20) {
                    typingInIdField = true;
                    typingInPriceField = false;
                    typingInQuantityField = false;
                    return true;
                }

                float priceInputY = panelStartY + 81;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= priceInputY && mouseY <= priceInputY + 20) {
                    typingInIdField = false;
                    typingInPriceField = true;
                    typingInQuantityField = false;
                    return true;
                }

                float quantityInputY = panelStartY + 109;
                if (mouseX >= idInputX && mouseX <= idInputX + inputWidth &&
                        mouseY >= quantityInputY && mouseY <= quantityInputY + 20) {
                    typingInIdField = false;
                    typingInPriceField = false;
                    typingInQuantityField = true;
                    return true;
                }

                float buttonY = panelStartY + panelSize - 28;
                float buttonWidth = (panelSize - 35) / 2;
                float buttonHeight = 22;

                float cancelX = panelStartX + 10;
                float saveX = panelStartX + panelSize - buttonWidth - 10;

                if (isMouseOver((float)mouseX, (float)mouseY, cancelX, buttonY, buttonWidth, buttonHeight)) {
                    cancelEditing();
                    return true;
                }

                if (isMouseOver((float)mouseX, (float)mouseY, saveX, buttonY, buttonWidth, buttonHeight)) {
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
            float cardsStartY = panelY + 70;
            float scrollAreaY = cardsStartY + 5;
            float scrollAreaHeight = panelHeight - 250 - 10;

            float totalRows = (float) Math.ceil((double) buyItems.size() / CARDS_PER_ROW);
            float totalContentHeight = totalRows * (CARD_HEIGHT + CARD_MARGIN) - CARD_MARGIN;
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
        float cardsStartY = panelY + 70;
        float scrollAreaY = cardsStartY + 5;
        float scrollAreaHeight = panelHeight - 250 - 10;

        float totalRows = (float) Math.ceil((double) buyItems.size() / CARDS_PER_ROW);
        float totalContentHeight = totalRows * (CARD_HEIGHT + CARD_MARGIN) - CARD_MARGIN;
        float maxScroll = Math.max(0, totalContentHeight - scrollAreaHeight);

        if (maxScroll > 0) {
            scrollOffset -= verticalAmount * 35;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }



    private void saveItem() {
        if (newItemId.isEmpty() || newItemPrice.isEmpty() || newItemQuantity.isEmpty()) {
            ChatUtil.sendError("Заполните все поля!");
            return;
        }

        try {
            long price = Long.parseLong(newItemPrice);
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

    private boolean isMouseOverScrollbar(float mouseX, float mouseY) {
        if (scrollbarThumbHeight == 0) return false;

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    private boolean isMouseOverEditButton(float mouseX, float mouseY, float buttonX, float buttonY, float size) {
        return mouseX >= buttonX && mouseX <= buttonX + size &&
                mouseY >= buttonY && mouseY <= buttonY + size;
    }

    private boolean isMouseOverDeleteButton(float mouseX, float mouseY, float buttonX, float buttonY, float size) {
        return mouseX >= buttonX && mouseX <= buttonX + size &&
                mouseY >= buttonY && mouseY <= buttonY + size;
    }

    private boolean isMouseOverAddButton(float mouseX, float mouseY, float buttonX, float buttonY) {
        return mouseX >= buttonX && mouseX <= buttonX + 45 &&
                mouseY >= buttonY && mouseY <= buttonY + 40;
    }

    private boolean isMouseOverToggleButton(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    private boolean isMouseOverCloseButton(float mouseX, float mouseY, float buttonX, float buttonY, float size) {
        return mouseX >= buttonX && mouseX <= buttonX + size &&
                mouseY >= buttonY && mouseY <= buttonY + size;
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
                return net.minecraft.registry.Registries.ITEM.get(identifier);
            }
        } catch (Exception e) {
        }
        return null;
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
            AutoBuyManager.getInstance().openConfigsFolder();
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
        AutoBuyManager manager = AutoBuyManager.getInstance();
        if (manager != null) {
            List<AutoBuyUI.BuyItem> uiItems = new ArrayList<>();
            for (AutoBuy.BuyItem item : buyItems) {
                if (item.maxPricePerUnit > Integer.MAX_VALUE) {
                    ChatUtil.sendError("Цена " + item.maxPricePerUnit + " для " + item.id +
                            " слишком большая для сохранения!");
                    continue;
                }
                uiItems.add(new AutoBuyUI.BuyItem(item.id, (int) item.maxPricePerUnit, item.quantity, item.enabled));
            }
            manager.saveAutoBuyItems(uiItems);
            ChatUtil.sendChat("§aПредметы AutoBuy сохранены!");
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