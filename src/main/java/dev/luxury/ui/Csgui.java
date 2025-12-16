package dev.luxury.ui;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.ui.settings.SettingRenderer;
import dev.luxury.ui.settings.SettingRendererManager;
import dev.luxury.ui.settings.KeySettingRenderer;
import dev.luxury.ui.settings.SliderSettingRenderer;
import dev.luxury.ui.settings.ModeListSettingRenderer;
import dev.luxury.ui.settings.ModeSettingRenderer;
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

public class Csgui extends Screen {
    private Category selectedCategory = Category.Combat;
    private final ModuleManager moduleManager;
    private String searchText = "";
    private boolean searchActive = false;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;
    private float panelX;
    private float panelY;
    private final float panelWidth = 400;
    private final float panelHeight = 260;
    private final float categoryHeight = 25;
    private final float moduleHeight = 25;
    private float lastPanelWidth = -1;
    private float lastPanelHeight = -1;

    private Module waitingForKeybind = null;

    private Map<Module, Boolean> openSettings = new HashMap<>();
    private Map<Module, Float> settingsScrollOffsets = new HashMap<>();

    private float modulesScrollOffset = 0f;
    private final float moduleSpacing = 3f;
    private final float moduleAreaTop = 30f;
    private final float moduleAreaBottom = 10f;

    private static class ModuleAnimState {
        float indicatorX = -1;
        float width = 11f;
        long lastUpdate = 0;

        public ModuleAnimState() {
        }
    }

    private Map<Module, ModuleAnimState> animStates = new HashMap<>();

    public Csgui(ModuleManager moduleManager) {
        super(Text.literal("Csgui"));
        this.moduleManager = moduleManager;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        FontDraw montserratBig = FontHelper.monsterrat[20];
        FontDraw montserratMedium = FontHelper.monsterrat[16];

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        // Основной фон GUI
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, 121, panelHeight, new Vector4f(8f, 8f, 0f, 0f), Color.gray.getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX + 120, panelY, 400 - 134, panelHeight, new Vector4f(0f, 0f, 8f, 8f), new Color(9, 10, 10).getRGB());

