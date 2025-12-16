package dev.luxury.modules.impl;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;

@ModuleAnnotation(
        name = "NoPush",
        desc = "Убивает коллизию от разных типов",
        category = Category.Player
)
public class NoPush extends Module {

    public final ModeListSetting mods = new ModeListSetting("Типы",
            new BooleanSetting("Вода", false),
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Блоки", true),
            new BooleanSetting("Удочка", true)
    );

    public NoPush() {
        addSettings(mods);
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