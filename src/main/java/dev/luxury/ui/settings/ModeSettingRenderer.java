package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class ModeSettingRenderer extends SettingRenderer {
    private static final float MODE_ITEM_HEIGHT = 18f;

    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof ModeSetting)) return y;

        ModeSetting modeSetting = (ModeSetting) setting;
        float currentY = y;
        float actualY = currentY - scrollOffset;

        if (actualY + SETTING_HEIGHT >= 0 && actualY <= context.getScaledWindowHeight()) {
            Color bgColor = new Color(35, 35, 40);
            RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT,
                    new Vector4f(4f, 4f, 4f, 4f), bgColor.getRGB());

            FontDraw montserratMaloy = FontHelper.monsterrat[14];
            montserratMaloy.drawFontLeft(context.getMatrices(), modeSetting.getName(), x + 5, actualY + 6, Color.WHITE.getRGB());
        }

        currentY += SETTING_HEIGHT + 2;

        String currentValue = modeSetting.get();
        for (String mode : modeSetting.getModes()) {
            actualY = currentY - scrollOffset;

            if (actualY + MODE_ITEM_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
                currentY += MODE_ITEM_HEIGHT + 2;
                continue;
            }

            boolean isSelected = mode.equals(currentValue);
            boolean isHovered = isMouseOver(mouseX, mouseY, x + 10, currentY, width - 10, MODE_ITEM_HEIGHT, scrollOffset);

            Color bgColor;
            if (isSelected) {
                bgColor = new Color(45, 125, 255);
            } else if (isHovered) {
                bgColor = new Color(40, 40, 45);
            } else {
                bgColor = new Color(30, 30, 35);
            }

            RenderUtil.drawRoundedRect(context.getMatrices(), x + 10, actualY, width - 10, MODE_ITEM_HEIGHT,
                    new Vector4f(3f, 3f, 3f, 3f), bgColor.getRGB());

            float radioX = x + 15;
            float radioY = actualY + 4;
            float radioSize = 10f;

            Color radioColor = isSelected ? new Color(45, 125, 255) : new Color(100, 100, 110);
            RenderUtil.drawRoundedRect(context.getMatrices(), radioX, radioY, radioSize, radioSize,
                    new Vector4f(5f, 5f, 5f, 5f), radioColor.getRGB());

            if (isSelected) {
                RenderUtil.drawRoundedRect(context.getMatrices(), radioX + 2, radioY + 2, radioSize - 4, radioSize - 4,
                        new Vector4f(5f, 5f, 5f, 5f), Color.WHITE.getRGB());
            }

            FontDraw montserratMaloy = FontHelper.monsterrat[14];
            Color textColor = isSelected ? Color.WHITE : new Color(200, 200, 210);
            montserratMaloy.drawFontLeft(context.getMatrices(), mode, x + 30, actualY + 5, textColor.getRGB());

            currentY += MODE_ITEM_HEIGHT + 2;
        }

        return currentY + SETTING_PADDING;
    }

    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof ModeSetting)) return false;
        if (button != 0) return false;

        ModeSetting modeSetting = (ModeSetting) setting;
        float currentY = y + SETTING_HEIGHT + 2;

        for (String mode : modeSetting.getModes()) {
            if (isMouseOver(mouseX, mouseY, x + 10, currentY, width - 10, MODE_ITEM_HEIGHT, scrollOffset)) {
                modeSetting.setValue(mode);
                return true;
            }
            currentY += MODE_ITEM_HEIGHT + 2;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }

    public float getEstimatedHeight(Setting setting) {
        if (!(setting instanceof ModeSetting)) return SETTING_HEIGHT + SETTING_PADDING;

        ModeSetting modeSetting = (ModeSetting) setting;
        float height = SETTING_HEIGHT + 2;
        height += modeSetting.getModes().size() * (MODE_ITEM_HEIGHT + 2);
        return height + SETTING_PADDING;
    }
}