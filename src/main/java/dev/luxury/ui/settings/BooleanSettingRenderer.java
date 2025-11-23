package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class BooleanSettingRenderer extends SettingRenderer {
    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof BooleanSetting)) return y;
        
        BooleanSetting boolSetting = (BooleanSetting) setting;
        float actualY = y - scrollOffset;

        if (actualY + SETTING_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
            return y + SETTING_HEIGHT + SETTING_PADDING;
        }
        
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset);
        boolean value = boolSetting.get();

        int bgColor = isHovered ? 0xFF333333 : 0xFF2A2A2A;
        RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT, new Vector4f(5f, 5f, 5f, 5f), bgColor);

        FontDraw montserratMaloy = FontHelper.monsterrat[14];
        montserratMaloy.drawFontLeft(context.getMatrices(), boolSetting.getName(), x + 5, actualY + 6, Color.WHITE.getRGB());

        float toggleX = x + width - 35;
        float toggleY = actualY + 3;
        float toggleWidth = 30f;
        float toggleHeight = 14f;
        
        int toggleBgColor = value ? 0xFF00ff11 : 0xFF808080;
        RenderUtil.drawRoundedRect(context.getMatrices(), toggleX, toggleY, toggleWidth, toggleHeight, new Vector4f(7f, 7f, 7f, 7f), toggleBgColor);

        float indicatorSize = 10f;
        float indicatorX = value ? toggleX + toggleWidth - indicatorSize - 2 : toggleX + 2;
        float indicatorY = toggleY + 2;
        
        RenderUtil.drawRoundedRect(context.getMatrices(), indicatorX, indicatorY, indicatorSize, indicatorSize, new Vector4f(5f, 5f, 5f, 5f), Color.WHITE.getRGB());
        
        return y + SETTING_HEIGHT + SETTING_PADDING;
    }
    
    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof BooleanSetting)) return false;
        if (button != 0) return false;
        
        BooleanSetting boolSetting = (BooleanSetting) setting;
        float actualY = y - scrollOffset;
        
        if (isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset)) {
            boolSetting.toggle();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }
}

