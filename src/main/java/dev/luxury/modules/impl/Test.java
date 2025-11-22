package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.render.ColorRGBA;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;


@ModuleAnnotation(
        name = "Test",
        desc = "",
        category = Category.Render
)

public class Test extends Module {


    
    @EventTarget
    public void render(EventRender2D e) {
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

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
