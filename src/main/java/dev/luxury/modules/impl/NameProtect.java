package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.TextFactoryEvent;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.managers.FriendManager;

@ModuleAnnotation(
        name = "NameProtect",
        desc = "Защищает имена игроков",
        category = Category.Render
)
public class NameProtect extends Module {

    BooleanSetting friendsSetting = new BooleanSetting("Друзья", true);

    public static NameProtect instance;

    public NameProtect() {
        addSettings(friendsSetting);
        instance = this;
    }

    public String getProtectedName(String originalName) {
        if (!isEnabled()) {
            return originalName;
        }

        if (mc != null && mc.getSession() != null && originalName.equals(mc.getSession().getUsername())) {
            return "LuxuryFreeBoost";
        }

        if (friendsSetting.isValue() && FriendManager.getInstance().isFriend(originalName)) {
            return "LuxuryFriend";
        }

        return originalName;
    }

    public boolean isNameProtected(String name) {
        if (!isEnabled()) {
            return false;
        }

        if (mc != null && mc.getSession() != null && name.equals(mc.getSession().getUsername())) {
            return true;
        }

        return friendsSetting.isValue() && FriendManager.getInstance().isFriend(name);
    }

    @EventTarget
    public void onTextFactory(TextFactoryEvent e) {
        e.replaceText(mc.getSession().getUsername(), "LuxuryFreeBoost");
        if (friendsSetting.isValue()) {
            FriendManager.getInstance().getFriends().forEach(friend -> e.replaceText(friend, "LuxuryFriend"));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}