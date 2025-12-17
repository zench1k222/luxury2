package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.modules.impl.killaura.Move;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@ModuleAnnotation(
        name = "Speed",
        desc = "AresMine speed",
        category = Category.Movement
)
public class Speed extends Module {

    private final SliderSetting speedFactor = new SliderSetting("Скорость", 8.0f, 1.0f, 15.0f);
    private final SliderSetting distance = new SliderSetting("Дистанция", 3.0f, 0.5f, 5.0f);
    private final SliderSetting distanceElytra = new SliderSetting("Дистанция на элитре", 25.0f, 1.0f, 40.0f);

    private final BooleanSetting predictMovement = new BooleanSetting("Предикт", true);
    private final BooleanSetting smoothMovement = new BooleanSetting("Плавность", true);

    private Entity target;
    private Vec3d lastTargetPos;
    private Vec3d predictedPos;
    private final double[] lastMotion = new double[]{0.0, 0.0};

    private final MinecraftClient mc = MinecraftClient.getInstance();
   public Speed(){
       addSettings(speedFactor,distance,distanceElytra,predictMovement,smoothMovement);
   }
    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        updateTarget();
        handleAresMine();
    }

    @Override
    public void onEnable() {
        target = null;
        lastTargetPos = null;
        predictedPos = null;
        lastMotion[0] = lastMotion[1] = 0.0;
    }

    private void updateTarget() {
        if (KillAura.instance.isEnabled() && KillAura.instance.getTarget() != null) {
            target = KillAura.instance.getTarget();
        } else {
            target = null;
        }

        if (target == null) {
            lastTargetPos = null;
            predictedPos = null;
            return;
        }

        Vec3d currentPos = target.getPos();

        if (lastTargetPos == null) {
            lastTargetPos = currentPos;
            predictedPos = currentPos;
            return;
        }

        Vec3d velocity = currentPos.subtract(lastTargetPos);

        if (predictMovement.get()) {
            predictedPos = currentPos.add(
                    velocity.x * 2.0,
                    velocity.y * 2.0,
                    velocity.z * 2.0
            );
        } else {
            predictedPos = currentPos;
        }

        lastTargetPos = currentPos;
    }

    private void handleAresMine() {
        if (!KillAura.instance.isEnabled() || target == null) return;
        if (!Move.isMoving()) return;

        Vec3d targetPos = predictMovement.get() && predictedPos != null ? predictedPos : target.getPos();

        float activeDistance = distance.getFloatValue();

        if (target instanceof PlayerEntity player && player.isGliding()) {
            activeDistance = distanceElytra.getFloatValue();
        }

        double distSq = mc.player.squaredDistanceTo(targetPos);
        if (distSq > activeDistance * activeDistance) return;

        float slipperiness = mc.world.getBlockState(mc.player.getBlockPos()).getBlock().getSlipperiness();

        float horizontalFriction = mc.player.isOnGround() ? slipperiness * 0.91F : 0.91F;

        float verticalFriction = mc.player.isOnGround() ? slipperiness : 0.99F;

        double speed = speedFactor.getValue() * 0.01 * horizontalFriction * verticalFriction;

        double[] motion = getDirectionToPoint(mc.player.getPos(), targetPos, speed);

        if (smoothMovement.get()) {
            double accel = 0.6;
            motion[0] = lastMotion[0] + (motion[0] - lastMotion[0]) * accel;
            motion[1] = lastMotion[1] + (motion[1] - lastMotion[1]) * accel;
        }

        lastMotion[0] = motion[0];
        lastMotion[1] = motion[1];

        mc.player.setVelocity(motion[0], 0.0, motion[1]);
    }

    private double[] getDirectionToPoint(Vec3d from, Vec3d to, double speed) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length == 0.0) {
            return new double[]{0.0, 0.0};
        }

        return new double[]{
                dx / length * speed,
                dz / length * speed
        };
    }
}