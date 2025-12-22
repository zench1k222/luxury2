package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.render.TextRenderUtil;

@ModuleAnnotation(
        name = "NameProtect",
        desc = "Защищает имена игроков",
        category = Category.Render
)
public class NameProtect extends Module {
    BooleanSetting friendsSetting = new BooleanSetting("Друзья", false);

    public NameProtect() {
        addSettings(friendsSetting);
    }

    @EventTarget
    public void onTextFactory(TextRenderUtil e) {
        e.replaceText(mc.getSession().getUsername(), "LuxuryFree");
        if (friendsSetting.get()) {
            FriendManager.getInstance().getFriends().forEach(friend ->
                    e.replaceText(friend, "LuxuryFree")
            );
        }
    }
}