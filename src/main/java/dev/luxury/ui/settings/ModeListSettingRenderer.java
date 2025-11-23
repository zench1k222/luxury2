package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class ModeListSettingRenderer extends SettingRenderer {
    private static final float SUB_SETTING_OFFSET = 10f;
    private static final float SUB_SETTING_HEIGHT = 18f;
    
    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof ModeListSetting)) return y;
        
        ModeListSetting modeListSetting = (ModeListSetting) setting;
        float currentY = y;
        float actualY = currentY - scrollOffset;

        if (actualY + SETTING_HEIGHT >= 0 && actualY <= context.getScaledWindowHeight()) {
            int bgColor = 0xFF2A2A2A;
            RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT, new Vector4f(5f, 5f, 5f, 5f), bgColor);

            FontDraw montserratMaloy = FontHelper.monsterrat[14];
            montserratMaloy.drawFontLeft(context.getMatrices(), modeListSetting.getName(), x + 5, actualY + 6, 0xFFFF910E);
        }
        
        currentY += SETTING_HEIGHT + 2;

        for (BooleanSetting subSetting : modeListSetting.getSettings()) {
            actualY = currentY - scrollOffset;

            if (actualY + SUB_SETTING_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
                currentY += SUB_SETTING_HEIGHT + 2;
                continue;
            }
            
            boolean isHovered = isMouseOver(mouseX, mouseY, x + SUB_SETTING_OFFSET, currentY, width - SUB_SETTING_OFFSET, SUB_SETTING_HEIGHT, scrollOffset);
            boolean value = subSetting.get();

            int bgColor = isHovered ? 0xFF333333 : 0xFF252525;
            RenderUtil.drawRoundedRect(context.getMatrices(), x + SUB_SETTING_OFFSET, actualY, width - SUB_SETTING_OFFSET, SUB_SETTING_HEIGHT, new Vector4f(3f, 3f, 3f, 3f), bgColor);

            FontDraw montserratMaloy = FontHelper.monsterrat[14];
            montserratMaloy.drawFontLeft(context.getMatrices(), subSetting.getName(), x + SUB_SETTING_OFFSET + 5, actualY + 5, Color.WHITE.getRGB());

            float checkboxX = x + width - SUB_SETTING_OFFSET - 20;
            float checkboxY = actualY + 2;
            float checkboxSize = 14f;
            
            int checkboxBgColor = value ? 0xFF00ff11 : 0xFF808080;
            RenderUtil.drawRoundedRect(context.getMatrices(), checkboxX, checkboxY, checkboxSize, checkboxSize, new Vector4f(3f, 3f, 3f, 3f), checkboxBgColor);
            
            if (value) {
                montserratMaloy.drawFontLeft(context.getMatrices(), "✓", checkboxX + 3, checkboxY + 2, Color.WHITE.getRGB());
            }
            
            currentY += SUB_SETTING_HEIGHT + 2;
        }
        
        return currentY + SETTING_PADDING;
    }
    
    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof ModeListSetting)) return false;
        if (button != 0) return false;
        
        ModeListSetting modeListSetting = (ModeListSetting) setting;
        float currentY = y + SETTING_HEIGHT + 2;
        
        for (BooleanSetting subSetting : modeListSetting.getSettings()) {
            if (isMouseOver(mouseX, mouseY, x + SUB_SETTING_OFFSET, currentY, width - SUB_SETTING_OFFSET, SUB_SETTING_HEIGHT, scrollOffset)) {
                subSetting.toggle();
                return true;
            }
            currentY += SUB_SETTING_HEIGHT + 2;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }
    
    public float getEstimatedHeight(Setting setting) {
        if (!(setting instanceof ModeListSetting)) return SETTING_HEIGHT + SETTING_PADDING;
        
        ModeListSetting modeListSetting = (ModeListSetting) setting;
        float height = SETTING_HEIGHT + 2; // Заголовок
        height += modeListSetting.getSettings().size() * (SUB_SETTING_HEIGHT + 2); // Поднастройки
        return height + SETTING_PADDING;
    }
}

