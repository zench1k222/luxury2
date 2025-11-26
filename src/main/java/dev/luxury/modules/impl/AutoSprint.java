package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "AutoSprint",
        category = Category.Movement
)
public class AutoSprint extends Module {

    @EventTarget
    public void onTick(EventTick e){
        mc.options.sprintKey.setPressed(true);
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
