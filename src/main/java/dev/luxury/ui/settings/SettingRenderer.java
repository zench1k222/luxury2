package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.Setting;
import net.minecraft.client.gui.DrawContext;

public abstract class SettingRenderer {
    public static final float SETTING_HEIGHT = 20f;
    public static final float SETTING_PADDING = 5f;

    public abstract float render(DrawContext context, Setting setting, float x, float y, float width, int mouseX, int mouseY, float scrollOffset);

    public abstract boolean mouseClicked(Setting setting, double mouseX, double mouseY, int button, float x, float y, float width, float scrollOffset);

    public abstract boolean mouseScrolled(Setting setting, double mouseX, double mouseY, double amount, float x, float y, float width, float scrollOffset);

    protected boolean isMouseOver(double mouseX, double mouseY, float x, float y, float width, float height, float scrollOffset) {
        float actualY = y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    protected boolean isVisible(float y, float height, float scrollOffset, float visibleAreaTop, float visibleAreaBottom) {
        float actualY = y - scrollOffset;
        return actualY + height >= visibleAreaTop && actualY <= visibleAreaBottom;
    }
}