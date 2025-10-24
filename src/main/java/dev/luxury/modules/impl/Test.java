package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;


@ModuleAnnotation(
        name = "Test",
        key = GLFW.GLFW_KEY_G,
        desc = "",
        category = Category.Render
)

public class Test extends Module {

    public static final Identifier IMAGE = Identifier.tryParse("luxury:images/marker.png");
    @EventTarget
    public void render(EventRender2D e) {

        float x = 50.0f;
        float y = 50.0f;
        float width = 200.0f;
        float height = 100.0f;


    }
    @Override
    public void onEnable() {
        System.out.println("vkl");
        super.onEnable();
    }

   @Override
    public void onDisable() {
        System.out.println("vikl");
        super.onDisable();
    }
}
