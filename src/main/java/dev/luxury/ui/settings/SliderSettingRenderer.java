package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.Setting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class SliderSettingRenderer extends SettingRenderer {
    private boolean isDragging = false;
    private Setting draggedSetting = null;

    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof SliderSetting)) return y;

        SliderSetting sliderSetting = (SliderSetting) setting;
        float actualY = y - scrollOffset;

        if (actualY + SETTING_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
            return y + SETTING_HEIGHT + SETTING_PADDING;
        }

        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset);
        double value = sliderSetting.getValue();
        double min = sliderSetting.getMin();
        double max = sliderSetting.getMax();

        // Цвета как в TabbedGUI
        Color bgColor = isHovered ? new Color(40, 40, 45) : new Color(35, 35, 40);
        RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT,
                new Vector4f(4f, 4f, 4f, 4f), bgColor.getRGB());

        FontDraw montserratMaloy = FontHelper.monsterrat[14];
        String displayText = sliderSetting.getName() + ": " + String.format("%.1f", value);
        montserratMaloy.drawFontLeft(context.getMatrices(), displayText, x + 5, actualY + 0.8F, Color.WHITE.getRGB());

        float sliderX = x + 5;
        float sliderY = actualY + SETTING_HEIGHT - 8;
        float sliderWidth = width - 10;
        float sliderHeight = 4f;

        RenderUtil.drawRoundedRect(context.getMatrices(), sliderX, sliderY, sliderWidth, sliderHeight,
                new Vector4f(2f, 2f, 2f, 2f), new Color(60, 60, 70).getRGB());

        float fillPercent = (float) ((value - min) / (max - min));
        float fillWidth = sliderWidth * fillPercent;

        RenderUtil.drawRoundedRect(context.getMatrices(), sliderX, sliderY, fillWidth, sliderHeight,
                new Vector4f(2f, 2f, 2f, 2f), new Color(45, 125, 255).getRGB());

        float indicatorX = sliderX + fillWidth - 4;
        float indicatorY = sliderY - 2;
        float indicatorSize = 8f;
        RenderUtil.drawRoundedRect(context.getMatrices(), indicatorX, indicatorY, indicatorSize, indicatorSize,
                new Vector4f(4f, 4f, 4f, 4f), Color.WHITE.getRGB());

        return y + SETTING_HEIGHT + SETTING_PADDING;
    }

    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof SliderSetting)) return false;
        if (button != 0) return false;

        SliderSetting sliderSetting = (SliderSetting) setting;
        float actualY = y - scrollOffset;

        if (isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset)) {
            isDragging = true;
            draggedSetting = setting;
            updateSliderValue(sliderSetting, mouseX, x, width);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof SliderSetting)) return false;

        SliderSetting sliderSetting = (SliderSetting) setting;
        float actualY = y - scrollOffset;

        if (isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset)) {
            double currentValue = sliderSetting.getValue();
            double increment = sliderSetting.getIncrement();
            double newValue = currentValue + (amount > 0 ? increment : -increment);
            sliderSetting.setValue(newValue);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(Setting setting, double mouseX, double mouseY, int button, float x, float width) {
        if (!(setting instanceof SliderSetting)) return false;
        if (button != 0) return false;
        if (!isDragging || draggedSetting != setting) return false;

        SliderSetting sliderSetting = (SliderSetting) setting;
        updateSliderValue(sliderSetting, mouseX, x, width);
        return true;
    }

    public void mouseReleased() {
        isDragging = false;
        draggedSetting = null;
    }

    private void updateSliderValue(SliderSetting sliderSetting, double mouseX, float x, float width) {
        float sliderX = x + 5;
        float sliderWidth = width - 10;

        double percent = Math.max(0, Math.min(1, (mouseX - sliderX) / sliderWidth));
        double newValue = sliderSetting.getMin() + (sliderSetting.getMax() - sliderSetting.getMin()) * percent;
        sliderSetting.setValue(newValue);
    }
}