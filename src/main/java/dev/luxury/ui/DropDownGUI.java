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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DropDownGUI extends Screen {
    private final ModuleManager moduleManager;

    // Основные параметры GUI
    private final float panelWidth = 800;
    private final float panelHeight = 450;
    private float panelX, panelY;

    // Параметры категорий (вкладок)
    private Category selectedCategory = Category.Combat;
    private final float categoryHeight = 40;
    private final float categorySpacing = 5;

    // Параметры модулей
    private final float moduleWidth = 180;
    private final float moduleHeight = 30;
    private final float moduleSpacing = 5;
    private final int modulesPerColumn = 8;
    private float modulesScrollOffset = 0;

    // Окно настроек
    private Module settingsModule = null;
    private final float settingsWidth = 300;
    private final float settingsHeight = 350;
    private float settingsScrollOffset = 0;

    // Поиск
    private String searchText = "";
    private boolean searchActive = false;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;

    // Анимации
    private final Map<Module, ModuleToggleAnim> toggleAnims = new HashMap<>();
    private final Map<Category, CategoryAnim> categoryAnims = new HashMap<>();

    // Цвета
    private final Color backgroundColor = new Color(20, 22, 27, 230);
    private final Color panelColor = new Color(25, 27, 32);
    private final Color accentColor = new Color(45, 125, 255);
    private final Color accentGradientStart = new Color(65, 105, 225);
    private final Color accentGradientEnd = new Color(135, 206, 250);
    private final Color textColor = new Color(230, 230, 235);
    private final Color mutedTextColor = new Color(150, 155, 165);
    private final Color borderColor = new Color(50, 55, 65);
    private final Color moduleBgColor = new Color(35, 37, 43);
    private final Color moduleHoverColor = new Color(42, 45, 52);
    private final Color settingsBgColor = new Color(28, 30, 36, 240);

    public DropDownGUI(ModuleManager moduleManager) {
        super(Text.literal("DropDownGUI"));
        this.moduleManager = moduleManager;

        // Инициализация анимаций для категорий
        for (Category category : Category.values()) {
            categoryAnims.put(category, new CategoryAnim());
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        FontDraw titleFont = FontHelper.monsterrat[24];
        FontDraw mediumFont = FontHelper.monsterrat[18];
        FontDraw smallFont = FontHelper.monsterrat[14];

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();

        // Позиционирование панели по центру
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        // Обновление анимаций
        updateAnimations();

        // Отрисовка основного фона с размытием
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX - 10, panelY - 10,
                panelWidth + 20, panelHeight + 20,
                new Vector4f(15f, 15f, 15f, 15f),
                new Color(10, 12, 17, 180).getRGB());

        // Основная панель
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY,
                panelWidth, panelHeight,
                new Vector4f(12f, 12f, 12f, 12f),
                panelColor.getRGB());

        // Градиентная рамка
        drawGradientBorder(context);

        // Заголовок
        drawTitleBar(context, titleFont, mediumFont);

        // Панель поиска
        drawSearchBar(context, mediumFont, mouseX, mouseY);

        // Вкладки категорий
        drawCategoryTabs(context, mediumFont, mouseX, mouseY);

        // Список модулей
        drawModulesGrid(context, mediumFont, smallFont, mouseX, mouseY);

        // Информационная панель справа
        drawInfoPanel(context, mediumFont, smallFont, mouseX, mouseY);

        // Окно настроек (если открыто)
        if (settingsModule != null) {
            drawSettingsWindow(context, mediumFont, smallFont, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Полупрозрачный черный фон
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                0x80000000);
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();

        // Обновление анимаций категорий
        for (CategoryAnim anim : categoryAnims.values()) {
            anim.update(currentTime);
        }

        // Обновление анимаций переключателей модулей
        for (Module module : moduleManager.getModules()) {
            ModuleToggleAnim anim = toggleAnims.computeIfAbsent(module, k -> new ModuleToggleAnim());
            anim.update(currentTime, module.isEnabled());
        }
    }

    private void drawGradientBorder(DrawContext context) {
        float borderThickness = 1.5f;

        RenderUtil.drawGradientHorizontalLine(context.getMatrices(),
                panelX, panelY, panelWidth, borderThickness,
                accentGradientStart.getRGB(), accentGradientEnd.getRGB());

        RenderUtil.drawGradientHorizontalLine(context.getMatrices(),
                panelX, panelY + panelHeight - borderThickness, panelWidth, borderThickness,
                accentGradientStart.getRGB(), accentGradientEnd.getRGB());

        RenderUtil.drawGradientVerticalLine(context.getMatrices(),
                panelX, panelY, panelHeight, borderThickness,
                accentGradientStart.getRGB(), accentGradientEnd.getRGB());

        RenderUtil.drawGradientVerticalLine(context.getMatrices(),
                panelX + panelWidth - borderThickness, panelY, panelHeight, borderThickness,
                accentGradientEnd.getRGB(), accentGradientStart.getRGB());
    }

    private void drawTitleBar(DrawContext context, FontDraw titleFont, FontDraw mediumFont) {
        float titleX = panelX + 20;
        float titleY = panelY + 20;

        long time = System.currentTimeMillis();
        titleFont.drawAnimatedGradientText(context.getMatrices(),
                "Luxury Client 1.21.4",
                titleX, titleY,
                0xFF4A90E2, 0xFF7EC8FF,
                time / 1000f);

        int enabledCount = (int) moduleManager.getModules().stream()
                .filter(Module::isEnabled).count();
        mediumFont.drawFontLeft(context.getMatrices(),
                "Active: " + enabledCount + "/" + moduleManager.getModules().size(),
                panelX + panelWidth - 150, panelY + 28,
                enabledCount > 0 ? 0xFF00FF00 : mutedTextColor.getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(),
                panelX + 20, panelY + 60, panelWidth - 40, 1,
                new Vector4f(0, 0, 0, 0),
                new Color(60, 65, 75).getRGB());
    }

    private void drawSearchBar(DrawContext context, FontDraw font, int mouseX, int mouseY) {
        float searchX = panelX + 20;
        float searchY = panelY + 70;
        float searchWidth = 250;
        float searchHeight = 32;

        boolean isHovered = isMouseOver(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight);
        Color bgColor = searchActive ? new Color(40, 42, 48) :
                (isHovered ? new Color(38, 40, 46) : new Color(35, 37, 43));

        RenderUtil.drawRoundedRect(context.getMatrices(), searchX, searchY, searchWidth, searchHeight,
                new Vector4f(8f, 8f, 8f, 8f), bgColor.getRGB());

        if (searchActive) {
            RenderUtil.drawBorder(context.getMatrices(), searchX, searchY, searchWidth, searchHeight,
                    new Vector4f(8f, 8f, 8f, 8f), accentColor.getRGB(), 1f, 1, 1, false);
        }

        font.drawFontLeft(context.getMatrices(), "🔍", searchX + 12, searchY + 8,
                searchActive ? accentColor.getRGB() : mutedTextColor.getRGB());

        String displayText = searchText;
        if (displayText.isEmpty() && !searchActive) {
            displayText = "Search modules...";
        }

        float textX = searchX + 40;
        float textY = searchY + 9;
        int textColor = searchActive ? Color.WHITE.getRGB() :
                (searchText.isEmpty() ? mutedTextColor.getRGB() : Color.white.getRGB());

        float maxTextWidth = searchWidth - 55;
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
                float textWidth = font.getWidth(displayText);
                font.drawFontLeft(context.getMatrices(), "|", textX + textWidth + 2, textY, accentColor.getRGB());
            }
        }
    }

    private void drawCategoryTabs(DrawContext context, FontDraw font, int mouseX, int mouseY) {
        float tabsStartX = panelX + 20;
        float tabsStartY = panelY + 115;
        float tabWidth = 120;
        float tabHeight = categoryHeight;

        Category[] categories = Category.values();

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            CategoryAnim anim = categoryAnims.get(category);

            float tabX = tabsStartX + (i * (tabWidth + categorySpacing));

            boolean isSelected = category == selectedCategory;
            boolean isHovered = isMouseOver(mouseX, mouseY, tabX, tabsStartY, tabWidth, tabHeight);

            anim.setHovered(isHovered);
            anim.setSelected(isSelected);

            Color bgColor = anim.getBackgroundColor(isSelected, isHovered);

            Vector4f borderRadius = new Vector4f(8f, 8f, 8f, 8f);
            if (i == 0) {
                borderRadius = new Vector4f(8f, 0f, 0f, 8f);
            } else if (i == categories.length - 1) {
                borderRadius = new Vector4f(0f, 8f, 8f, 0f);
            }

            RenderUtil.drawRoundedRect(context.getMatrices(), tabX, tabsStartY, tabWidth, tabHeight,
                    borderRadius, bgColor.getRGB());

            if (isSelected) {
                drawTabBorder(context, tabX, tabsStartY, tabWidth, tabHeight, borderRadius);
            }

            String icon = getCategoryIcon(category);
            String name = getCategoryDisplayName(category);

            float iconX = tabX + 15;
            float iconY = tabsStartY + 11;
            float textX = tabX + 45;
            float textY = tabsStartY + 12;

            int iconColor = isSelected ? Color.WHITE.getRGB() :
                    (isHovered ? accentColor.getRGB() : mutedTextColor.getRGB());
            int textColor = isSelected ? Color.WHITE.getRGB() : Color.WHITE.getRGB();

            font.drawFontLeft(context.getMatrices(), icon, iconX, iconY, iconColor);
            font.drawFontLeft(context.getMatrices(), name, textX, textY, textColor);

            int moduleCount = getCategoryModules(category).size();
            if (moduleCount > 0) {
                float countX = tabX + tabWidth - 25;
                float countY = tabsStartY + 12;

                Color countBg = isSelected ? new Color(255, 255, 255, 40) :
                        new Color(255, 255, 255, 20);
                float countWidth = font.getWidth(String.valueOf(moduleCount)) + 10;

                RenderUtil.drawRoundedRect(context.getMatrices(),
                        countX - 5, countY - 3, countWidth, 18,
                        new Vector4f(4f, 4f, 4f, 4f), countBg.getRGB());

                font.drawCenteredText(context.getMatrices(),
                        String.valueOf(moduleCount),
                        countX + countWidth/2 - 5, countY + 1,
                        isSelected ? Color.WHITE.getRGB() : mutedTextColor.getRGB());
            }
        }
    }

    private void drawTabBorder(DrawContext context, float x, float y, float width, float height, Vector4f radius) {
        float borderThickness = 2f;

        RenderUtil.drawGradientHorizontalLine(context.getMatrices(),
                x, y, width, borderThickness,
                accentGradientStart.getRGB(), accentGradientEnd.getRGB());

        RenderUtil.drawGradientHorizontalLine(context.getMatrices(),
                x, y + height - borderThickness, width, borderThickness,
                accentGradientEnd.getRGB(), accentGradientStart.getRGB());

        if (radius.x > 0) {
            RenderUtil.drawRoundedRect(context.getMatrices(),
                    x, y, borderThickness, height,
                    new Vector4f(radius.x, 0, 0, radius.w),
                    accentGradientStart.getRGB());
        }

        if (radius.y > 0) {
            RenderUtil.drawRoundedRect(context.getMatrices(),
                    x + width - borderThickness, y, borderThickness, height,
                    new Vector4f(0, radius.y, radius.z, 0),
                    accentGradientEnd.getRGB());
        }
    }

    private void drawModulesGrid(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                 int mouseX, int mouseY) {
        float gridX = panelX + 20;
        float gridY = panelY + 165;
        float gridWidth = panelWidth - 300;
        float gridHeight = panelHeight - 185;

        RenderUtil.drawRoundedRect(context.getMatrices(), gridX, gridY, gridWidth, gridHeight,
                new Vector4f(8f, 8f, 8f, 8f), new Color(30, 32, 38).getRGB());

        mediumFont.drawFontLeft(context.getMatrices(),
                getCategoryDisplayName(selectedCategory) + " Modules",
                gridX + 15, gridY + 15, textColor.getRGB());

        List<Module> modules = getCategoryModules(selectedCategory);
        if (searchText != null && !searchText.isEmpty()) {
            modules = getSearchedModules();
        }

        if (modules.isEmpty()) {
            String message = !searchText.isEmpty() ? "No modules found" : "No modules in this category";
            mediumFont.drawCenteredText(context.getMatrices(), message,
                    gridX + gridWidth / 2, gridY + gridHeight / 2, mutedTextColor.getRGB());
            return;
        }

        int columns = (int) Math.ceil(gridWidth / (moduleWidth + moduleSpacing));
        columns = Math.max(1, Math.min(columns, 3));

        float visibleHeight = gridHeight - 40;
        float totalHeight = (float) Math.ceil(modules.size() / (float) columns) * (moduleHeight + moduleSpacing);
        float maxScroll = Math.max(0, totalHeight - visibleHeight);
        modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));

        float startY = gridY + 40 - modulesScrollOffset;
        float columnWidth = (gridWidth - 30) / columns;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % columns;
            int row = i / columns;

            float moduleX = gridX + 15 + (col * (columnWidth + moduleSpacing));
            float moduleY = startY + (row * (moduleHeight + moduleSpacing));

            if (moduleY + moduleHeight < gridY + 40 || moduleY > gridY + gridHeight) {
                continue;
            }

            drawModuleEntry(context, mediumFont, smallFont, module,
                    moduleX, moduleY, columnWidth, mouseX, mouseY);
        }

        if (maxScroll > 0) {
            float scrollBarWidth = 4;
            float scrollBarX = gridX + gridWidth - 10;
            float scrollBarHeight = visibleHeight * (visibleHeight / totalHeight);
            float scrollBarY = gridY + 40 + (modulesScrollOffset / maxScroll) * (visibleHeight - scrollBarHeight);

            RenderUtil.drawRoundedRect(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2),
                    new Color(80, 85, 95).getRGB());

            RenderUtil.drawBorder(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2),
                    new Color(100, 105, 115).getRGB(), 0.5f, 1, 1, false);
        }
    }

    private void drawModuleEntry(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                 Module module, float x, float y, float width, int mouseX, int mouseY) {
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, moduleHeight);
        boolean isEnabled = module.isEnabled();

        Color bgColor = isHovered ? moduleHoverColor : moduleBgColor;

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, width, moduleHeight,
                new Vector4f(6f, 6f, 6f, 6f), bgColor.getRGB());

        if (isHovered) {
            RenderUtil.drawBorder(context.getMatrices(), x, y, width, moduleHeight,
                    new Vector4f(6f, 6f, 6f, 6f),
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100).getRGB(),
                    1f, 1, 1, false);
        }

        String displayName = module.getName();
        int nameColor = isEnabled ? accentColor.getRGB() : textColor.getRGB();
        mediumFont.drawFontLeft(context.getMatrices(), displayName, x + 10, y + 9, nameColor);

        if (!module.getSettings().isEmpty()) {
            float settingsX = x + width - 60;
            float settingsY = y + 9;

            boolean settingsHovered = isMouseOver(mouseX, mouseY, settingsX, settingsY, 20, 15);
            String settingsIcon = "⚙";
            int settingsColor = settingsHovered ? accentColor.getRGB() : mutedTextColor.getRGB();

            mediumFont.drawFontLeft(context.getMatrices(), settingsIcon, settingsX, settingsY, settingsColor);

            if (settingsHovered) {
                smallFont.drawFontLeft(context.getMatrices(), "Right click for settings",
                        x + width / 2 - 40, y + moduleHeight + 5,
                        new Color(200, 200, 210, 180).getRGB());
            }
        }

        drawModuleToggle(context, module, x + width - 40, y + 7, mouseX, mouseY);
    }

    private void drawModuleToggle(DrawContext context, Module module, float x, float y, int mouseX, int mouseY) {
        boolean isEnabled = module.isEnabled();
        ModuleToggleAnim anim = toggleAnims.computeIfAbsent(module, k -> new ModuleToggleAnim());

        float toggleWidth = 30;
        float toggleHeight = 16;
        float indicatorSize = 10;

        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, toggleWidth, toggleHeight);

        Color toggleBg = isEnabled ?
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), isHovered ? 200 : 255) :
                new Color(60, 65, 75, isHovered ? 200 : 255);

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, toggleWidth, toggleHeight,
                new Vector4f(8f, 8f, 8f, 8f), toggleBg.getRGB());

        float indicatorX = anim.getIndicatorX(x, toggleWidth, indicatorSize, isEnabled);
        float indicatorY = y + 3;

        Color indicatorColor = isEnabled ? Color.WHITE : new Color(200, 205, 215);

        RenderUtil.drawRoundedRect(context.getMatrices(), indicatorX, indicatorY,
                indicatorSize, indicatorSize, new Vector4f(5f, 5f, 5f, 5f), indicatorColor.getRGB());

        if (isEnabled && anim.isRecentlyToggled()) {
            float glowSize = indicatorSize + 4;
            float glowX = indicatorX - 2;
            float glowY = indicatorY - 2;

            RenderUtil.drawRoundedRect(context.getMatrices(), glowX, glowY,
                    glowSize, glowSize, new Vector4f(6f, 6f, 6f, 6f),
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 80).getRGB());
        }
    }

    private void drawInfoPanel(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                               int mouseX, int mouseY) {
        float panelX = this.panelX + panelWidth - 260;
        float panelY = this.panelY + 115;
        float panelWidth = 240;
        float panelHeight = this.panelHeight - 135;

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                new Vector4f(8f, 8f, 8f, 8f), new Color(30, 32, 38).getRGB());

        RenderUtil.drawGradientHorizontalRect(context.getMatrices(),
                panelX, panelY, panelWidth, 40,
                new Color(45, 125, 255, 50).getRGB(),
                new Color(135, 206, 250, 30).getRGB());

        mediumFont.drawFontLeft(context.getMatrices(), "Quick Info",
                panelX + 15, panelY + 12, textColor.getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(),
                panelX + 15, panelY + 35, panelWidth - 30, 1,
                new Vector4f(0, 0, 0, 0), new Color(60, 65, 75).getRGB());

        float infoY = panelY + 50;

        int categoryModules = getCategoryModules(selectedCategory).size();
        smallFont.drawFontLeft(context.getMatrices(),
                "Category: " + getCategoryDisplayName(selectedCategory),
                panelX + 15, infoY, accentColor.getRGB());
        infoY += 20;

        smallFont.drawFontLeft(context.getMatrices(),
                "Modules: " + categoryModules,
                panelX + 15, infoY, mutedTextColor.getRGB());
        infoY += 20;

        int totalEnabled = (int) moduleManager.getModules().stream()
                .filter(Module::isEnabled).count();
        int totalModules = moduleManager.getModules().size();

        smallFont.drawFontLeft(context.getMatrices(),
                "Total Enabled: " + totalEnabled + "/" + totalModules,
                panelX + 15, infoY,
                totalEnabled > 0 ? 0xFF00FF00 : mutedTextColor.getRGB());
        infoY += 25;

        List<Module> recentModules = moduleManager.getModules().stream()
                .filter(Module::isEnabled)
                .limit(3)
                .collect(Collectors.toList());

        if (!recentModules.isEmpty()) {
            smallFont.drawFontLeft(context.getMatrices(), "Active Modules:",
                    panelX + 15, infoY, textColor.getRGB());
            infoY += 15;

            for (Module mod : recentModules) {
                smallFont.drawFontLeft(context.getMatrices(), "• " + mod.getName(),
                        panelX + 20, infoY, accentColor.getRGB());
                infoY += 15;
            }
        }

        infoY = panelY + panelHeight - 70;
        smallFont.drawFontLeft(context.getMatrices(), "Controls:",
                panelX + 15, infoY, mutedTextColor.getRGB());
        infoY += 15;

        smallFont.drawFontLeft(context.getMatrices(), "LMB - Toggle module",
                panelX + 20, infoY, new Color(130, 135, 145).getRGB());
        infoY += 12;

        smallFont.drawFontLeft(context.getMatrices(), "RMB - Open settings",
                panelX + 20, infoY, new Color(130, 135, 145).getRGB());
        infoY += 12;

        smallFont.drawFontLeft(context.getMatrices(), "MMB - Bind key",
                panelX + 20, infoY, new Color(130, 135, 145).getRGB());
    }

    private void drawSettingsWindow(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                    int mouseX, int mouseY) {
        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();

        float windowX = (screenWidth - settingsWidth) / 2;
        float windowY = (screenHeight - settingsHeight) / 2;

        context.fill(0, 0, (int) screenWidth, (int) screenHeight, 0x80000000);

        RenderUtil.drawRoundedRect(context.getMatrices(),
                windowX - 5, windowY - 5, settingsWidth + 10, settingsHeight + 10,
                new Vector4f(12f, 12f, 12f, 12f), new Color(10, 12, 17, 200).getRGB());

        RenderUtil.drawRoundedRect(context.getMatrices(),
                windowX, windowY, settingsWidth, settingsHeight,
                new Vector4f(10f, 10f, 10f, 10f), settingsBgColor.getRGB());

        drawWindowBorder(context, windowX, windowY, settingsWidth, settingsHeight);

        drawSettingsHeader(context, mediumFont, windowX, windowY, mouseX, mouseY);

        drawSettingsContent(context, smallFont, windowX, windowY, mouseX, mouseY);
    }

    private void drawWindowBorder(DrawContext context, float x, float y, float width, float height) {
        float borderThickness = 1.5f;

        RenderUtil.drawGradientRect(context.getMatrices(),
                x - borderThickness, y - borderThickness,
                width + borderThickness * 2, height + borderThickness * 2,
                new Vector4f(10f + borderThickness, 10f + borderThickness, 10f + borderThickness, 10f + borderThickness),
                accentGradientStart.getRGB(), accentGradientEnd.getRGB(),
                accentGradientEnd.getRGB(), accentGradientStart.getRGB());
    }

    private void drawSettingsHeader(DrawContext context, FontDraw font, float windowX, float windowY, int mouseX, int mouseY) {

        RenderUtil.drawGradientHorizontalRect(context.getMatrices(),
                windowX, windowY, settingsWidth, 50,
                new Color(45, 125, 255, 80).getRGB(),
                new Color(135, 206, 250, 60).getRGB());

        font.drawFontLeft(context.getMatrices(),
                settingsModule.getName() + " Settings",
                windowX + 20, windowY + 17, textColor.getRGB());

        float closeX = windowX + settingsWidth - 35;
        float closeY = windowY + 15;

        boolean closeHovered = isMouseOver(mouseX, mouseY, closeX, closeY, 20, 20);

        String closeIcon = "✕";
        int closeColor = closeHovered ? accentColor.getRGB() : textColor.getRGB();

        font.drawFontLeft(context.getMatrices(), closeIcon, closeX, closeY, closeColor);

        RenderUtil.drawRoundedRect(context.getMatrices(),
                windowX + 20, windowY + 45, settingsWidth - 40, 1,
                new Vector4f(0, 0, 0, 0), new Color(60, 65, 75).getRGB());
    }

    private void drawSettingsContent(DrawContext context, FontDraw font,
                                     float windowX, float windowY, int mouseX, int mouseY) {
        float contentX = windowX + 20;
        float contentY = windowY + 55;
        float contentWidth = settingsWidth - 40;
        float contentHeight = settingsHeight - 75;

        List<Setting> settings = settingsModule.getSettings();
        if (settings.isEmpty()) {
            font.drawCenteredText(context.getMatrices(), "No settings available",
                    windowX + settingsWidth / 2, windowY + settingsHeight / 2,
                    mutedTextColor.getRGB());
            return;
        }

        // Расчет высоты контента
        float totalHeight = 0;
        for (Setting setting : settings) {
            totalHeight += getEstimatedHeight(setting);
        }

        float maxScroll = Math.max(0, totalHeight - contentHeight);
        settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));

        float currentY = contentY - settingsScrollOffset;

        for (Setting setting : settings) {
            if (currentY + getEstimatedHeight(setting) < contentY) {
                currentY += getEstimatedHeight(setting);
                continue;
            }
            if (currentY > contentY + contentHeight) {
                break;
            }

            SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
            if (renderer != null) {
                // Фон настройки
                RenderUtil.drawRoundedRect(context.getMatrices(),
                        contentX, currentY, contentWidth, getEstimatedHeight(setting) - 5,
                        new Vector4f(6f, 6f, 6f, 6f), new Color(40, 42, 48).getRGB());

                currentY = renderer.render(context, setting, contentX + 10, currentY,
                        contentWidth - 20, mouseX, mouseY, settingsScrollOffset);
            }
        }

        // Полоса прокрутки
        if (maxScroll > 0) {
            float scrollBarWidth = 4;
            float scrollBarX = windowX + settingsWidth - 15;
            float scrollBarHeight = contentHeight * (contentHeight / totalHeight);
            float scrollBarY = contentY + (settingsScrollOffset / maxScroll) * (contentHeight - scrollBarHeight);

            RenderUtil.drawRoundedRect(context.getMatrices(), scrollBarX, scrollBarY,
                    scrollBarWidth, scrollBarHeight, new Vector4f(2, 2, 2, 2),
                    new Color(80, 85, 95).getRGB());
        }
    }

    private String getCategoryIcon(Category category) {
        switch (category) {
            case Combat: return "⚔";
            case Movement: return "🏃";
            case Player: return "👤";
            case Render: return "👁";
            case Misc: return "⚙";
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

    private List<Module> getCategoryModules(Category category) {
        return moduleManager.getSorted().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    private List<Module> getSearchedModules() {
        String searchLower = searchText.toLowerCase();
        return moduleManager.getSorted().stream()
                .filter(module -> module.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }

    private float getEstimatedHeight(Setting setting) {
        SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
        if (renderer != null) {
            if (renderer instanceof dev.luxury.ui.settings.ModeListSettingRenderer) {
                return ((dev.luxury.ui.settings.ModeListSettingRenderer) renderer).getEstimatedHeight(setting);
            }
            if (renderer instanceof dev.luxury.ui.settings.ModeSettingRenderer) {
                return ((dev.luxury.ui.settings.ModeSettingRenderer) renderer).getEstimatedHeight(setting);
            }
        }
        return SettingRenderer.SETTING_HEIGHT + SettingRenderer.SETTING_PADDING;
    }

    private boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка окна настроек
        if (settingsModule != null) {
            float screenWidth = this.client.getWindow().getScaledWidth();
            float screenHeight = this.client.getWindow().getScaledHeight();
            float windowX = (screenWidth - settingsWidth) / 2;
            float windowY = (screenHeight - settingsHeight) / 2;

            // Кнопка закрытия
            float closeX = windowX + settingsWidth - 35;
            float closeY = windowY + 15;

            if (isMouseOver((int) mouseX, (int) mouseY, closeX, closeY, 20, 20)) {
                settingsModule = null;
                settingsScrollOffset = 0;
                return true;
            }

            // Клики по настройкам
            if (mouseX >= windowX && mouseX <= windowX + settingsWidth &&
                    mouseY >= windowY && mouseY <= windowY + settingsHeight) {

                float contentX = windowX + 20;
                float contentY = windowY + 55;
                float contentWidth = settingsWidth - 40;
                float contentHeight = settingsHeight - 75;

                float currentY = contentY - settingsScrollOffset;

                for (Setting setting : settingsModule.getSettings()) {
                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer != null) {
                        float settingHeight = getEstimatedHeight(setting);

                        if (isMouseOver((int) mouseX, (int) mouseY,
                                (int) contentX, (int) currentY,
                                (int) contentWidth, (int) settingHeight)) {

                            if (renderer.mouseClicked(setting, mouseX, mouseY, button,
                                    contentX + 10, currentY, contentWidth - 20, settingsScrollOffset)) {
                                return true;
                            }
                        }

                        currentY += settingHeight;
                    }
                }
            }

            // Клик вне окна закрывает его
            if (!isMouseOver((int) mouseX, (int) mouseY,
                    (int) windowX, (int) windowY,
                    (int) settingsWidth, (int) settingsHeight)) {
                settingsModule = null;
                settingsScrollOffset = 0;
                return true;
            }
        }

        // Поисковая строка
        float searchX = panelX + 20;
        float searchY = panelY + 70;
        float searchWidth = 250;
        float searchHeight = 32;

        if (isMouseOver((int) mouseX, (int) mouseY, searchX, searchY, searchWidth, searchHeight)) {
            searchActive = true;
            cursorVisible = true;
            lastCursorBlink = System.currentTimeMillis();
            return true;
        } else {
            searchActive = false;
        }

        // Вкладки категорий
        float tabsStartX = panelX + 20;
        float tabsStartY = panelY + 115;
        float tabWidth = 120;
        float tabHeight = categoryHeight;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            float tabX = tabsStartX + (i * (tabWidth + categorySpacing));
            if (isMouseOver((int) mouseX, (int) mouseY, tabX, tabsStartY, tabWidth, tabHeight)) {
                selectedCategory = categories[i];
                modulesScrollOffset = 0;
                return true;
            }
        }

        // Сетка модулей
        float gridX = panelX + 20;
        float gridY = panelY + 165;
        float gridWidth = panelWidth - 300;
        float gridHeight = panelHeight - 185;

        if (isMouseOver((int) mouseX, (int) mouseY, gridX, gridY, gridWidth, gridHeight)) {
            List<Module> modules = getCategoryModules(selectedCategory);
            if (searchText != null && !searchText.isEmpty()) {
                modules = getSearchedModules();
            }

            int columns = (int) Math.ceil(gridWidth / (moduleWidth + moduleSpacing));
            columns = Math.max(1, Math.min(columns, 3));

            float startY = gridY + 40 - modulesScrollOffset;
            float columnWidth = (gridWidth - 30) / columns;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                int col = i % columns;
                int row = i / columns;

                float moduleX = gridX + 15 + (col * (columnWidth + moduleSpacing));
                float moduleY = startY + (row * (moduleHeight + moduleSpacing));

                if (isMouseOver((int) mouseX, (int) mouseY,
                        (int) moduleX, (int) moduleY,
                        (int) columnWidth, (int) moduleHeight)) {

                    if (button == 0) { // ЛКМ
                        module.toggle();
                        ModuleToggleAnim anim = toggleAnims.computeIfAbsent(module, k -> new ModuleToggleAnim());
                        anim.setToggled();
                        return true;
                    } else if (button == 1) { // ПКМ
                        if (!module.getSettings().isEmpty()) {
                            settingsModule = module;
                            settingsScrollOffset = 0;
                            return true;
                        }
                    } else if (button == 2) { // Средняя кнопка
                        // Кейбинд будет обработан в keyPressed
                        settingsModule = null;
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Скролл в сетке модулей
        float gridX = panelX + 20;
        float gridY = panelY + 165;
        float gridWidth = panelWidth - 300;
        float gridHeight = panelHeight - 185;

        if (isMouseOver((int) mouseX, (int) mouseY, gridX, gridY, gridWidth, gridHeight)) {
            modulesScrollOffset -= verticalAmount * 15;
            List<Module> modules = getCategoryModules(selectedCategory);

            int columns = (int) Math.ceil(gridWidth / (moduleWidth + moduleSpacing));
            columns = Math.max(1, Math.min(columns, 3));

            float totalHeight = (float) Math.ceil(modules.size() / (float) columns) * (moduleHeight + moduleSpacing);
            float maxScroll = Math.max(0, totalHeight - (gridHeight - 40));
            modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));
            return true;
        }

        // Скролл в окне настроек
        if (settingsModule != null) {
            float screenWidth = this.client.getWindow().getScaledWidth();
            float screenHeight = this.client.getWindow().getScaledHeight();
            float windowX = (screenWidth - settingsWidth) / 2;
            float windowY = (screenHeight - settingsHeight) / 2;

            if (isMouseOver((int) mouseX, (int) mouseY,
                    (int) windowX, (int) windowY,
                    (int) settingsWidth, (int) settingsHeight)) {

                settingsScrollOffset -= verticalAmount * 10;
                float totalHeight = calculateSettingsHeight(settingsModule);
                float maxScroll = Math.max(0, totalHeight - (settingsHeight - 75));
                settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (settingsModule != null && button == 0) {
            float screenWidth = this.client.getWindow().getScaledWidth();
            float screenHeight = this.client.getWindow().getScaledHeight();
            float windowX = (screenWidth - settingsWidth) / 2;
            float windowY = (screenHeight - settingsHeight) / 2;

            float contentX = windowX + 20;
            float contentY = windowY + 55;
            float contentWidth = settingsWidth - 40;
            float contentHeight = settingsHeight - 75;

            float currentY = contentY - settingsScrollOffset;

            for (Setting setting : settingsModule.getSettings()) {
                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                if (renderer instanceof SliderSettingRenderer) {
                    float settingHeight = getEstimatedHeight(setting);

                    if (isMouseOver((int) mouseX, (int) mouseY,
                            (int) contentX, (int) currentY,
                            (int) contentWidth, (int) settingHeight)) {

                        if (((SliderSettingRenderer) renderer).mouseDragged(setting, mouseX, mouseY,
                                button, contentX + 10, contentWidth - 20)) {
                            return true;
                        }
                    }

                    currentY += settingHeight;
                } else {
                    currentY += getEstimatedHeight(setting);
                }
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (settingsModule != null && button == 0) {
            for (Setting setting : settingsModule.getSettings()) {
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
        // ESC закрывает GUI
        if (keyCode == 256) {
            this.close();
            return true;
        }

        // Поиск
        if (searchActive) {
            if (keyCode == 259) { // Backspace
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter
                searchActive = false;
                return true;
            } else if (keyCode == 256) { // Escape
                searchActive = false;
                return true;
            }
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

    private float calculateSettingsHeight(Module module) {
        float totalHeight = 0;
        for (Setting setting : module.getSettings()) {
            totalHeight += getEstimatedHeight(setting);
        }
        return totalHeight;
    }

    // Классы анимаций
    private static class CategoryAnim {
        private float hoverProgress = 0;
        private float selectProgress = 0;
        private long lastUpdate = System.currentTimeMillis();

        public void setHovered(boolean hovered) {
            float target = hovered ? 1.0f : 0.0f;
            updateProgress(target, 0.1f);
        }

        public void setSelected(boolean selected) {
            float target = selected ? 1.0f : 0.0f;
            updateProgress(target, 0.15f);
        }

        private void updateProgress(float target, float speed) {
            long currentTime = System.currentTimeMillis();
            float delta = (currentTime - lastUpdate) / 16f;
            lastUpdate = currentTime;

            selectProgress += (target - selectProgress) * speed * delta;
            selectProgress = Math.max(0, Math.min(1, selectProgress));
        }

        public void update(long currentTime) {
            // Обновление анимации
        }

        public Color getBackgroundColor(boolean selected, boolean hovered) {
            if (selected) {
                return new Color(45, 125, 255, 100 + (int)(selectProgress * 155));
            } else if (hovered) {
                return new Color(50, 55, 65, 200);
            } else {
                return new Color(40, 42, 48, 180);
            }
        }
    }

    private static class ModuleToggleAnim {
        private float position = 0;
        private long lastToggleTime = 0;
        private boolean toggled = false;

        public void update(long currentTime, boolean isEnabled) {
            float target = isEnabled ? 1.0f : 0.0f;
            float speed = toggled ? 0.3f : 0.15f;

            position += (target - position) * speed;

            if (currentTime - lastToggleTime > 300) {
                toggled = false;
            }
        }

        public float getIndicatorX(float x, float toggleWidth, float indicatorSize, boolean isEnabled) {
            float maxTravel = toggleWidth - indicatorSize - 4;
            float currentPos = isEnabled ? position : 1 - position;
            return x + 2 + (currentPos * maxTravel);
        }

        public void setToggled() {
            toggled = true;
            lastToggleTime = System.currentTimeMillis();
        }

        public boolean isRecentlyToggled() {
            return System.currentTimeMillis() - lastToggleTime < 300;
        }
    }
}