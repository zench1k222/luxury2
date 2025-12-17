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

    private float returnLastYaw = 0.0f;
    private float returnLastPitch = 0.0f;

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        boolean isEnabled = killAura != null && killAura.isEnabled();
        LivingEntity entity = getTargetEntity();
        boolean hasTarget = entity != null;

        boolean shouldStartReturning = !isEnabled || (!hasTarget && hadTarget);

        if (shouldStartReturning && !isReturning) {
            isReturning = true;
            returnStartTime = System.currentTimeMillis();
            lastTargetRotation = current;
            returnLastYaw = lastYaw;
            returnLastPitch = lastPitch;
        }

        if (isEnabled && hasTarget && isReturning) {
            isReturning = false;
            lastTargetRotation = null;
        }

        hadTarget = hasTarget;

        // Обработка возвращения
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
        float targetPitch = (float) -Math.toDegrees(Math.atan2(targetPos.y - eyes.y,
                Math.sqrt((targetPos.x - eyes.x) * (targetPos.x - eyes.x) + (targetPos.z - eyes.z) * (targetPos.z - eyes.z))));

        float yawDelta = MathHelper.wrapDegrees(targetYaw - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - current.getPitch());

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4f), 22.5f);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4f), 7.0f);

        float randomYawFactor = (random.nextFloat() * 2.5f - 1.5f);
        float randomPitchFactor = (random.nextFloat() * 2.5f - 1.0f);
        float randomThreshold = random.nextFloat() * 2.5f;
        float randomAddition = random.nextFloat() * 3.5f + 2.5f;

        clampedPitch /= 3.0f;

        if (Math.abs(clampedYaw - returnLastYaw) <= randomThreshold) {
            clampedYaw = returnLastYaw + randomAddition;
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

        returnLastYaw = clampedYaw;
        returnLastPitch = clampedPitch;

        return new Rotate(yaw, pitch);
    }

    private Rotate processSmoothReturn(Rotate current) {
        Rotate playerRotation = new Rotate(mc.player.getYaw(), mc.player.getPitch());

        float yawDelta = MathHelper.wrapDegrees(playerRotation.getYaw() - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(playerRotation.getPitch() - current.getPitch());

        if (Math.abs(yawDelta) < 1.5f && Math.abs(pitchDelta) < 1.5f) {
            isReturning = false;
            lastTargetRotation = null;
            lastTarget = null;
            return playerRotation;
        }

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4f), 22.5f);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4f), 7.0f);

        float randomYawFactor = (random.nextFloat() * 2.5f - 1.5f);
        float randomPitchFactor = (random.nextFloat() * 2.5f - 1.0f);
        float randomThreshold = random.nextFloat() * 2.5f;
        float randomAddition = random.nextFloat() * 3.5f + 2.5f;

        clampedPitch /= 3.0f;

        if (Math.abs(clampedYaw - returnLastYaw) <= randomThreshold) {
            clampedYaw = returnLastYaw + randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        clampedYaw *= 0.7f;
        clampedPitch *= 0.7f;

        float yaw = current.getYaw() + (yawDelta > 0.0f ? clampedYaw : -clampedYaw);
        float pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0.0f ? clampedPitch : -clampedPitch), -80.0f, 70.0f);

        float gcd = Rotate.gcd();
        if (gcd > 0.0f && gcd < 10.0f && !Float.isInfinite(gcd) && !Float.isNaN(gcd)) {
            yaw = (float) (yaw - (yaw - current.getYaw()) % gcd);
            pitch = (float) (pitch - (pitch - current.getPitch()) % gcd);
        }

        yaw = MathHelper.wrapDegrees(yaw);

        returnLastYaw = clampedYaw;
        returnLastPitch = clampedPitch;

        return new Rotate(yaw, pitch);
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
        returnLastYaw = 0.0f;
        returnLastPitch = 0.0f;
    }
}