package dev.luxury.ui;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.ui.settings.SettingRenderer;
import dev.luxury.ui.settings.SettingRendererManager;
import dev.luxury.ui.settings.KeySettingRenderer;
import dev.luxury.ui.settings.SliderSettingRenderer;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Vector4f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TabbedGUI extends Screen {
    private Category selectedCategory = Category.Combat;
    private final ModuleManager moduleManager;

    private String searchText = "";
    private boolean searchActive = false;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;

    private final float panelWidth = 650;
    private final float panelHeight = 400;
    private float panelX, panelY;

    private final float tabHeight = 35;
    private final float tabWidth = 120;
    private final Category[] mainCategories = {Category.Combat, Category.Movement, Category.Player, Category.Render, Category.Misc};

    private final float moduleHeight = 35;
    private final float moduleSpacing = 5;
    private float modulesScrollOffset = 0;

    private Module waitingForKeybind = null;
    private Map<Module, Boolean> openSettings = new HashMap<>();
    private Map<Module, ModuleAnimState> animStates = new HashMap<>();

    private Module selectedModuleForSettings = null;
    private float settingsScrollOffset = 0;

    private final Color backgroundColor = new Color(20, 20, 25, 255);
    private final Color tabColor = new Color(30, 30, 35);
    private final Color activeTabColor = new Color(45, 125, 255);
    private final Color moduleBgColor = new Color(35, 35, 40);
    private final Color moduleHoverColor = new Color(45, 45, 50);
    private final Color accentColor = new Color(45, 125, 255);
    private final Color borderColor = new Color(60, 60, 70);

    private static class ModuleAnimState {
        float indicatorX = -1;
        float width = 11f;
        long lastUpdate = 0;
    }

    public TabbedGUI(ModuleManager moduleManager) {
        super(Text.literal("Luxury Client"));
        this.moduleManager = moduleManager;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        FontDraw titleFont = FontHelper.monsterrat[22];
        FontDraw mediumFont = FontHelper.monsterrat[16];
        FontDraw smallFont = FontHelper.monsterrat[14];

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(12f, 12f, 12f, 12f), backgroundColor.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(12f, 12f, 12f, 12f), borderColor.getRGB(), 1.5f, 1, 1, false);

        drawTitleBar(context, titleFont);

        drawSearchBar(context, mediumFont, mouseX, mouseY);

        drawTabs(context, mediumFont, mouseX, mouseY);

        drawModulesList(context, mediumFont, smallFont, mouseX, mouseY);

        drawRightPanel(context, mediumFont, smallFont, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x00000000);
    }

    private void drawTitleBar(DrawContext context, FontDraw font) {
        float titleX = panelX + 20;
        float titleY = panelY + 15;

        font.drawAnimatedGradientText(context.getMatrices(), "Luxury Free",
                titleX, titleY, 0xFF45B5FF, 0xFF2D8AFF, System.currentTimeMillis() / 1000f);

        RenderUtil.drawRoundedRect(context.getMatrices(),
                panelX + 20, panelY + 45, panelWidth - 40, 1,
                new Vector4f(0f, 0f, 0f, 0f), new Color(60, 60, 70).getRGB());
    }

    private void drawSearchBar(DrawContext context, FontDraw font, int mouseX, int mouseY) {
        float searchX = panelX + panelWidth - 180;
        float searchY = panelY + 15;
        float searchWidth = 150;
        float searchHeight = 28;

        boolean isHovered = isMouseOver(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight);
        Color bgColor = searchActive ? new Color(40, 40, 50) :
                (isHovered ? new Color(35, 35, 40) : new Color(30, 30, 35));

        RenderUtil.drawRoundedRect(context.getMatrices(), searchX, searchY, searchWidth, searchHeight,
                new Vector4f(6f, 6f, 6f, 6f), bgColor.getRGB());

        if (searchActive) {
            RenderUtil.drawBorder(context.getMatrices(), searchX, searchY, searchWidth, searchHeight,
                    new Vector4f(6f, 6f, 6f, 6f), accentColor.getRGB(), 1f, 1, 1, false);
        }

        font.drawFontLeft(context.getMatrices(), "🔍", searchX + 10, searchY + 7,
                searchActive ? accentColor.getRGB() : new Color(150, 150, 160).getRGB());

        String displayText = searchText;
        if (displayText.isEmpty() && !searchActive) {
            displayText = "Search...";
        }

        float textX = searchX + 35;
        float textY = searchY + 7;
        int textColor = searchActive ? Color.WHITE.getRGB() :
                (searchText.isEmpty() ? new Color(130, 130, 140).getRGB() : Color.WHITE.getRGB());

        float maxTextWidth = searchWidth - 45;
        while (font.getWidth(displayText) > maxTextWidth && displayText.length() > 3) {
            displayText = displayText.substring(0, displayText.length() - 1);
        }

        font.drawFontLeft(context.getMatrices(), displayText, textX, textY, textColor);

        if (searchActive) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCursorBlink > 500) {
                cursorVisible = !cursorVisible;
                lastCursorBlink = currentTime;
            }

            if (cursorVisible) {
                float textWidth = displayText.isEmpty() ? 0 : font.getWidth(displayText);
                font.drawFontLeft(context.getMatrices(), "|", textX + textWidth + 2, textY, accentColor.getRGB());
            }
        }
    }

    private void drawTabs(DrawContext context, FontDraw font, int mouseX, int mouseY) {
        float tabStartY = panelY + 60; // табы
        float tabSpacing = 5;

        for (int i = 0; i < mainCategories.length; i++) {
            Category category = mainCategories[i];
            float tabX = panelX + 20 + (i * (tabWidth + tabSpacing));

            boolean isSelected = category == selectedCategory;
            boolean isHovered = isMouseOver(mouseX, mouseY, tabX, tabStartY, tabWidth, tabHeight);

            Color bgColor;
            if (isSelected) {
                bgColor = activeTabColor;
            } else if (isHovered) {
                bgColor = new Color(40, 40, 50);
            } else {
                bgColor = tabColor;
            }

            RenderUtil.drawRoundedRect(context.getMatrices(), tabX, tabStartY, tabWidth, tabHeight,
                    new Vector4f(6f, 6f, 6f, 6f), bgColor.getRGB());

            if (isSelected) {
                RenderUtil.drawBorder(context.getMatrices(), tabX, tabStartY, tabWidth, tabHeight,
                        new Vector4f(6f, 6f, 6f, 6f), accentColor.getRGB(), 1.5f, 1, 1, false);
            }

            String icon = getCategoryIcon(category);
            String name = getCategoryDisplayName(category);

            float iconX = tabX + 15;
            float iconY = tabStartY + 10;
            float textX = tabX + 40;
            float textY = tabStartY + 12;

            font.drawFontLeft(context.getMatrices(), icon, iconX, iconY,
                    isSelected ? Color.WHITE.getRGB() : new Color(180, 180, 190).getRGB());
            font.drawFontLeft(context.getMatrices(), name, textX, textY,
                    isSelected ? Color.WHITE.getRGB() : new Color(200, 200, 210).getRGB());
        }
    }

    private void drawModulesList(DrawContext context, FontDraw mediumFont, FontDraw smallFont, int mouseX, int mouseY) {
        float listX = panelX + 20;
        float listY = panelY + 110;
        float listWidth = panelWidth - 250;
        float listHeight = panelHeight - 120;

        RenderUtil.drawRoundedRect(context.getMatrices(), listX, listY, listWidth, listHeight,
                new Vector4f(8f, 8f, 8f, 8f), new Color(25, 25, 30).getRGB());

        RenderUtil.drawBorder(context.getMatrices(), listX, listY, listWidth, listHeight,
                new Vector4f(8f, 8f, 8f, 8f), borderColor.getRGB(), 1f, 1, 1, false);

        mediumFont.drawFontLeft(context.getMatrices(), getCategoryDisplayName(selectedCategory),
                listX + 15, listY + 15, Color.WHITE.getRGB());

        int moduleCount = getCategoryModules().size();
        smallFont.drawFontLeft(context.getMatrices(), moduleCount + " modules",
                listX + 15, listY + 35, new Color(150, 150, 160).getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(), listX + 15, listY + 50, listWidth - 30, 1,
                new Vector4f(0, 0, 0, 0), new Color(60, 60, 70).getRGB());

        float moduleStartY = listY + 60;
        float moduleContentHeight = listHeight - 70;
        float visibleAreaTop = listY + 60;
        float visibleAreaBottom = listY + listHeight - 10;

        List<Module> modules = getCategoryModules();
        if (modules.isEmpty()) {
            String message = !searchText.isEmpty() ? "No modules found" : "No modules in this category";
            mediumFont.drawCenteredText(context.getMatrices(), message,
                    listX + listWidth / 2, listY + listHeight / 2, new Color(150, 150, 160).getRGB());
            return;
        }

        float totalHeight = modules.size() * (moduleHeight + moduleSpacing);
        float maxScroll = Math.max(0, totalHeight - moduleContentHeight);
        modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));

        float currentY = moduleStartY - modulesScrollOffset;

        for (Module module : modules) {
            if (currentY + moduleHeight < visibleAreaTop) {
                currentY += moduleHeight + moduleSpacing;
                continue;
            }
            if (currentY > visibleAreaBottom) {
                break;
            }

            drawModuleEntry(context, mediumFont, smallFont, module, listX + 15, currentY,
                    listWidth - 30, mouseX, mouseY);

            currentY += moduleHeight + moduleSpacing;
        }

        if (maxScroll > 0) {
            float scrollBarWidth = 4;
            float scrollBarX = listX + listWidth - 10;
            float scrollBarHeight = moduleContentHeight * (moduleContentHeight / totalHeight);
            float scrollBarY = listY + 60 + (modulesScrollOffset / maxScroll) * (moduleContentHeight - scrollBarHeight);

            RenderUtil.drawRoundedRect(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2), new Color(80, 80, 90).getRGB());

            RenderUtil.drawBorder(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2), new Color(100, 100, 110).getRGB(), 0.5f, 1, 1, false);
        }
    }

    private void drawModuleEntry(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                 Module module, float x, float y, float width, int mouseX, int mouseY) {
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, moduleHeight);
        boolean isEnabled = module.isEnabled();
        boolean isSelectedForSettings = module == selectedModuleForSettings;

        Color bgColor;
        if (isSelectedForSettings) {
            bgColor = new Color(50, 70, 100); // цвет
        } else if (isHovered) {
            bgColor = moduleHoverColor;
        } else {
            bgColor = moduleBgColor;
        }

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, width, moduleHeight,
                new Vector4f(5f, 5f, 5f, 5f), bgColor.getRGB());

        // бордер
        if (isSelectedForSettings) {
            RenderUtil.drawBorder(context.getMatrices(), x, y, width, moduleHeight,
                    new Vector4f(5f, 5f, 5f, 5f), accentColor.getRGB(), 1f, 1, 1, false);
        } else {
            RenderUtil.drawBorder(context.getMatrices(), x, y, width, moduleHeight,
                    new Vector4f(5f, 5f, 5f, 5f), isHovered ? new Color(80, 80, 90).getRGB() : new Color(60, 60, 70).getRGB(),
                    0.5f, 1, 1, false);
        }

        String displayName = waitingForKeybind == module ? "Waiting for key..." : module.getName();
        int nameColor = isEnabled ? accentColor.getRGB() : Color.WHITE.getRGB();
        mediumFont.drawFontLeft(context.getMatrices(), displayName, x + 15, y + 12, nameColor);

        drawModuleToggle(context, module, x + width - 70, y + 10, mouseX, mouseY);

        // Настройка
        if (!module.getSettings().isEmpty()) {
            float settingsBtnX = x + width - 100;
            float settingsBtnY = y + 10;
            float settingsBtnSize = 16;

            boolean settingsHovered = isMouseOver(mouseX, mouseY, settingsBtnX, settingsBtnY,
                    settingsBtnSize, settingsBtnSize);

            String settingsIcon = "⚙";
            int settingsColor = isSelectedForSettings ? accentColor.getRGB() :
                    (settingsHovered ? accentColor.getRGB() : new Color(150, 150, 160).getRGB());

            mediumFont.drawFontLeft(context.getMatrices(), settingsIcon, settingsBtnX, settingsBtnY, settingsColor);

            if (settingsHovered) {
                smallFont.drawFontLeft(context.getMatrices(), "Settings", settingsBtnX - 50, settingsBtnY + 20,
                        new Color(200, 200, 210).getRGB());
            }
        }
    }

    private void drawModuleToggle(DrawContext context, Module module, float x, float y, int mouseX, int mouseY) {
        boolean isEnabled = module.isEnabled();
        float toggleWidth = 50;
        float toggleHeight = 20;

        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, toggleWidth, toggleHeight);
        Color toggleBg = isEnabled ? new Color(45, 125, 255, isHovered ? 200 : 255) :
                new Color(60, 60, 70, isHovered ? 200 : 255);

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, toggleWidth, toggleHeight,
                new Vector4f(8f, 8f, 8f, 8f), toggleBg.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), x, y, toggleWidth, toggleHeight,
                new Vector4f(8f, 8f, 8f, 8f), isHovered ? new Color(100, 100, 110).getRGB() : new Color(80, 80, 90).getRGB(),
                1f, 1, 1, false);

        FontDraw smallFont = FontHelper.monsterrat[12];
        String toggleText = isEnabled ? "ON" : "OFF";
        int textColor = isEnabled ? Color.WHITE.getRGB() : new Color(180, 180, 190).getRGB();

        smallFont.drawCenteredText(context.getMatrices(), toggleText, x + toggleWidth / 2, y + 6, textColor);

        ModuleAnimState state = animStates.computeIfAbsent(module, k -> new ModuleAnimState());
        long currentTime = System.currentTimeMillis();
        long deltaTime = Math.min(currentTime - state.lastUpdate, 16);
        state.lastUpdate = currentTime;

        float targetX = isEnabled ? x + toggleWidth - 15 : x + 5;
        if (state.indicatorX < 0) state.indicatorX = targetX;

        float speed = 0.2f * (deltaTime / 16f);
        float diff = targetX - state.indicatorX;
        state.indicatorX += diff * speed;

        if (Math.abs(diff) < 0.1f) {
            state.indicatorX = targetX;
        }

        float indicatorSize = 10;
        float indicatorY = y + 5;
        Color indicatorColor = isEnabled ? Color.WHITE : new Color(200, 200, 210);

        RenderUtil.drawRoundedRect(context.getMatrices(), state.indicatorX, indicatorY,
                indicatorSize, indicatorSize, new Vector4f(4f, 4f, 4f, 4f), indicatorColor.getRGB());
    }

    private void drawRightPanel(DrawContext context, FontDraw mediumFont, FontDraw smallFont, int mouseX, int mouseY) {
        float panelX = this.panelX + panelWidth - 210;
        float panelY = this.panelY + 110;
        float panelWidth = 190;
        float panelHeight = this.panelHeight - 120;

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(8f, 8f, 8f, 8f), new Color(25, 25, 30).getRGB());

        RenderUtil.drawBorder(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(8f, 8f, 8f, 8f), borderColor.getRGB(), 1f, 1, 1, false);

        if (selectedModuleForSettings != null && !selectedModuleForSettings.getSettings().isEmpty()) {
            drawSettingsPanel(context, smallFont, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        } else {
            drawInfoPanelDefault(context, mediumFont, smallFont, panelX, panelY, panelWidth, panelHeight);
        }
    }

    private void drawSettingsPanel(DrawContext context, FontDraw smallFont,
                                   float panelX, float panelY, float panelWidth, float panelHeight, int mouseX, int mouseY) {

        smallFont.drawFontLeft(context.getMatrices(),
                selectedModuleForSettings.getName() + " Settings",
                panelX + 10, panelY + 10, Color.WHITE.getRGB());

        float closeBtnX = panelX + panelWidth - 20;
        float closeBtnY = panelY + 8;
        boolean closeHovered = isMouseOver(mouseX, mouseY, closeBtnX, closeBtnY, 12, 12);

        smallFont.drawFontLeft(context.getMatrices(), "✕", closeBtnX, closeBtnY,
                closeHovered ? accentColor.getRGB() : Color.WHITE.getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(),
                panelX + 10, panelY + 30, panelWidth - 20, 1,
                new Vector4f(0, 0, 0, 0), new Color(60, 60, 70).getRGB());

        float settingsY = panelY + 35;
        float settingsHeight = panelHeight - 45;
        float settingsContentHeight = calculateSettingsHeight(selectedModuleForSettings);
        float maxScroll = Math.max(0, settingsContentHeight - settingsHeight);
        settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));

        float currentY = settingsY - settingsScrollOffset;

        for (Setting setting : selectedModuleForSettings.getSettings()) {
            SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
            if (renderer != null) {
                float settingHeight = getEstimatedHeight(setting);

                if (currentY + settingHeight < settingsY) {
                    currentY += settingHeight;
                    continue;
                }
                if (currentY > settingsY + settingsHeight) {
                    break;
                }

                RenderUtil.drawRoundedRect(context.getMatrices(),
                        panelX + 10, currentY, panelWidth - 20, settingHeight - 5,
                        new Vector4f(4f, 4f, 4f, 4f), new Color(35, 35, 40).getRGB());

                RenderUtil.drawBorder(context.getMatrices(), panelX + 10, currentY,
                        panelWidth - 20, settingHeight - 5, new Vector4f(4f, 4f, 4f, 4f),
                        new Color(60, 60, 70).getRGB(), 0.5f, 1, 1, false);

                currentY = renderer.render(context, setting, panelX + 15, currentY,
                        panelWidth - 30, mouseX, mouseY, settingsScrollOffset);
            }
        }

        if (maxScroll > 0) {
            float scrollBarWidth = 4;
            float scrollBarX = panelX + panelWidth - 8;
            float scrollBarHeight = settingsHeight * (settingsHeight / settingsContentHeight);
            float scrollBarY = settingsY + (settingsScrollOffset / maxScroll) * (settingsHeight - scrollBarHeight);

            RenderUtil.drawRoundedRect(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2), new Color(80, 80, 90).getRGB());
        }
    }

    private void drawInfoPanelDefault(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                      float panelX, float panelY, float panelWidth, float panelHeight) {

        mediumFont.drawFontLeft(context.getMatrices(), "Information", panelX + 15, panelY + 15, Color.WHITE.getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX + 15, panelY + 40, panelWidth - 30, 1,
                new Vector4f(0, 0, 0, 0), new Color(60, 60, 70).getRGB());

        float infoY = panelY + 55;

        int totalModules = moduleManager.getModules().size();
        smallFont.drawFontLeft(context.getMatrices(), "Total modules: " + totalModules,
                panelX + 15, infoY, new Color(180, 180, 190).getRGB());
        infoY += 20;

        long activeModules = moduleManager.getModules().stream().filter(Module::isEnabled).count();
        smallFont.drawFontLeft(context.getMatrices(), "Active: " + activeModules,
                panelX + 15, infoY, new Color(100, 255, 100).getRGB());
        infoY += 20;

        if (!searchText.isEmpty()) {
            smallFont.drawFontLeft(context.getMatrices(), "Search: " + searchText,
                    panelX + 15, infoY, accentColor.getRGB());
            infoY += 20;
        }

        infoY = panelY + panelHeight - 80;
        smallFont.drawFontLeft(context.getMatrices(), "Instructions:", panelX + 15, infoY,
                new Color(150, 150, 160).getRGB());
        infoY += 15;

        smallFont.drawFontLeft(context.getMatrices(), "• LMB - Toggle module", panelX + 20, infoY,
                new Color(130, 130, 140).getRGB());
        infoY += 12;

        smallFont.drawFontLeft(context.getMatrices(), "• RMB - Open settings", panelX + 20, infoY,
                new Color(130, 130, 140).getRGB());
        infoY += 12;

        smallFont.drawFontLeft(context.getMatrices(), "• MMB - Bind key", panelX + 20, infoY,
                new Color(130, 130, 140).getRGB());
    }

    private float calculateSettingsHeight(Module module) {
        float totalHeight = 0;
        for (Setting setting : module.getSettings()) {
            totalHeight += getEstimatedHeight(setting);
        }
        return totalHeight;
    }

    // иконки блять, твой рендер хуня бесит меня
    private String getCategoryIcon(Category category) {
        switch (category) {
            case Combat: return "⚔";
            case Movement: return "🏃";
            case Player: return "👤";
            case Render: return "👁";
            case Misc: return "📦";
            default: return "📁";
        }
    }

    private String getCategoryDisplayName(Category category) {
        switch (category) {
            case Combat: return "Combat";
            case Movement: return "Movement";
            case Player: return "Player";
            case Render: return "Render";
            case Misc: return "Misc";
            default: return category.name();
        }
    }

    private List<Module> getCategoryModules() {
        if (!searchText.isEmpty()) {
            String searchLower = searchText.toLowerCase();
            return moduleManager.getSorted().stream()
                    .filter(module -> module.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        } else {
            return moduleManager.getSorted().stream()
                    .filter(module -> module.getCategory() == selectedCategory)
                    .collect(Collectors.toList());
        }
    }

    private float getEstimatedHeight(Setting setting) {
        SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
        if (renderer instanceof dev.luxury.ui.settings.ModeListSettingRenderer) {
            return ((dev.luxury.ui.settings.ModeListSettingRenderer) renderer).getEstimatedHeight(setting);
        }
        if (renderer instanceof dev.luxury.ui.settings.ModeSettingRenderer) {
            return ((dev.luxury.ui.settings.ModeSettingRenderer) renderer).getEstimatedHeight(setting);
        }
        return SettingRenderer.SETTING_HEIGHT + SettingRenderer.SETTING_PADDING;
    }

    private boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Map.Entry<Module, Boolean> entry : openSettings.entrySet()) {
            if (entry.getValue() && !entry.getKey().getSettings().isEmpty()) {
                for (Setting setting : entry.getKey().getSettings()) {
                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer instanceof KeySettingRenderer) {
                        KeySettingRenderer keyRenderer = (KeySettingRenderer) renderer;
                        if (keyRenderer.getWaitingKey() != null) {
                            if (keyRenderer.mouseClicked(setting, mouseX, mouseY, button, 0, 0, 0, 0)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        float searchX = panelX + panelWidth - 180;
        float searchY = panelY + 15;
        float searchWidth = 150;
        float searchHeight = 28;

        if (isMouseOver((int)mouseX, (int)mouseY, searchX, searchY, searchWidth, searchHeight)) {
            searchActive = true;
            cursorVisible = true;
            lastCursorBlink = System.currentTimeMillis();
            return true;
        } else {
            searchActive = false;
        }

        float tabStartY = panelY + 60;
        for (int i = 0; i < mainCategories.length; i++) {
            float tabX = panelX + 20 + (i * (tabWidth + 5));
            if (isMouseOver((int)mouseX, (int)mouseY, tabX, tabStartY, tabWidth, tabHeight)) {
                selectedCategory = mainCategories[i];
                modulesScrollOffset = 0;
                selectedModuleForSettings = null;
                return true;
            }
        }

        float rightPanelX = panelX + panelWidth - 210;
        float rightPanelY = panelY + 110;
        float rightPanelWidth = 190;
        float rightPanelHeight = panelHeight - 120;

        if (selectedModuleForSettings != null &&
                isMouseOver((int)mouseX, (int)mouseY, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight)) {

            float closeBtnX = rightPanelX + rightPanelWidth - 20;
            float closeBtnY = rightPanelY + 8;

            if (isMouseOver((int)mouseX, (int)mouseY, closeBtnX, closeBtnY, 12, 12)) {
                selectedModuleForSettings = null;
                settingsScrollOffset = 0;
                return true;
            }

            float settingsY = rightPanelY + 35;
            float settingsHeight = rightPanelHeight - 45;
            float settingsContentHeight = calculateSettingsHeight(selectedModuleForSettings);

            float currentY = settingsY - settingsScrollOffset;

            for (Setting setting : selectedModuleForSettings.getSettings()) {
                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                if (renderer != null) {
                    float settingHeight = getEstimatedHeight(setting);

                    if (currentY + settingHeight < settingsY) {
                        currentY += settingHeight;
                        continue;
                    }
                    if (currentY > settingsY + settingsHeight) {
                        break;
                    }

                    if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX + 10, currentY, rightPanelWidth - 20, settingHeight)) {
                        if (renderer.mouseClicked(setting, mouseX, mouseY, button, rightPanelX + 15, currentY, rightPanelWidth - 30, settingsScrollOffset)) {
                            return true;
                        }
                    }

                    currentY += settingHeight;
                }
            }
        }

        float listX = panelX + 20;
        float listY = panelY + 110;
        float listWidth = panelWidth - 250;
        float listHeight = panelHeight - 120;

        if (isMouseOver((int)mouseX, (int)mouseY, listX, listY, listWidth, listHeight)) {
            float moduleStartY = listY + 60;
            List<Module> modules = getCategoryModules();
            float currentY = moduleStartY - modulesScrollOffset;

            for (Module module : modules) {
                float moduleX = listX + 15;
                float moduleWidth = listWidth - 30;

                if (isMouseOver((int)mouseX, (int)mouseY, moduleX, currentY, moduleWidth, moduleHeight)) {
                    if (button == 0) {
                        module.toggle();
                        return true;
                    } else if (button == 1) {
                        if (!module.getSettings().isEmpty()) {
                            if (selectedModuleForSettings == module) {
                                selectedModuleForSettings = null;
                            } else {
                                selectedModuleForSettings = module;
                                settingsScrollOffset = 0;
                            }
                            return true;
                        }
                    } else if (button == 2) {
                        waitingForKeybind = module;
                        return true;
                    }
                }

                currentY += moduleHeight + moduleSpacing;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float listX = panelX + 20;
        float listY = panelY + 110;
        float listWidth = panelWidth - 250;
        float listHeight = panelHeight - 120;

        if (isMouseOver((int)mouseX, (int)mouseY, listX, listY, listWidth, listHeight)) {
            modulesScrollOffset -= verticalAmount * 15;
            List<Module> modules = getCategoryModules();
            float totalHeight = modules.size() * (moduleHeight + moduleSpacing);
            float maxScroll = Math.max(0, totalHeight - (listHeight - 70));
            modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));
            return true;
        }

        if (selectedModuleForSettings != null) {
            float rightPanelX = panelX + panelWidth - 210;
            float rightPanelY = panelY + 110;
            float rightPanelWidth = 190;
            float rightPanelHeight = panelHeight - 120;

            if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight)) {
                settingsScrollOffset -= verticalAmount * 10;
                float settingsContentHeight = calculateSettingsHeight(selectedModuleForSettings);
                float maxScroll = Math.max(0, settingsContentHeight - (rightPanelHeight - 45));
                settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (selectedModuleForSettings != null) {
            float rightPanelX = panelX + panelWidth - 210;
            float rightPanelY = panelY + 110;
            float rightPanelWidth = 190;
            float rightPanelHeight = panelHeight - 120;

            if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight)) {
                float settingsY = rightPanelY + 35;
                float settingsHeight = rightPanelHeight - 45;
                float currentY = settingsY - settingsScrollOffset;

                for (Setting setting : selectedModuleForSettings.getSettings()) {
                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer instanceof SliderSettingRenderer) {
                        float settingHeight = getEstimatedHeight(setting);

                        if (currentY + settingHeight < settingsY) {
                            currentY += settingHeight;
                            continue;
                        }
                        if (currentY > settingsY + settingsHeight) {
                            break;
                        }

                        if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX + 10, currentY, rightPanelWidth - 20, settingHeight)) {
                            if (((SliderSettingRenderer) renderer).mouseDragged(setting, mouseX, mouseY, button, rightPanelX + 15, rightPanelWidth - 30)) {
                                return true;
                            }
                        }

                        currentY += settingHeight;
                    } else {
                        currentY += getEstimatedHeight(setting);
                    }
                }
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedModuleForSettings != null) {
            for (Setting setting : selectedModuleForSettings.getSettings()) {
                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                if (renderer instanceof SliderSettingRenderer) {
                    ((SliderSettingRenderer) renderer).mouseReleased();
                }
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedModuleForSettings != null) {
            for (Setting setting : selectedModuleForSettings.getSettings()) {
                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                if (renderer instanceof KeySettingRenderer) {
                    KeySettingRenderer keyRenderer = (KeySettingRenderer) renderer;
                    if (keyRenderer.getWaitingKey() == setting) {
                        if (keyRenderer.keyPressed(setting, keyCode)) {
                            return true;
                        }
                    }
                }
            }
        }

        if (waitingForKeybind != null) {
            if (keyCode == 256) { // ESC
                waitingForKeybind.setKey(-1);
            } else {
                waitingForKeybind.setKey(keyCode);
            }
            waitingForKeybind = null;
            return true;
        }

        if (searchActive) {
            if (keyCode == 259) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                searchActive = false;
                return true;
            } else if (keyCode == 256) {
                searchActive = false;
                return true;
            }
        }

        if (keyCode == 256) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchActive) {
            if (chr >= 32 && chr != 127) {
                searchText += chr;
                cursorVisible = true;
                lastCursorBlink = System.currentTimeMillis();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }
}