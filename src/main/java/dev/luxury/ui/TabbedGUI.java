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
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.joml.Vector4f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.awt.Color.white;

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

    private float animationProgress = 0f;
    private boolean opening = true;
    private boolean closing = false;
    private long animationStartTime = 0;
    private static final long ANIMATION_DURATION = 250;

    FontDraw iconFont = FontHelper.icons[18];

    private static class ModuleAnimState {
        float indicatorX = -1;
        float width = 11f;
        long lastUpdate = 0;
    }

    public TabbedGUI(ModuleManager moduleManager) {
        super(Text.literal("LuxuryFree"));
        this.moduleManager = moduleManager;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();

        if (animationStartTime == 0) {
            animationStartTime = currentTime;
        }

        long elapsed = currentTime - animationStartTime;

        if (opening) {
            animationProgress = Math.min(1f, (float) elapsed / ANIMATION_DURATION);
            if (animationProgress >= 1f) {
                opening = false;
            }
        } else if (closing) {
            animationProgress = Math.max(0f, 1f - (float) elapsed / ANIMATION_DURATION);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Обновляем анимацию
        updateAnimation();

        // Пропускаем рендеринг если GUI закрыт
        if (closing && animationProgress <= 0f) {
            this.close();
            return;
        }

        this.renderBackground(context, mouseX, mouseY, delta);

        FontDraw titleFont = FontHelper.monsterrat[22];
        FontDraw mediumFont = FontHelper.monsterrat[16];
        FontDraw smallFont = FontHelper.monsterrat[14];

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();

        float animatedPanelWidth = panelWidth * animationProgress;
        float animatedPanelHeight = panelHeight * animationProgress;

        panelX = (screenWidth - animatedPanelWidth) / 2;
        panelY = (screenHeight - animatedPanelHeight) / 2;

        int bgAlpha = (int)(255 * animationProgress);
        int borderAlpha = (int)(255 * animationProgress);

        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY,
                animatedPanelWidth, animatedPanelHeight,
                new Vector4f(12f, 12f, 12f, 12f),
                new Color(backgroundColor.getRed(), backgroundColor.getGreen(),
                        backgroundColor.getBlue(), bgAlpha).getRGB());

        RenderUtil.drawBorder(context.getMatrices(), panelX, panelY,
                animatedPanelWidth, animatedPanelHeight,
                new Vector4f(12f, 12f, 12f, 12f),
                new Color(borderColor.getRed(), borderColor.getGreen(),
                        borderColor.getBlue(), borderAlpha).getRGB(),
                1.5f, 1, 1, false);

        if (animationProgress < 0.2f) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        float contentAlpha = Math.min(1f, (animationProgress - 0.2f) * 1.25f);

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 0);

        drawTitleBar(context, titleFont);
        drawSearchBar(context, mediumFont, mouseX, mouseY);
        drawTabs(context, mediumFont, mouseX, mouseY);
        drawModulesList(context, mediumFont, smallFont, mouseX, mouseY);
        drawRightPanel(context, mediumFont, smallFont, mouseX, mouseY);

        context.getMatrices().pop();

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

        font.drawFontLeft(context.getMatrices(), displayText, textX - 20, textY + 1.5F, textColor);

        if (searchActive) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCursorBlink > 500) {
                cursorVisible = !cursorVisible;
                lastCursorBlink = currentTime;
            }

            if (cursorVisible) {
                float textWidth = displayText.isEmpty() ? 0 : font.getWidth(displayText);
                font.drawFontLeft(context.getMatrices(), "|", textX + textWidth + 2 - 20, textY + 1.5F, accentColor.getRGB());
            }
        }
    }

    private void drawTabs(DrawContext context, FontDraw font, int mouseX, int mouseY) {
        float tabStartY = panelY + 60;
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

            iconFont.drawFontLeft(context.getMatrices(), icon, iconX, iconY + 2.5F,
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

        float scrollableAreaX = listX + 10;
        float scrollableAreaY = listY + 60;
        float scrollableAreaWidth = listWidth - 20;
        float scrollableAreaHeight = listHeight - 70;

        RenderUtil.enableScissor((int)scrollableAreaX, (int)scrollableAreaY,
                (int)scrollableAreaWidth, (int)scrollableAreaHeight);

        List<Module> modules = getCategoryModules();
        if (modules.isEmpty()) {
            RenderUtil.disableScissor();
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

        RenderUtil.disableScissor();
    }

    private void drawModuleEntry(DrawContext context, FontDraw mediumFont, FontDraw smallFont,
                                 Module module, float x, float y, float width, int mouseX, int mouseY) {
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, moduleHeight);
        boolean isEnabled = module.isEnabled();
        boolean isSelectedForSettings = module == selectedModuleForSettings;

        Color bgColor;
        if (isSelectedForSettings) {
            bgColor = new Color(50, 70, 100);
        } else if (isHovered) {
            bgColor = moduleHoverColor;
        } else {
            bgColor = moduleBgColor;
        }

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, width, moduleHeight,
                new Vector4f(5f, 5f, 5f, 5f), bgColor.getRGB());

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

        if (!module.getSettings().isEmpty()) {
            float settingsBtnX = x + width - 100;
            float settingsBtnY = y + 10;
            float settingsBtnSize = 16;

            boolean settingsHovered = isMouseOver(mouseX, mouseY, settingsBtnX, settingsBtnY,
                    settingsBtnSize, settingsBtnSize);

            String settingsIcon = "R";
            int settingsColor = isSelectedForSettings ? accentColor.getRGB() :
                    (settingsHovered ? accentColor.getRGB() : new Color(150, 150, 160).getRGB());

            iconFont.drawFontLeft(context.getMatrices(), settingsIcon, settingsBtnX, settingsBtnY + 2.5F, settingsColor);
        }
    }

    private void drawModuleToggle(DrawContext context, Module module, float x, float y, int mouseX, int mouseY) {
        boolean isEnabled = module.isEnabled();
        float toggleWidth = 50;
        float toggleHeight = 20;

        boolean isHovered = isMouseOver(mouseX, mouseY, x, y  - 2.5F, toggleWidth, toggleHeight);
        Color toggleBg = isEnabled ? new Color(45, 125, 255, isHovered ? 200 : 255) :
                new Color(60, 60, 70, isHovered ? 200 : 255);

        RenderUtil.drawRoundedRect(context.getMatrices(), x, y - 2.5F, toggleWidth, toggleHeight,
                new Vector4f(8f, 8f, 8f, 8f), toggleBg.getRGB());

        RenderUtil.drawBorder(context.getMatrices(), x, y - 2.5F, toggleWidth, toggleHeight,
                new Vector4f(8f, 8f, 8f, 8f), isHovered ? new Color(100, 100, 110).getRGB() : new Color(80, 80, 90).getRGB(),
                1f, 1, 1, false);

        FontDraw smallFont = FontHelper.monsterrat[12];
        String toggleText = isEnabled ? "ON" : "OFF";
        int textColor = isEnabled ? Color.WHITE.getRGB() : new Color(180, 180, 190).getRGB();

        smallFont.drawCenteredText(context.getMatrices(), toggleText, x + toggleWidth / 2, y + 6 - 2.5F, textColor);

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

        RenderUtil.drawRoundedRect(context.getMatrices(), state.indicatorX, indicatorY - 2.5F,
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

        float scissorX = panelX + 10;
        float scissorY = panelY + 35;
        float scissorWidth = panelWidth - 20;
        float scissorHeight = panelHeight - 45;

        context.enableScissor((int)scissorX, (int)scissorY,
                (int)(scissorX + scissorWidth), (int)(scissorY + scissorHeight));

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

                float newY = renderer.render(context, setting, panelX + 15, currentY,
                        panelWidth - 30, mouseX, mouseY, settingsScrollOffset);
                currentY = newY;
            }
        }

        context.disableScissor();

        if (maxScroll > 0) {
            float scrollBarWidth = 4;
            float scrollBarX = panelX + panelWidth - 8;
            float scrollBarHeight = Math.max(10, settingsHeight * (settingsHeight / settingsContentHeight));
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

    private String getCategoryIcon(Category category) {
        switch (category) {
            case Combat: return "G";
            case Movement: return "O";
            case Player: return "L";
            case Render: return "M";
            case Misc: return "P";
            default: return "R";
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
            float settingsVisibleAreaTop = rightPanelY + 35;
            float settingsVisibleAreaBottom = rightPanelY + rightPanelHeight - 10;

            float currentY = settingsY - settingsScrollOffset;

            for (Setting setting : selectedModuleForSettings.getSettings()) {
                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                if (renderer != null) {
                    float settingHeight = getEstimatedHeight(setting);

                    float actualY = currentY - settingsScrollOffset;

                    if (actualY + settingHeight < settingsVisibleAreaTop) {
                        currentY += settingHeight;
                        continue;
                    }
                    if (actualY > settingsVisibleAreaBottom) {
                        break;
                    }

                    if (isMouseOver((int)mouseX, (int)mouseY,
                            rightPanelX + 10, actualY,
                            rightPanelWidth - 20, settingHeight)) {

                        if (renderer.mouseClicked(setting, mouseX, mouseY, button,
                                rightPanelX + 15, currentY, rightPanelWidth - 30, settingsScrollOffset)) {
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
            float moduleContentHeight = listHeight - 70;
            float maxScroll = Math.max(0, totalHeight - moduleContentHeight);
            modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));
            return true;
        }

        if (selectedModuleForSettings != null) {
            float rightPanelX = panelX + panelWidth - 210;
            float rightPanelY = panelY + 110;
            float rightPanelWidth = 190;
            float rightPanelHeight = panelHeight - 120;

            if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight)) {
                float settingsHeight = rightPanelHeight - 45;
                float settingsContentHeight = calculateSettingsHeight(selectedModuleForSettings);

                float maxScroll = Math.max(0, settingsContentHeight - settingsHeight);

                settingsScrollOffset -= verticalAmount * 10;
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
                float settingsVisibleAreaTop = rightPanelY + 35;
                float settingsVisibleAreaBottom = rightPanelY + rightPanelHeight - 10;

                float currentY = settingsY - settingsScrollOffset;

                for (Setting setting : selectedModuleForSettings.getSettings()) {
                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer instanceof SliderSettingRenderer) {
                        float settingHeight = getEstimatedHeight(setting);

                        float actualY = currentY - settingsScrollOffset;
                        if (actualY + settingHeight < settingsVisibleAreaTop) {
                            currentY += settingHeight;
                            continue;
                        }
                        if (actualY > settingsVisibleAreaBottom) {
                            break;
                        }

                        if (isMouseOver((int)mouseX, (int)mouseY,
                                rightPanelX + 10, actualY,
                                rightPanelWidth - 20, settingHeight)) {
                            if (((SliderSettingRenderer) renderer).mouseDragged(setting, mouseX, mouseY, button,
                                    rightPanelX + 15, rightPanelWidth - 30)) {
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

        if (button == 2) {
            float rightPanelX = panelX + panelWidth - 210;
            float rightPanelY = panelY + 110;
            float rightPanelWidth = 190;
            float rightPanelHeight = panelHeight - 120;

            if (isMouseOver((int)mouseX, (int)mouseY, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight)) {
                float settingsHeight = rightPanelHeight - 45;
                float settingsContentHeight = calculateSettingsHeight(selectedModuleForSettings);
                float maxScroll = Math.max(0, settingsContentHeight - settingsHeight);

                settingsScrollOffset += deltaY * 0.5f;
                settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));
                return true;
            }

            float listX = panelX + 20;
            float listY = panelY + 110;
            float listWidth = panelWidth - 250;
            float listHeight = panelHeight - 120;

            if (isMouseOver((int)mouseX, (int)mouseY, listX, listY, listWidth, listHeight)) {
                float moduleContentHeight = listHeight - 70;
                List<Module> modules = getCategoryModules();
                float totalHeight = modules.size() * (moduleHeight + moduleSpacing);
                float maxScroll = Math.max(0, totalHeight - moduleContentHeight);

                modulesScrollOffset += deltaY * 0.5f;
                modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));
                return true;
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
            if (keyCode == 256) {
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