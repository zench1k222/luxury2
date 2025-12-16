package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.KeySetting;
import dev.luxury.modules.api.settings.Setting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class KeySettingRenderer extends SettingRenderer {
    private Setting waitingForKey = null;

    @Override
    public float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset) {
        if (!(setting instanceof KeySetting)) return y;

        KeySetting keySetting = (KeySetting) setting;
        float actualY = y - scrollOffset;

        if (actualY + SETTING_HEIGHT < 0 || actualY > context.getScaledWindowHeight()) {
            return y + SETTING_HEIGHT + SETTING_PADDING;
        }

        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset);
        boolean isWaiting = waitingForKey == setting;
        int key = keySetting.getValue();
        boolean isMouse = keySetting.isMouse();

        Color bgColor;
        if (isWaiting) {
            bgColor = new Color(255, 255, 0, 50);
        } else if (isHovered) {
            bgColor = new Color(40, 40, 45);
        } else {
            bgColor = new Color(35, 35, 40);
        }

        RenderUtil.drawRoundedRect(context.getMatrices(), x, actualY, width, SETTING_HEIGHT,
                new Vector4f(4f, 4f, 4f, 4f), bgColor.getRGB());

        FontDraw montserratMaloy = FontHelper.monsterrat[14];
        String keyName;

        if (isWaiting) {
            keyName = "Waiting...";
        } else if (key == -1) {
            keyName = "None";
        } else if (isMouse) {
            keyName = "M" + (key + 1);
        } else {
            String glfwName = GLFW.glfwGetKeyName(key, 0);
            keyName = glfwName != null ? glfwName : "Key " + key;
        }

        Color textColor = isWaiting ? Color.YELLOW : Color.WHITE;
        montserratMaloy.drawFontLeft(context.getMatrices(), keySetting.getName() + ": " + keyName, x + 5, actualY + 6, textColor.getRGB());

        return y + SETTING_HEIGHT + SETTING_PADDING;
    }

    @Override
    public boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset) {
        if (!(setting instanceof KeySetting)) return false;

        KeySetting keySetting = (KeySetting) setting;
        float actualY = y - scrollOffset;

        if (button == 0 && isMouseOver(mouseX, mouseY, x, y, width, SETTING_HEIGHT, scrollOffset)) {
            waitingForKey = setting;
            return true;
        }

        if (waitingForKey == setting && button != 0) {
            keySetting.setValue(button);
            keySetting.setMouse(true);
            waitingForKey = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset) {
        return false;
    }

    public boolean keyPressed(Setting setting, int keyCode) {
        if (!(setting instanceof KeySetting)) return false;
        if (waitingForKey != setting) return false;

        KeySetting keySetting = (KeySetting) setting;

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            keySetting.setValue(-1);
            keySetting.setMouse(false);
        } else {
            keySetting.setValue(keyCode);
            keySetting.setMouse(false);
        }

        waitingForKey = null;
        return true;
    }

    public Setting getWaitingKey() {
        return waitingForKey;
    }
}