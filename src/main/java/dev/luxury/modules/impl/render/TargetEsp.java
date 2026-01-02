package dev.luxury.modules.impl.render;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.impl.combat.KillAura;
import dev.luxury.modules.impl.other.targetesp.TargetESPHandler;

@ModuleAnnotation(
        name = "TargetEsp",
        desc = "",
        category = Category.Render
)
public class TargetEsp extends Module {
    private final ModeSetting type = new ModeSetting("Тип", "Маркер", new String[]{ "Маркер","Призраки","Круг", "Кристаллы"});
    public TargetEsp() {
        addSettings(type);
    }
    @EventTarget
    public void onRender(EventRender3D event) {
        if (ModuleManager.getModule(KillAura.class).hasTarget()) {
            TargetESPHandler.renderESP(type.get(), event);
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
