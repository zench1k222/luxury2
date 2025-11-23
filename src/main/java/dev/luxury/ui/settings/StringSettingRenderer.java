package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.Setting;
import dev.luxury.modules.api.settings.StringSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

import java.awt.*;

public class StringSettingRenderer extends SettingRenderer {
    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof StringSetting)) return y;
        
        StringSetting stringSetting = (StringSetting) setting;
        float actualY = y - scrollOffset;

        if (actualY + SETTING_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
            return y + SETTING_HEIGHT + SETTING_PADDING;
        }
        
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset);
        String value = stringSetting.getValue();

        int bgColor = isHovered ? 0xFF333333 : 0xFF2A2A2A;
        RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT, new Vector4f(5f, 5f, 5f, 5f), bgColor);

        FontDraw montserratMaloy = FontHelper.monsterrat[14];
        String displayText = stringSetting.getName() + ": " + value;
        montserratMaloy.drawFontLeft(context.getMatrices(), displayText, x + 5, actualY + 6, Color.WHITE.getRGB());
        
        return y + SETTING_HEIGHT + SETTING_PADDING;
    }
    
    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        return false;
    }
    
    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }
}

