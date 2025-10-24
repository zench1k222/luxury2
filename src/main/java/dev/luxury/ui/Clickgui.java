package dev.luxury.ui;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickGui",
        desc = "Custom Click GUI",
        key = GLFW.GLFW_KEY_RIGHT_SHIFT,
        category = Category.Render
)
public class Clickgui extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Csgui csgui = new Csgui(new ModuleManager());

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.currentScreen == null) {
            mc.setScreen(csgui);
        }
        disable();
    }

    @Override
    public void onDisable() {
        super.onDisable();

    }
}
