package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.render.TextRenderUtil;

@ModuleAnnotation(
        name = "NameProtect",
        desc = "Защищает имена игроков",
        category = Category.Render
)
public class NameProtect extends Module {

    public static boolean state = false;

    BooleanSetting friendsSetting = new BooleanSetting("Друзья", true);

    public NameProtect() {
        addSettings(friendsSetting);
    }

    @EventTarget
    public void onTextFactory(TextRenderUtil e) {
        String text = e.getText();
        if (text == null || text.isEmpty()) return;

        String myName = mc.getSession().getUsername();

        text = text.replace(myName, "LuxuryFree");

        if (friendsSetting.get() && mc.world != null && mc.player != null) {
            for (var player : mc.world.getPlayers()) {
                String playerName = player.getName().getString();
                if (FriendManager.getInstance().isFriend(playerName)) {
                    text = text.replace(playerName, "LuxuryFriend");
                }
            }
        }

        e.setText(text);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        state = false;
    }
}