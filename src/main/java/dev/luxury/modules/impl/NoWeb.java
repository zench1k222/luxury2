package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleAnnotation(
        name = "NoWeb",
        desc = "Позволяет двигаться в паутине без замедления",
        category = Category.Movement
)
public class NoWeb extends Module {

    @EventTarget
    public void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (isCollide(Blocks.COBWEB)) {
            Vec3d velocity = mc.player.getVelocity();

            if (mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(velocity.x, 0.9, velocity.z);
            } else if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(velocity.x, -0.9, velocity.z);
            } else {
                mc.player.setVelocity(velocity.x, 0, velocity.z);
            }

            setSpeed(0.21F);
        }
    }


    public static boolean isCollide(net.minecraft.block.Block block) {
        if (mc.player == null || mc.world == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();

        return mc.world.getBlockState(playerPos).isOf(block) || mc.world.getBlockState(playerPos.up()).isOf(block);
    }

    public static void setSpeed(float speed) {
        if (mc.player == null) return;

        float yaw;
        if (KillAura.state && Luxury.getInstance().getRotationManager().getCurrentRotate() != null) {
            yaw = Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw();
        } else {
            yaw = mc.player.getYaw();
        }

        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;

        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }

        double motionX = forward * speed * Math.cos(Math.toRadians(yaw + 90)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90));
        double motionZ = forward * speed * Math.sin(Math.toRadians(yaw + 90)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90));

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
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