        // Разделительные линии
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY + 35, 385, 2, new Vector4f(0f, 0f, 0f, 0f), new Color(145, 145, 145, 100).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX + 120, panelY + 36, 2, 225, new Vector4f(0f, 0f, 0f, 0f), new Color(145, 145, 145, 50).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX, panelY + 200, 121, 2, new Vector4f(0f, 0f, 0f, 0f), new Color(145, 145, 145, 100).getRGB());

        // Поисковая панель
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX + 300, panelY + 9.5f, 75, 17.5f, new Vector4f(8, 8, 8, 8), new Color(145, 145, 145, 50).getRGB());

        // Заголовок
        montserratBig.drawAnimatedGradientText(context.getMatrices(), "Luxury 1.21.4", panelX + 25, panelY + 20, 0xFFff910e, 0xFFf5cc22, System.currentTimeMillis() / 1000f);

        // Отрисовка категорий
        float currentY = panelY + 45;
        float categoryX = panelX + 10;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = isMouseOver(mouseX, mouseY, categoryX, currentY, 100, categoryHeight);

            int bgColor = isSelected ? 0xFF2196F3 : (isHovered ? 0xFF1976D2 : 0xFF1a1a1a);

            RenderUtil.drawRoundedRect(context.getMatrices(), categoryX, currentY, 100, categoryHeight, new Vector4f(8f, 8f, 8f, 8f), bgColor);

            int textColor = isSelected || isHovered ? Color.white.getRGB() : 0xFFaaaaaa;
            montserratMedium.drawFontLeft(context.getMatrices(), category.name(), categoryX + 10, currentY + 9, textColor);
            currentY += categoryHeight + 5;
        }

        drawModules(context, mouseX, mouseY);
        searchPanel(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private Category lastSelectedCategory = null;

    private void drawModules(DrawContext context, int mouseX, int mouseY) {
        FontDraw montserratMedium = FontHelper.monsterrat[16];
        List<Module> categoryModules = getCategoryModules();

        if (lastSelectedCategory != selectedCategory && searchText.isEmpty()) {
            animStates.clear();
            openSettings.clear();
            settingsScrollOffsets.clear();
            lastSelectedCategory = selectedCategory;
        }

        if (lastPanelWidth != panelWidth || lastPanelHeight != panelHeight) {
            animStates.clear();
            lastPanelWidth = panelWidth;
            lastPanelHeight = panelHeight;
        }

        float moduleStartX = panelX + 125;
        float startModuleY = panelY + moduleAreaTop;
        float totalModuleWidth = panelWidth - 150;
        float moduleWidth = (totalModuleWidth - 15) / 2;
        float columnGap = 10;
        float visibleAreaTop = panelY + moduleAreaTop;
        float visibleAreaBottom = panelY + panelHeight - moduleAreaBottom;
        float visibleAreaHeight = visibleAreaBottom - visibleAreaTop;

        // Расчет позиций модулей
        Map<Integer, Float> baseColumnY = calculateBasePositions(categoryModules, startModuleY, moduleWidth, columnGap);
        float maxContentHeight = Math.max(baseColumnY.get(0) - startModuleY, baseColumnY.get(1) - startModuleY);
        float maxScroll = Math.max(0, maxContentHeight - visibleAreaHeight);
        modulesScrollOffset = Math.max(0, Math.min(modulesScrollOffset, maxScroll));

        long currentTime = System.currentTimeMillis();
        int column = 0;
        Map<Integer, Float> columnY = new HashMap<>();
        columnY.put(0, startModuleY);
        columnY.put(1, startModuleY);

        for (int i = 0; i < categoryModules.size(); i++) {
            Module module = categoryModules.get(i);
            float moduleX = moduleStartX + 5 + (column * (moduleWidth + columnGap));
            float baseY = columnY.get(column);
            float actualY = baseY - modulesScrollOffset;

            boolean settingsOpen = openSettings.getOrDefault(module, false);
            float moduleTotalHeight = moduleHeight + moduleSpacing;

            // Расчет высоты настроек
            float settingsHeight = 0;
            if (settingsOpen && !module.getSettings().isEmpty()) {
                for (Setting setting : module.getSettings()) {
                    settingsHeight += getEstimatedHeight(setting);
                }
                moduleTotalHeight += settingsHeight + 5;
            }

            // Пропуск если модуль вне видимой области
            if (actualY + moduleTotalHeight < visibleAreaTop || actualY > visibleAreaBottom) {
                columnY.put(column, baseY + moduleTotalHeight);
                column++;
                if (column >= 2) column = 0;
                continue;
            }

            // Отрисовка модуля
            int bgColor = 0xFF2A2A2A;
            RenderUtil.drawRoundedRect(context.getMatrices(), moduleX, actualY, moduleWidth, moduleHeight, new Vector4f(7f, 7f, 7f, 7f), bgColor);

            String displayName;
            int textColor;
            if (waitingForKeybind == module) {
                displayName = "Waiting...";
                textColor = 0xFFFFFF00;
            } else {
                displayName = module.getName();
                textColor = module.isEnabled() ? 0xFF00ff11 : Color.WHITE.getRGB();
            }

            montserratMedium.drawFontLeft(context.getMatrices(), displayName, moduleX + 7, actualY + 9.5f, textColor);

            // Отрисовка тоггл-свитча
            drawToggleSwitch(context, module, moduleX, actualY, moduleWidth, currentTime);

            float nextY = actualY + moduleHeight + moduleSpacing;

            // Отрисовка настроек модуля
            if (settingsOpen && !module.getSettings().isEmpty()) {
                float settingsY = actualY + moduleHeight + moduleSpacing;
                float settingsX = moduleX;
                float settingsWidth = moduleWidth;

                // Расчет максимальной высоты для настроек
                float maxSettingsHeight = visibleAreaBottom - settingsY - 5;
                float totalSettingsHeight = 0;
                for (Setting setting : module.getSettings()) {
                    totalSettingsHeight += getEstimatedHeight(setting);
                }

                // Ограничение высоты и добавление скролла
                boolean needsScroll = totalSettingsHeight > maxSettingsHeight;
                float actualSettingsHeight = needsScroll ? maxSettingsHeight : totalSettingsHeight;

                // Фон настроек
                RenderUtil.drawRoundedRect(context.getMatrices(), settingsX, settingsY, settingsWidth, actualSettingsHeight + 5, new Vector4f(5f, 5f, 5f, 5f), 0xFF1F1F1F);

                // Установка области отсечения для настроек
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100); // Небольшой z-offset

                // Clip область для настроек
                RenderUtil.enableScissor(
                        (int)(settingsX + 1),
                        (int)(settingsY + 1),
                        (int)(settingsWidth - 2),
                        (int)(actualSettingsHeight + 3)
                );

                float settingsScroll = settingsScrollOffsets.getOrDefault(module, 0f);
                float currentSettingY = settingsY + 5 - settingsScroll;
                float settingsBottom = settingsY + actualSettingsHeight + 5;

                for (Setting setting : module.getSettings()) {
                    if (currentSettingY >= settingsBottom) break;

                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer != null) {
                        float settingHeight = getEstimatedHeight(setting);
                        if (currentSettingY + settingHeight < settingsBottom) {
                            currentSettingY = renderer.render(context, setting, settingsX + 5, currentSettingY, settingsWidth - 10, mouseX, mouseY, settingsScroll);
                        } else {
                            currentSettingY += settingHeight;
                        }
                    }
                }

                // Отключение области отсечения
                RenderUtil.disableScissor();
                context.getMatrices().pop();

                // Индикатор скролла (если нужно)
                if (needsScroll) {
                    float scrollbarX = settingsX + settingsWidth - 5;
                    float scrollbarY = settingsY + 5;
                    float scrollbarHeight = actualSettingsHeight - 10;

                    float scrollPercent = settingsScroll / (totalSettingsHeight - maxSettingsHeight);
                    float scrollbarThumbY = scrollbarY + (scrollbarHeight - 10) * scrollPercent;

                    RenderUtil.drawRoundedRect(context.getMatrices(), scrollbarX, scrollbarY, 3, scrollbarHeight, new Vector4f(1.5f, 1.5f, 1.5f, 1.5f), 0xFF444444);
                    RenderUtil.drawRoundedRect(context.getMatrices(), scrollbarX, scrollbarThumbY, 3, 10, new Vector4f(1.5f, 1.5f, 1.5f, 1.5f), 0xFF888888);
                }

                nextY = settingsY + actualSettingsHeight + moduleSpacing;
            }

            columnY.put(column, baseY + (nextY - actualY));
            column++;
            if (column >= 2) column = 0;
        }

        // Сообщение если модулей нет
        if (categoryModules.isEmpty()) {
            String message = !searchText.isEmpty() ? "Ничего не найдено" : "Здесь пока что нету функций";
            float emptyY = startModuleY - modulesScrollOffset + 20;
            montserratMedium.drawFontLeft(context.getMatrices(), message, moduleStartX + 20, emptyY, 0xFF888888);
        }
    }

    private void drawToggleSwitch(DrawContext context, Module module, float moduleX, float actualY, float moduleWidth, long currentTime) {
        boolean isEnabled = module.isEnabled();
        float toggleX = moduleX + moduleWidth - 30;
        float toggleY = actualY + 5;
        float baseWidth = 11f;
        float maxStretch = 22f;
        float toggleWidth = 26f;
        float padding = 2f;

        float centerOff = toggleX + padding + baseWidth / 2f;
        float centerOn = toggleX + toggleWidth - padding - baseWidth / 2f;
        float targetCenter = isEnabled ? centerOn : centerOff;

        ModuleAnimState state = animStates.computeIfAbsent(module, k -> new ModuleAnimState());

        if (state.indicatorX < 0) {
            state.indicatorX = targetCenter;
            state.width = baseWidth;
            state.lastUpdate = currentTime;
        }

        long deltaTime = currentTime - state.lastUpdate;
        if (deltaTime > 100) deltaTime = 16;
        state.lastUpdate = currentTime;

        float deltaFactor = Math.min(deltaTime / 16f, 2f);
        float distance = targetCenter - state.indicatorX;
        float absDistance = Math.abs(distance);

        float lerpSpeed = 0.15f * deltaFactor;
        state.indicatorX += distance * lerpSpeed;
        if (absDistance < 0.05f) {
            state.indicatorX = targetCenter;
        }

        float targetWidth;
        if (absDistance > 0.5f) {
            float movementSpeed = Math.abs(distance * lerpSpeed);
            float stretchFromDistance = Math.min(absDistance / 8f, 1f);
            float stretchFromSpeed = Math.min(movementSpeed * 15f, 1f);
            float stretchFactor = Math.max(stretchFromDistance, stretchFromSpeed);
            targetWidth = baseWidth + (maxStretch - baseWidth) * stretchFactor;
        } else {
            targetWidth = baseWidth;
        }

        float widthLerpSpeed = 0.20f * deltaFactor;
        float widthDiff = targetWidth - state.width;
        state.width += widthDiff * widthLerpSpeed;

        if (Math.abs(state.width - baseWidth) < 0.05f && absDistance < 0.5f) {
            state.width = baseWidth;
        }

        state.width = Math.max(baseWidth, Math.min(state.width, maxStretch));

        float stretch = state.width - baseWidth;
        float drawX;

        if (distance > 0.3f) {
            drawX = state.indicatorX - state.width / 2f - stretch * 0.3f;
        } else if (distance < -0.3f) {
            drawX = state.indicatorX - state.width / 2f + stretch * 0.3f;
        } else {
            drawX = state.indicatorX - state.width / 2f;
        }

        float minX = toggleX + padding;
        float maxX = toggleX + toggleWidth - state.width - padding;
        drawX = Math.max(minX, Math.min(drawX, maxX));

        if (isEnabled) {
            RenderUtil.drawRoundedRectGradientAnimated(context.getMatrices(), drawX + 1, toggleY + 1, state.width, 12, new Vector4f(5f, 5f, 5f, 5f), 0xFFfc03a9, 0xFFff910e, 0xFFfc03a9, 0xFFff910e, 3000);
        } else {
            int grayColor = 0xFF808080;
            RenderUtil.drawRoundedRect(context.getMatrices(), drawX - 1, toggleY + 1, state.width, 12, new Vector4f(5f, 5f, 5f, 5f), grayColor);
        }

        RenderUtil.drawBorder(context.getMatrices(), toggleX, toggleY, 26, 14, new Vector4f(6, 6, 6, 6), -1, 0.1f, 1, 1, false);
    }

    private Map<Integer, Float> calculateBasePositions(List<Module> modules, float startY, float moduleWidth, float columnGap) {
        Map<Integer, Float> columnY = new HashMap<>();
        columnY.put(0, startY);
        columnY.put(1, startY);
        int column = 0;

        for (Module module : modules) {
            float baseY = columnY.get(column);
            float moduleTotalHeight = moduleHeight + moduleSpacing;

            boolean settingsOpen = openSettings.getOrDefault(module, false);
            if (settingsOpen && !module.getSettings().isEmpty()) {
                float settingsHeight = 0;
                for (Setting setting : module.getSettings()) {
                    settingsHeight += getEstimatedHeight(setting);
                }
                moduleTotalHeight += settingsHeight + 5;
            }

            columnY.put(column, baseY + moduleTotalHeight);
            column++;
            if (column >= 2) column = 0;
        }

        return columnY;
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

    private void searchPanel(DrawContext context, int mouseX, int mouseY) {
        FontDraw montserratMedium = FontHelper.monsterrat[16];

        float maxWidth = 75 - 10;
        String displayText = searchText;

        while (montserratMedium.getWidth(displayText) > maxWidth && displayText.length() > 0) {
            displayText = displayText.substring(1);
        }

        int textColor;
        if (searchActive) {
            textColor = Color.white.getRGB();
        } else {
            textColor = searchText.isEmpty() ? new Color(145, 145, 145, 50).getRGB() : new Color(200, 200, 200).getRGB();
        }

        if (displayText.isEmpty() && !searchActive) {
            montserratMedium.drawFontLeft(context.getMatrices(), "Search...", panelX + 305, panelY + 15.5f, textColor);
        } else {
            montserratMedium.drawFontLeft(context.getMatrices(), displayText, panelX + 305, panelY + 15.5f, textColor);
        }

        if (searchActive) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCursorBlink > 530) {
                cursorVisible = !cursorVisible;
                lastCursorBlink = currentTime;
            }

            if (cursorVisible) {
                float textWidth = displayText.isEmpty() ? 0 : montserratMedium.getWidth(displayText);
                montserratMedium.drawFontLeft(context.getMatrices(), "|", panelX + 305 + textWidth, panelY + 15.5f, Color.white.getRGB());
            }
        }
    }

    private float getEstimatedHeight(Setting setting) {
        SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
        if (renderer instanceof ModeListSettingRenderer) {
            return ((ModeListSettingRenderer) renderer).getEstimatedHeight(setting);
        }
        if (renderer instanceof ModeSettingRenderer) {
            return ((ModeSettingRenderer) renderer).getEstimatedHeight(setting);
        }
        return SettingRenderer.SETTING_HEIGHT + SettingRenderer.SETTING_PADDING;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка нажатия на KeySetting
        for (Map.Entry<Module, Boolean> entry : openSettings.entrySet()) {
            if (entry.getValue() && !entry.getKey().getSettings().isEmpty()) {
                Module module = entry.getKey();
                float moduleStartX = panelX + 125;
                float startModuleY = panelY + moduleAreaTop;
                float moduleWidth = (panelWidth - 165) / 2;

                // Найти позицию модуля
                List<Module> modules = getCategoryModules();
                int column = 0;
                Map<Integer, Float> columnY = new HashMap<>();
                columnY.put(0, startModuleY);
                columnY.put(1, startModuleY);

                for (Module m : modules) {
                    float moduleX = moduleStartX + 5 + (column * (moduleWidth + 10));
                    float baseY = columnY.get(column);
                    float actualY = baseY - modulesScrollOffset;

                    if (m == module) {
                        float settingsY = actualY + moduleHeight + moduleSpacing;
                        float settingsX = moduleX;
                        float settingsWidth = moduleWidth;

                        float settingsScroll = settingsScrollOffsets.getOrDefault(module, 0f);
                        float currentSettingY = settingsY + 5 - settingsScroll;

                        for (Setting setting : module.getSettings()) {
                            SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                            if (renderer instanceof KeySettingRenderer) {
                                KeySettingRenderer keyRenderer = (KeySettingRenderer) renderer;
                                if (keyRenderer.getWaitingKey() != null) {
                                    if (keyRenderer.mouseClicked(setting, mouseX, mouseY, button, settingsX + 5, currentSettingY, settingsWidth - 10, settingsScroll)) {
                                        return true;
                                    }
                                }
                            }
                            currentSettingY += getEstimatedHeight(setting);
                        }
                        break;
                    }

                    column++;
                    if (column >= 2) column = 0;
                }
            }
        }

        // Обработка клика по кнопке поиска
        if (button == 0 && isMouseOver((int) mouseX, (int) mouseY, panelX + 300, panelY + 9.5f, 75, 17.5f)) {
            searchActive = true;
            cursorVisible = true;
            lastCursorBlink = System.currentTimeMillis();
            return true;
        } else {
            searchActive = false;
        }

        // Обработка клика по категориям
        float currentY = panelY + 40;
        float categoryX = panelX + 10;
        for (Category category : Category.values()) {
            if (isMouseOver((int) mouseX, (int) mouseY, categoryX, currentY, 100, categoryHeight)) {
                selectedCategory = category;
                openSettings.clear();
                settingsScrollOffsets.clear();
                return true;
            }
            currentY += categoryHeight + 5;
        }

        // Обработка кликов по модулям и их настройкам
        List<Module> categoryModules = getCategoryModules();
        float moduleStartX = panelX + 125;
        float startModuleY = panelY + moduleAreaTop;
        float totalModuleWidth = panelWidth - 150;
        float moduleWidth = (totalModuleWidth - 15) / 2;
        float columnGap = 10;
        float visibleAreaTop = panelY + moduleAreaTop;
        float visibleAreaBottom = panelY + panelHeight - moduleAreaBottom;

        Map<Integer, Float> columnY = new HashMap<>();
        columnY.put(0, startModuleY);
        columnY.put(1, startModuleY);
        int column = 0;

        for (Module module : categoryModules) {
            float moduleX = moduleStartX + 5 + (column * (moduleWidth + columnGap));
            float baseY = columnY.get(column);
            float actualY = baseY - modulesScrollOffset;

            boolean settingsOpen = openSettings.getOrDefault(module, false);
            float moduleTotalHeight = moduleHeight + moduleSpacing;
            if (settingsOpen && !module.getSettings().isEmpty()) {
                float settingsHeight = 0;
                for (Setting setting : module.getSettings()) {
                    settingsHeight += getEstimatedHeight(setting);
                }
                moduleTotalHeight += settingsHeight + 5;
            }

            // Проверка видимости модуля
            if (actualY + moduleHeight >= visibleAreaTop && actualY <= visibleAreaBottom) {
                // ПКМ по модулю - открыть/закрыть настройки
                if (button == 1 && isMouseOver((int) mouseX, (int) mouseY, moduleX, actualY, moduleWidth, moduleHeight)) {
                    if (!module.getSettings().isEmpty()) {
                        boolean currentlyOpen = openSettings.getOrDefault(module, false);
                        openSettings.put(module, !currentlyOpen);
                        if (!currentlyOpen) {
                            settingsScrollOffsets.put(module, 0f);
                        }
                        return true;
                    }
                }

                // Средняя кнопка мыши - изменить кейбинд
                if (button == 2 && isMouseOver((int) mouseX, (int) mouseY, moduleX, actualY, moduleWidth, moduleHeight)) {
                    waitingForKeybind = module;
                    return true;
                }

                // ЛКМ по модулю - включить/выключить
                if (button == 0 && isMouseOver((int) mouseX, (int) mouseY, moduleX, actualY, moduleWidth, moduleHeight)) {
                    module.toggle();
                    return true;
                }
            }

            // Обработка кликов по настройкам модуля
            if (settingsOpen && !module.getSettings().isEmpty()) {
                float settingsY = actualY + moduleHeight + moduleSpacing;
                float settingsX = moduleX;
                float settingsWidth = moduleWidth;

                // Расчет области настроек
                float maxSettingsHeight = visibleAreaBottom - settingsY - 5;
                float totalSettingsHeight = 0;
                for (Setting setting : module.getSettings()) {
                    totalSettingsHeight += getEstimatedHeight(setting);
                }

                boolean needsScroll = totalSettingsHeight > maxSettingsHeight;
                float actualSettingsHeight = needsScroll ? maxSettingsHeight : totalSettingsHeight;

                // Проверка клика внутри области настроек
                if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
                        mouseY >= settingsY && mouseY <= settingsY + actualSettingsHeight + 5) {

                    float settingsScroll = settingsScrollOffsets.getOrDefault(module, 0f);
                    float currentSettingY = settingsY + 5 - settingsScroll;

                    for (Setting setting : module.getSettings()) {
                        if (currentSettingY >= settingsY + actualSettingsHeight + 5) break;

                        SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                        if (renderer != null) {
                            float settingHeight = getEstimatedHeight(setting);
                            if (currentSettingY + settingHeight >= settingsY + 5) {
                                if (renderer.mouseClicked(setting, mouseX, mouseY, button, settingsX + 5, currentSettingY, settingsWidth - 10, settingsScroll)) {
                                    return true;
                                }
                            }
                            currentSettingY += settingHeight;
                        }
                    }
                }
            }

            columnY.put(column, baseY + moduleTotalHeight);
            column++;
            if (column >= 2) column = 0;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Обработка KeySetting
        for (Map.Entry<Module, Boolean> entry : openSettings.entrySet()) {
            if (entry.getValue() && !entry.getKey().getSettings().isEmpty()) {
                for (Setting setting : entry.getKey().getSettings()) {
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
        }

        // Обработка установки кейбинда модуля
        if (waitingForKeybind != null) {
            if (keyCode == 256) { // ESC
                waitingForKeybind.setKey(-1);
            } else {
                waitingForKeybind.setKey(keyCode);
            }
            waitingForKeybind = null;
            return true;
        }

        // Обработка поиска
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float moduleStartX = panelX + 125;
        float moduleAreaY = panelY + moduleAreaTop;
        float moduleAreaHeight = panelHeight - moduleAreaTop - moduleAreaBottom;

        // Скролл основной области модулей
        if (mouseX >= moduleStartX && mouseX <= moduleStartX + (panelWidth - 150) &&
                mouseY >= moduleAreaY && mouseY <= moduleAreaY + moduleAreaHeight) {

            // Проверка, не над ли настройками какого-либо модуля
            List<Module> modules = getCategoryModules();
            float moduleWidth = (panelWidth - 165) / 2;
            int column = 0;
            Map<Integer, Float> columnY = new HashMap<>();
            columnY.put(0, panelY + moduleAreaTop);
            columnY.put(1, panelY + moduleAreaTop);

            for (Module module : modules) {
                float moduleX = moduleStartX + 5 + (column * (moduleWidth + 10));
                float baseY = columnY.get(column);
                float actualY = baseY - modulesScrollOffset;

                boolean settingsOpen = openSettings.getOrDefault(module, false);
                if (settingsOpen && !module.getSettings().isEmpty()) {
                    float settingsY = actualY + moduleHeight + moduleSpacing;
                    float settingsWidth = moduleWidth;

                    // Расчет высоты настроек
                    float maxSettingsHeight = (panelY + panelHeight - moduleAreaBottom) - settingsY - 5;
                    float totalSettingsHeight = 0;
                    for (Setting setting : module.getSettings()) {
                        totalSettingsHeight += getEstimatedHeight(setting);
                    }

                    if (totalSettingsHeight > maxSettingsHeight) {
                        float actualSettingsHeight = maxSettingsHeight;

                        // Проверка, находится ли мышь над настройками этого модуля
                        if (mouseX >= moduleX && mouseX <= moduleX + settingsWidth &&
                                mouseY >= settingsY && mouseY <= settingsY + actualSettingsHeight + 5) {

                            float scroll = settingsScrollOffsets.getOrDefault(module, 0f);
                            float maxScroll = totalSettingsHeight - maxSettingsHeight;
                            scroll -= verticalAmount * 10;
                            scroll = Math.max(0, Math.min(scroll, maxScroll));
                            settingsScrollOffsets.put(module, scroll);
                            return true;
                        }
                    }
                }

                column++;
                if (column >= 2) column = 0;
            }

            // Если не над настройками, скроллим основную область
            modulesScrollOffset -= verticalAmount * 10;
            modulesScrollOffset = Math.max(0, modulesScrollOffset);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            List<Module> categoryModules = getCategoryModules();
            float moduleStartX = panelX + 125;
            float startModuleY = panelY + moduleAreaTop;
            float moduleWidth = (panelWidth - 165) / 2;

            for (Module module : categoryModules) {
                boolean settingsOpen = openSettings.getOrDefault(module, false);
                if (settingsOpen && !module.getSettings().isEmpty()) {
                    // Найти позицию модуля
                    int column = 0;
                    Map<Integer, Float> columnY = new HashMap<>();
                    columnY.put(0, startModuleY);
                    columnY.put(1, startModuleY);
                    boolean found = false;
                    float moduleX = 0, actualY = 0;

                    for (Module m : categoryModules) {
                        moduleX = moduleStartX + 5 + (column * (moduleWidth + 10));
                        float baseY = columnY.get(column);
                        actualY = baseY - modulesScrollOffset;

                        if (m == module) {
                            found = true;
                            break;
                        }

                        column++;
                        if (column >= 2) column = 0;
                    }

                    if (found) {
                        float settingsY = actualY + moduleHeight + moduleSpacing;
                        float settingsX = moduleX;
                        float settingsWidth = moduleWidth;

                        // Проверка, находится ли мышь над настройками
                        float maxSettingsHeight = (panelY + panelHeight - moduleAreaBottom) - settingsY - 5;
                        float totalSettingsHeight = 0;
                        for (Setting setting : module.getSettings()) {
                            totalSettingsHeight += getEstimatedHeight(setting);
                        }

                        if (totalSettingsHeight > maxSettingsHeight &&
                                mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
                                mouseY >= settingsY && mouseY <= settingsY + maxSettingsHeight + 5) {

                            // Обработка SliderSetting
                            float settingsScroll = settingsScrollOffsets.getOrDefault(module, 0f);
                            float currentSettingY = settingsY + 5 - settingsScroll;

                            for (Setting setting : module.getSettings()) {
                                SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                                if (renderer instanceof SliderSettingRenderer) {
                                    float settingHeight = getEstimatedHeight(setting);
                                    if (currentSettingY + settingHeight >= settingsY + 5) {
                                        if (((SliderSettingRenderer) renderer).mouseDragged(setting, (int)mouseX, (int)mouseY, button, settingsX + 5, settingsWidth - 10)) {
                                            return true;
                                        }
                                    }
                                }
                                currentSettingY += getEstimatedHeight(setting);
                            }
                        }
                    }
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Отпускание SliderSetting
        for (Module module : openSettings.keySet()) {
            if (openSettings.get(module)) {
                for (Setting setting : module.getSettings()) {
                    SettingRenderer renderer = SettingRendererManager.getRenderer(setting);
                    if (renderer instanceof SliderSettingRenderer) {
                        ((SliderSettingRenderer) renderer).mouseReleased();
                    }
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}