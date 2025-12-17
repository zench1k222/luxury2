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

import java.util.Random;

public class SpookytimeMode extends RotationMode {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private LivingEntity lastTarget = null;

    private long returnStartTime = 0;
    private boolean isReturning = false;
    private Rotate lastTargetRotation = null;
    private static final long TRACKING_DURATION = 1000;

    private boolean hadTarget = false;
    private long lastTargetTime = 0;

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        boolean isEnabled = killAura != null && killAura.isEnabled();
        LivingEntity entity = getTargetEntity();
        boolean hasTarget = entity != null;

        if (hasTarget) {
            lastTargetTime = System.currentTimeMillis();
        }

        boolean shouldStartReturning = !isEnabled || (!hasTarget && hadTarget);

        if (shouldStartReturning && !isReturning) {
            isReturning = true;
            returnStartTime = System.currentTimeMillis();
            lastTargetRotation = current;
        }

        if (isEnabled && hasTarget && isReturning) {
            isReturning = false;
            lastTargetRotation = null;
        }

        hadTarget = hasTarget;

        if (isReturning) {
            long timeSinceReturn = System.currentTimeMillis() - returnStartTime;

            if (timeSinceReturn < TRACKING_DURATION && entity != null) {
                return processTrackingPhase(current, entity);
            } else {
                return processSmoothReturn(current);
            }
        }

        if (entity == null) {
            return current;
        }

        float neckHeight = (float) (entity.getEyeY() - entity.getY() - 0.3f);
        Vec3d targetPos = entity.getPos().add(0, neckHeight, 0);

        if (lastTarget == entity) {
            float randomOffsetX = (random.nextFloat() - 0.5f) * 0.1f;
            float randomOffsetZ = (random.nextFloat() - 0.5f) * 0.1f;
            targetPos = targetPos.add(randomOffsetX, 0, randomOffsetZ);
        }

