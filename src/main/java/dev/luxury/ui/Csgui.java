package dev.luxury.ui;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.render.ColorUtil;
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

    private float panelX;
    private float panelY;
    private final float panelWidth = 400;
    private final float panelHeight = 260;
    private final float categoryHeight = 25;
    private final float moduleHeight = 25;
    private final Map<Module, Float> indicatorXMap = new HashMap<>();
    private final Map<Module, Float> widthMap = new HashMap<>();
    private Map<Module, Long> lastTimeMap = new HashMap<>();
    private Map<Module, Float> velocityXMap = new HashMap<>();
    private Map<Module, Float> velocityWMap = new HashMap<>();
    private long lastTime = System.currentTimeMillis();
    private float lastPanelWidth = -1;
    private float lastPanelHeight = -1;
    public Csgui(ModuleManager moduleManager) {
        super(Text.literal("Csgui"));
        this.moduleManager = moduleManager;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
    private static class ModuleAnimState {
        float indicatorX = -1; // -1 означает не инициализировано
        float width = 11f;
        long lastUpdate = 0;

        public ModuleAnimState() {}
    }

    private Map<Module, ModuleAnimState> animStates = new HashMap<>();
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;
        RenderUtil.drawBlur(context.getMatrices(), panelX, panelY, 121, panelHeight, new Vector4f(8f, 8f, 0f, 0f), 12,Color.gray.getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(), panelX+120, panelY, 400 - 134, panelHeight, new Vector4f(0f, 0f, 8f, 8f), new Color(9,10,10).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(),panelX,panelY +35,385,2,new Vector4f(0f,0f,0f,0f),new Color(145,145,145,100).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(),panelX+120,panelY+36,2,225,new Vector4f(0f,0f,0f,0f),new Color(145,145,145,50).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(),panelX,panelY +200,121,2,new Vector4f(0f,0f,0f,0f),new Color(145,145,145,100).getRGB());
        RenderUtil.drawRoundedRect(context.getMatrices(),panelX+300,panelY+9.5f,75,17.5f,new Vector4f(8,8,8,8),new Color(145,145,145,50).getRGB());
        FontDraw.Montserrat_Medium.drawString(context.getMatrices(),"Search...",panelX + 305,panelY + 15.3f,new Color(145,145,145,50).getRGB());
        float currentY = panelY + 45;

        float categoryX = panelX + 10;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = isMouseOver(mouseX, mouseY, categoryX, currentY, 100, categoryHeight);

            int bgColor = isSelected ? 0xFF2196F3 : (isHovered ? 0xFF1976D2 : 0xFF1a1a1a);

            RenderUtil.drawRoundedRect(context.getMatrices(),categoryX,currentY,100,categoryHeight,new Vector4f(8f,8f,8f,8f),bgColor);

            FontDraw.Montserrat_Big.drawGradientString(context.getMatrices(),"Luxury",panelX+45,panelY+20,0xFFff910e,0xFFf5cc22,true,2000,0);
            int textColor = isSelected || isHovered ? Color.white.getRGB() : 0xFFaaaaaa;
            FontDraw.Montserrat_Medium.drawString(context.getMatrices(),category.name(),categoryX + 10,currentY + 9,textColor);
            currentY += categoryHeight + 5;
        }

    drawModules(context,mouseX,mouseY);
        super.render(context,mouseX,mouseY,delta);
    }
    private Category lastSelectedCategory = null;

    private void drawModules(DrawContext context, int mouseX, int mouseY) {
        List categoryModules = moduleManager.getSorted().stream().filter(module -> module.getCategory() == selectedCategory).collect(Collectors.toList());

        if (lastSelectedCategory != selectedCategory) {
            animStates.clear();
            lastSelectedCategory = selectedCategory;
        }

        float moduleStartX = panelX + 125;
        float startModuleY = panelY + 20;
        float totalModuleWidth = panelWidth - 150;

        float moduleWidth = (totalModuleWidth - 15) / 2;
        float columnGap = 10;
        if (lastPanelWidth != panelWidth || lastPanelHeight != panelHeight) {
            animStates.clear();
            lastPanelWidth = panelWidth;
            lastPanelHeight = panelHeight;
        }

        float currentModuleY = startModuleY + 30;
        int column = 0;

        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < categoryModules.size(); i++) {
            Module module = (Module) categoryModules.get(i);

            if (currentModuleY + moduleHeight > panelY + panelHeight - 10) {
                break;
            }

            float moduleX = moduleStartX + 5 + (column * (moduleWidth + columnGap));

            boolean isHovered = isMouseOver(mouseX, mouseY, moduleX, currentModuleY, moduleWidth, moduleHeight);
            boolean isEnabled = module.isEnabled();

            int bgColor = 0xFF2A2A2A;

            RenderUtil.drawRoundedRect(context.getMatrices(), moduleX, currentModuleY, moduleWidth, moduleHeight, new Vector4f(7f, 7f, 7f, 7f), bgColor);
            int textColor = isEnabled ? 0xFF00ff11 : Color.WHITE.getRGB();

            FontDraw.Montserrat_Medium.drawString(context.getMatrices(), module.getName(), moduleX + 7, currentModuleY + 9.5f, textColor);

            float toggleX = moduleX + moduleWidth - 30;
            float toggleY = currentModuleY + 5;

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
                RenderUtil.drawRoundedRectGradientAnimated(context.getMatrices(), drawX + 1, toggleY + 1, state.width, 12, new Vector4f(5f,5f,5f,5f), 0xFFfc03a9, 0xFFff910e, 0xFFfc03a9, 0xFFff910e, 3000);
            } else {
                int grayColor = 0xFF808080;
                RenderUtil.drawRoundedRect(context.getMatrices(), drawX - 1, toggleY + 1, state.width, 12, new Vector4f(5f,5f,5f,5f), grayColor);
            }

            RenderUtil.drawBorder(context.getMatrices(), toggleX, toggleY, 26, 14, new Vector4f(6,6,6,6), -1, 0.1f, 1, 1, false);

            column++;
            if (column >= 2) {
                column = 0;
                currentModuleY += moduleHeight + 3;
            }

 }




        if (categoryModules.isEmpty()) {
            FontDraw.Montserrat_Medium.drawString(context.getMatrices(), "Здесь пока что нету функций", moduleStartX + 20, currentModuleY + 20, 0xFF888888);
        }
}
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float currentY = panelY + 40;
            float categoryX = panelX + 10;

            for (Category category : Category.values()) {
                if (isMouseOver((int)mouseX, (int)mouseY, categoryX, currentY, 100, categoryHeight)) {
                    selectedCategory = category;
                    return true;
                }
                currentY += categoryHeight + 5;
            }

            List<Module> categoryModules = moduleManager.getSorted().stream().filter(module -> module.getCategory() == selectedCategory).collect(Collectors.toList());

            float moduleStartX = panelX + 125;
            float moduleY = panelY + 20 + 30;
            float totalModuleWidth = panelWidth - 150;
            float moduleWidth = (totalModuleWidth - 15) / 2;
            float columnGap = 10;

            int column = 0;

            for (int i = 0; i < categoryModules.size(); i++) {
                Module module = categoryModules.get(i);

                if (moduleY + moduleHeight > panelY + panelHeight - 10) {
                    break;
                }

                float moduleX = moduleStartX + 5 + (column * (moduleWidth + columnGap));

                if (isMouseOver((int)mouseX, (int)mouseY, moduleX, moduleY, moduleWidth, moduleHeight)) {
                    module.toggle();
                    return true;
                }

                column++;
                if (column >= 2) {
                    column = 0;
                    moduleY += moduleHeight + 3;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    private boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >=y&& mouseY <=y +height;
    }

}


