package dev.luxury.modules.impl.killaura.rotate.mods;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.modules.impl.killaura.rotate.DeltaRotate;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.mods.api.RotationMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class SlothAIMode extends RotationMode {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();
    private long lastIdleTime = 0;

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        DeltaRotate delta = current.rotationDeltaTo(target);
        float yawDelta = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        LivingEntity entity = getTargetEntity();
        float distanceToTarget = entity != null ? mc.player.distanceTo(entity) : Float.MAX_VALUE;

        boolean canAttack = entity != null && distanceToTarget <= getAttackDistance();
        float baseSpeed = canAttack ? 0.93f : 0.56f;

        if (distanceToTarget > 0 && distanceToTarget < 0.66f) {
            float closeRangeSpeed = MathHelper.clamp(distanceToTarget / 1.5f * 0.35f, 0.1f, 0.6f);
            baseSpeed = canAttack ? 0.85f : Math.min(baseSpeed, closeRangeSpeed);
        }

        boolean shouldIdle = entity == null && (System.currentTimeMillis() - lastIdleTime > 1000);
        if (shouldIdle) {
            baseSpeed = 0.35f;
        }

        if (entity != null) {
            lastIdleTime = System.currentTimeMillis();
        }

        float jitterYaw = 0;
        float jitterPitch = 0;

        if (!shouldIdle && entity != null) {
            jitterYaw = (float) (randomLerp(20, 26) * Math.sin(System.currentTimeMillis() / 25.0));
            jitterPitch = (float) (randomLerp(8, 23) * Math.sin(System.currentTimeMillis() / 27.0));
        }

        float moveYaw = rotationDifference > 0 ? MathHelper.clamp(yawDelta, -Math.abs(yawDelta / rotationDifference * 180), Math.abs(yawDelta / rotationDifference * 180)) : yawDelta;

        float movePitch = rotationDifference > 0 ? MathHelper.clamp(pitchDelta, -Math.abs(pitchDelta / rotationDifference * 180), Math.abs(pitchDelta / rotationDifference * 180)) : pitchDelta;

        float finalYaw = MathHelper.lerp(baseSpeed, current.getYaw(), current.getYaw() + moveYaw) + jitterYaw;
        float finalPitch = MathHelper.lerp(baseSpeed, current.getPitch(), current.getPitch() + movePitch) + jitterPitch;

        finalPitch = MathHelper.clamp(finalPitch, -90.0f, 90.0f);

        finalYaw = MathHelper.wrapDegrees(finalYaw);

        float gcd = Rotate.gcd();
        if (gcd > 0.0f && gcd < 10.0f && !Float.isInfinite(gcd) && !Float.isNaN(gcd)) {
            finalYaw = (float) (finalYaw - (finalYaw - current.getYaw()) % gcd);
            finalPitch = (float) (finalPitch - (finalPitch - current.getPitch()) % gcd);
        }

        return new Rotate(finalYaw, finalPitch);
    }

    private float randomLerp(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private LivingEntity getTargetEntity() {
        try {
            KillAura killAura = (KillAura) Luxury.getInstance().getModuleManager().getModules().stream().filter(m -> m instanceof KillAura && m.isEnabled()).findFirst().orElse(null);

            return killAura != null ? killAura.getTarget() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private float getAttackDistance() {
        try {
            KillAura killAura = (KillAura) Luxury.getInstance().getModuleManager().getModules().stream().filter(m -> m instanceof KillAura && m.isEnabled()).findFirst().orElse(null);

            return killAura != null ? killAura.distance.getFloatValue() : 3.0f;
        } catch (Exception e) {
            return 3.0f;
        }
    }
}