        Vec3d eyes = mc.player.getPos().add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Vec3d vecToNeck = targetPos.subtract(eyes);

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToNeck.z, vecToNeck.x)) - 90.0);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vecToNeck.y, Math.hypot(vecToNeck.x, vecToNeck.z))));

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - current.getPitch());

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4f), 22.5f);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4f), 7.0f);

        float randomYawFactor = (random.nextFloat() * 2.5f - 1.5f);
        float randomPitchFactor = (random.nextFloat() * 2.5f - 1.0f);
        float randomThreshold = random.nextFloat() * 2.5f;
        float randomAddition = random.nextFloat() * 3.5f + 2.5f;

        if (lastTarget != entity) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0f);
        } else {
            clampedPitch /= 3.0f;
        }

        if (Math.abs(clampedYaw - lastYaw) <= randomThreshold) {
            clampedYaw = lastYaw + randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        float yaw = current.getYaw() + (yawDelta > 0.0f ? clampedYaw : -clampedYaw);
        float pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0.0f ? clampedPitch : -clampedPitch), -80.0f, 70.0f);

        float gcd = Rotate.gcd();
        if (gcd > 0.0f && gcd < 10.0f && !Float.isInfinite(gcd) && !Float.isNaN(gcd)) {
            yaw = (float) (yaw - (yaw - current.getYaw()) % gcd);
            pitch = (float) (pitch - (pitch - current.getPitch()) % gcd);
        }

        yaw = MathHelper.wrapDegrees(yaw);

        lastYaw = clampedYaw;
        lastPitch = clampedPitch;
        lastTarget = entity;

        return new Rotate(yaw, pitch);
    }

    private Rotate processTrackingPhase(Rotate current, LivingEntity entity) {
        Vec3d eyes = mc.player.getPos().add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Vec3d targetPos = entity.getBoundingBox().getCenter();

        float targetYaw = (float) Math.toDegrees(Math.atan2(targetPos.z - eyes.z, targetPos.x - eyes.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(targetPos.y - eyes.y, Math.sqrt((targetPos.x - eyes.x) * (targetPos.x - eyes.x) + (targetPos.z - eyes.z) * (targetPos.z - eyes.z))));

        Rotate target = new Rotate(targetYaw, targetPitch);

        DeltaRotate delta = current.rotationDeltaTo(target);
        float yawDelta = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float baseSpeed = 0.35f;

        float randomYawJitter = (random.nextFloat() * 2.5f - 1.5f) * 0.5f;
        float randomPitchJitter = (random.nextFloat() * 2.5f - 1.0f) * 0.3f;

        float moveYaw = rotationDifference > 0 ? MathHelper.clamp(yawDelta, -Math.abs(yawDelta / rotationDifference * 180), Math.abs(yawDelta / rotationDifference * 180)) : yawDelta;
        float movePitch = rotationDifference > 0 ? MathHelper.clamp(pitchDelta, -Math.abs(pitchDelta / rotationDifference * 180), Math.abs(pitchDelta / rotationDifference * 180)) : pitchDelta;

        float finalYaw = MathHelper.lerp(baseSpeed, current.getYaw(), current.getYaw() + moveYaw) + randomYawJitter;
        float finalPitch = MathHelper.lerp(baseSpeed, current.getPitch(), current.getPitch() + movePitch) + randomPitchJitter;

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
            isReturning = false;
            lastTargetRotation = null;
            lastTarget = null;
            return playerRotation;
        }

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float returnSpeed = MathHelper.clamp(0.3f + (rotationDifference / 180.0f) * 0.25f, 0.3f, 0.55f);

        float jitterScale = Math.min(rotationDifference / 45.0f, 1.0f);
        float randomYawJitter = (random.nextFloat() * 2.5f - 1.5f) * jitterScale;
        float randomPitchJitter = (random.nextFloat() * 2.5f - 1.0f) * jitterScale * 0.5f;

        float randomThreshold = random.nextFloat() * 2.5f;
        if (random.nextFloat() < 0.1f && rotationDifference > 15.0f) {
            randomYawJitter += (random.nextFloat() - 0.5f) * 3.5f;
        }

        float moveYaw = rotationDifference > 0 ? MathHelper.clamp(yawDelta, -Math.abs(yawDelta / rotationDifference * 180), Math.abs(yawDelta / rotationDifference * 180)) : yawDelta;
        float movePitch = rotationDifference > 0 ? MathHelper.clamp(pitchDelta, -Math.abs(pitchDelta / rotationDifference * 180), Math.abs(pitchDelta / rotationDifference * 180)) : pitchDelta;

        float finalYaw = MathHelper.lerp(returnSpeed, current.getYaw(), current.getYaw() + moveYaw) + randomYawJitter;
        float finalPitch = MathHelper.lerp(returnSpeed, current.getPitch(), current.getPitch() + movePitch) + randomPitchJitter;

        finalPitch = MathHelper.clamp(finalPitch, -90.0f, 90.0f);
        finalYaw = MathHelper.wrapDegrees(finalYaw);

        float gcd = Rotate.gcd();
        if (gcd > 0.0f && gcd < 10.0f && !Float.isInfinite(gcd) && !Float.isNaN(gcd)) {
            finalYaw = (float) (finalYaw - (finalYaw - current.getYaw()) % gcd);
            finalPitch = (float) (finalPitch - (finalPitch - current.getPitch()) % gcd);
        }

        return new Rotate(finalYaw, finalPitch);
    }

    private LivingEntity getTargetEntity() {
        try {
            KillAura killAura = getKillAura();
            return killAura != null ? killAura.getTarget() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private KillAura getKillAura() {
        try {
            return (KillAura) Luxury.getInstance().getModuleManager().getModules().stream().filter(m -> m instanceof KillAura).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public void reset() {
        lastYaw = 0.0f;
        lastPitch = 0.0f;
        lastTarget = null;
        isReturning = false;
        lastTargetRotation = null;
        hadTarget = false;
        lastTargetTime = 0;
    }
}