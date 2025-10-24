package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.targetesp.TargetESPHandler;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "TargetEsp",
        desc = "",
        category = Category.Render,
        key = GLFW.GLFW_KEY_K
)
public class TargetEsp extends Module {
    private String espMode = "Marker";

    @EventTarget
    public void onRender(EventRender3D event) {
        if (ModuleManager.getModule(KillAura.class).hasTarget()) {
            TargetESPHandler.renderESP(espMode, event.getMatrices());
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
