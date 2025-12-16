package dev.luxury.ui;

import dev.luxury.Luxury;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickGui",
        desc = "Tabbed GUI with modern design",
        category = Category.Render
)
public class Clickgui extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public Clickgui() {
        this.setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.currentScreen == null) {
            mc.setScreen(new TabbedGUI(Luxury.getInstance().getModuleManager()));
        }
        disable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}