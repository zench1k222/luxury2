package dev.luxury.modules.impl.killaura.rotate.mods;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.modules.impl.killaura.rotate.DeltaRotate;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.mods.api.RotationMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class SlothAIMode extends RotationMode {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final SecureRandom secureRandom = new SecureRandom();
    private long lastIdleTime = 0;

    private long disableStartTime = 0;
    private boolean isDisabling = false;
    private Rotate lastTargetRotation = null;
    private static final long TRACKING_DURATION = 1000;

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        boolean isEnabled = killAura != null && killAura.isEnabled();

        if (!isEnabled && !isDisabling) {
            isDisabling = true;
            disableStartTime = System.currentTimeMillis();
            lastTargetRotation = target;
        }

        if (isEnabled && isDisabling) {
            isDisabling = false;
            lastTargetRotation = null;
        }

        if (isDisabling) {
            long timeSinceDisable = System.currentTimeMillis() - disableStartTime;

            if (timeSinceDisable < TRACKING_DURATION) {
                return processTrackingPhase(current, lastTargetRotation != null ? lastTargetRotation : target);
            } else {
                return processSmoothReturn(current);
            }
        }

        DeltaRotate delta = current.rotationDeltaTo(target);
        float yawDelta = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        LivingEntity entity = getTargetEntity();
        float distanceToTarget = entity != null ? mc.player.distanceTo(entity) : Float.MAX_VALUE;

        boolean canAttack = entity != null && distanceToTarget <= getAttackDistance();
        float baseSpeed = canAttack ? 0.87f : 0.56f;

        if (distanceToTarget > 0 && distanceToTarget < 0.66f) {
            float closeRangeSpeed = MathHelper.clamp(distanceToTarget / 1.5f * 0.35f, 0.1f, 0.6f);
            baseSpeed = canAttack ? 0.85f : Math.min(baseSpeed, closeRangeSpeed);
        }

        boolean shouldIdle = entity == null && (System.currentTimeMillis() - lastIdleTime > 1000);
        if (shouldIdle || (!isEnabled && System.currentTimeMillis() - lastIdleTime > 1000)) {
            baseSpeed = 0.35f;
        }

        if (entity != null) {
            lastIdleTime = System.currentTimeMillis();
        }

        float jitterYaw = 0;
        float jitterPitch = 0;

        if (!shouldIdle && entity != null && canAttack) {
            jitterYaw = (float) (randomLerp(18, 27) * Math.sin(System.currentTimeMillis() / 50.0));
            jitterPitch = (float) (randomLerp(15, 22) * Math.sin(System.currentTimeMillis() / 13.0));
        }

        float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 180) : 0;
        float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 0;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

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

    private Rotate processTrackingPhase(Rotate current, Rotate target) {
        LivingEntity entity = getTargetEntity();
        if (entity != null) {
            Vec3d eyes = mc.player.getPos().add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
            Vec3d targetPos = entity.getBoundingBox().getCenter();
            float targetYaw = (float) Math.toDegrees(Math.atan2(targetPos.z - eyes.z, targetPos.x - eyes.x)) - 90.0f;
            float targetPitch = (float) -Math.toDegrees(Math.atan2(targetPos.y - eyes.y,
                    Math.sqrt((targetPos.x - eyes.x) * (targetPos.x - eyes.x) +
                            (targetPos.z - eyes.z) * (targetPos.z - eyes.z))));
            target = new Rotate(targetYaw, targetPitch);
            lastTargetRotation = target;
        }

        DeltaRotate delta = current.rotationDeltaTo(target);
        float yawDelta = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        float baseSpeed = 0.56f;

        float jitterYaw = (float) (randomLerp(18, 27) * Math.sin(System.currentTimeMillis() / 50.0));
        float jitterPitch = (float) (randomLerp(15, 22) * Math.sin(System.currentTimeMillis() / 13.0));

        float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 180) : 0;
        float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 0;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

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

    private Rotate processSmoothReturn(Rotate current) {
        Rotate playerRotation = new Rotate(mc.player.getYaw(), mc.player.getPitch());
        DeltaRotate delta = current.rotationDeltaTo(playerRotation);

        float yawDelta = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();

        if (Math.abs(yawDelta) < 1.0f && Math.abs(pitchDelta) < 1.0f) {
            isDisabling = false;
            lastTargetRotation = null;
            return playerRotation;
        }

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        float returnSpeed = 0.56f;

        float jitterYaw = (float) (randomLerp(10, 18) * Math.sin(System.currentTimeMillis() / 70.0));
        float jitterPitch = (float) (randomLerp(8, 15) * Math.sin(System.currentTimeMillis() / 60.0));

        float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 180) : 0;
        float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 0;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float finalYaw = MathHelper.lerp(returnSpeed, current.getYaw(), current.getYaw() + moveYaw) + jitterYaw;
        float finalPitch = MathHelper.lerp(returnSpeed, current.getPitch(), current.getPitch() + movePitch) + jitterPitch;

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
        return MathHelper.lerp(secureRandom.nextFloat(), min, max);
    }

    public Vec3d randomValue() {
        return new Vec3d(0.01, 0.07, 0.02);
    }

    private LivingEntity getTargetEntity() {
        try {
            KillAura killAura = getKillAura();
            return killAura != null ? killAura.getTarget() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private float getAttackDistance() {
        try {
            KillAura killAura = getKillAura();
            return killAura != null ? killAura.distance.getFloatValue() : 3.0f;
        } catch (Exception e) {
            return 3.0f;
        }
    }

    private KillAura getKillAura() {
        try {
            return (KillAura) Luxury.getInstance().getModuleManager().getModules().stream()
                    .filter(m -> m instanceof KillAura)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}