package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "Speed",
        desc = "",
        category = Category.Movement
)
public class Speed extends Module {

    float horizontalSpeed = 0.1f;
    float verticalSpeed = 1.5f;
    boolean wasInVehicle = false;
    Entity previousVehicle = null;

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        boolean isInVehicle = mc.player.hasVehicle();
        Entity currentVehicle = mc.player.getVehicle();

        if (wasInVehicle && !isInVehicle && previousVehicle != null) {
            double angle = Math.toRadians(previousVehicle.getYaw());
            double sinAngle = Math.sin(angle);
            double cosAngle = Math.cos(angle);
            previousVehicle.setVelocity(-sinAngle * horizontalSpeed, verticalSpeed, cosAngle * horizontalSpeed);
        }

        wasInVehicle = isInVehicle;
        previousVehicle = currentVehicle;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            wasInVehicle = mc.player.hasVehicle();
            previousVehicle = mc.player.getVehicle();
        }
    }

    @Override
    public void onDisable() {
        wasInVehicle = false;
        previousVehicle = null;
    }
}