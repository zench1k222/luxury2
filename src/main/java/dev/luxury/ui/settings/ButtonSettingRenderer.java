package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.ButtonSetting;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class ButtonSettingRenderer extends SettingRenderer {
    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof ButtonSetting)) return y;

        ButtonSetting buttonSetting = (ButtonSetting) setting;
        float actualY = y - scrollOffset;
        float buttonWidth = buttonSetting.getWidth();
        float buttonHeight = buttonSetting.getHeight();

        if (buttonWidth <= 0) buttonWidth = width - 10;
        if (buttonHeight <= 0) buttonHeight = SETTING_HEIGHT;

        float buttonX = x + (width - buttonWidth) / 2;

        if (actualY + buttonHeight < 0 || actualY > context.getScaledWindowHeight()) {
            return y + buttonHeight + SETTING_PADDING;
        }

        boolean isHovered = isMouseOver(mouseX, mouseY, buttonX, y, buttonWidth, buttonHeight, scrollOffset);

        if (isHovered) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.2 + 0.8);
            int pulseAlpha = (int)(255 * pulse);
            int pulseColor = new Color(115, 115, 120, pulseAlpha).getRGB();

            RenderUtil.drawBlur(context.getMatrices(),
                    buttonX, actualY, buttonWidth, buttonHeight,
                    new Vector4f(3, 3, 3, 3),
                    18f,
                    pulseColor
            );

            RenderUtil.drawBlur(context.getMatrices(),
                    buttonX, actualY + buttonHeight - 2, buttonWidth, 0.9f,
                    new Vector4f(0f, 0f, 0f, 0f),
                    18f,
                    new Color(60, 60, 70).getRGB()
            );
        } else {
            RenderUtil.drawBlur(context.getMatrices(),
                    buttonX, actualY, buttonWidth, buttonHeight,
                    new Vector4f(8, 8, 8, 8),
                    18f,
                    new Color(115, 115, 120, 255).getRGB()
            );

            RenderUtil.drawBlur(context.getMatrices(),
                    buttonX, actualY + buttonHeight - 2, buttonWidth, 0.9f,
                    new Vector4f(0f, 0f, 0f, 0f),
                    18f,
                    new Color(60, 60, 70).getRGB()
            );
        }

        RenderUtil.drawBorder(context.getMatrices(),
                buttonX, actualY, buttonWidth, buttonHeight,
                new Vector4f(6f, 6f, 6f, 6f),
                isHovered ? new Color(100, 150, 255, 100).getRGB() : new Color(60, 60, 70, 100).getRGB(),
                1.5f,
                1.0f,
                1.0f,
                false
        );

        FontDraw montserratMaloy = FontHelper.monsterrat[14];
        float textWidth = montserratMaloy.getWidth(buttonSetting.getName());
        float textX = buttonX + (buttonWidth - textWidth) / 2;
        float textY = actualY + (buttonHeight - 8) / 2;

        Color textColor = isHovered ? new Color(200, 220, 255) : Color.WHITE;
        montserratMaloy.drawFontLeft(context.getMatrices(), buttonSetting.getName(), textX, textY, textColor.getRGB());

        return y + buttonHeight + SETTING_PADDING;
    }

    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof ButtonSetting)) return false;
        if (button != 0) return false;

        ButtonSetting buttonSetting = (ButtonSetting) setting;
        float buttonWidth = buttonSetting.getWidth();
        float buttonHeight = buttonSetting.getHeight();

        if (buttonWidth <= 0) buttonWidth = width - 10;
        if (buttonHeight <= 0) buttonHeight = SETTING_HEIGHT;

        float buttonX = x + (width - buttonWidth) / 2;

        if (isMouseOver(mouseX, mouseY, buttonX, y, buttonWidth, buttonHeight, scrollOffset)) {
            buttonSetting.execute();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }
}