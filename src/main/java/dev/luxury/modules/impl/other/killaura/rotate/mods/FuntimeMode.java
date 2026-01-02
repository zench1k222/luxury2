package dev.luxury.modules.impl.other.killaura.rotate.mods;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.combat.KillAura;
import dev.luxury.modules.impl.other.killaura.InterpolationUtil;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import dev.luxury.modules.impl.other.killaura.rotate.mods.api.RotationMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class FuntimeMode extends RotationMode {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private long lastHitMs = 0;
    private long shakeStartTime = 0L;
    private LivingEntity lastTarget = null;

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        LivingEntity entity = getTargetEntity();

        boolean isAttacking = killAura != null && killAura.isCanAttack();

        if (isAttacking && entity != null) {
            lastHitMs = System.currentTimeMillis();
            shakeStartTime = 0L;
        }

        if (System.currentTimeMillis() - lastHitMs < 450 && entity != null) {
            return processFuntimeAim(current, entity);
        } else {
            return processShakeRotation(current);
        }
    }

    private Rotate processFuntimeAim(Rotate current, LivingEntity entity) {
        Vec3d eyes = mc.player.getPos().add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Vec3d basePos = KillAura.instance != null && KillAura.instance.shouldRemoveInterpolation() ? InterpolationUtil.getPosition(entity) : entity.getPos();
        float neckHeight = (float) (entity.getEyeY() - entity.getY() - 0.3f);
        Vec3d targetPos = basePos.add(0, neckHeight, 0);

        if (lastTarget == entity) {
            float randomOffsetX = (random.nextFloat() - 0.5f) * 0.15f;
            float randomOffsetZ = (random.nextFloat() - 0.5f) * 0.15f;
            targetPos = targetPos.add(randomOffsetX, 0, randomOffsetZ);
        }

        Vec3d vecToTarget = targetPos.subtract(eyes);

        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToTarget.z, vecToTarget.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vecToTarget.y, Math.hypot(vecToTarget.x, vecToTarget.z))));

        float yawDelta = MathHelper.wrapDegrees(targetYaw - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - current.getPitch());

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4f), 35f);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4f), 12.0f);

        float randomYawFactor = (random.nextFloat() * 3.0f - 1.5f);
        float randomPitchFactor = (random.nextFloat() * 2.0f - 1.0f);

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        float yaw = current.getYaw() + (yawDelta > 0.0f ? clampedYaw : -clampedYaw);
        float pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0.0f ? clampedPitch : -clampedPitch), -80.0f, 70.0f);

        yaw = applyGCD(yaw, current.getYaw());
        pitch = applyGCD(pitch, current.getPitch());

        yaw = MathHelper.wrapDegrees(yaw);
        lastTarget = entity;

        return new Rotate(yaw, pitch);
    }

    private Rotate processShakeRotation(Rotate current) {
        long currentTime = System.currentTimeMillis();

        if (shakeStartTime == 0L) {
            shakeStartTime = currentTime;
        }

        float elapsedSec = (currentTime - shakeStartTime) / 1000f;

        double angle = 2 * Math.PI * 2.4 * elapsedSec;
        float yawOffset = (float) Math.sin(angle) * 24f;

        double angle2 = 2 * Math.PI * 0.08f * elapsedSec;
        double[] options = {5.0, 5.5, 5.8, 6.0};
        double randAmplitude = options[(int) (Math.random() * options.length)];
        float yawOffset2 = (float) (Math.sin(angle2) * randAmplitude);

        float finalYaw = mc.player.getYaw() + yawOffset + yawOffset2;
        float finalPitch = 0.0f + yawOffset2;

        float yawDelta = MathHelper.wrapDegrees(finalYaw - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(finalPitch - current.getPitch());

        float yawSpeed = Math.min(Math.abs(yawDelta), 20f);
        float pitchSpeed = Math.min(Math.abs(pitchDelta), 10f);

        float yaw = current.getYaw() + (yawDelta > 0 ? yawSpeed : -yawSpeed);
        float pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed), -80.0f, 70.0f);

        yaw = applyGCD(yaw, current.getYaw());
        pitch = applyGCD(pitch, current.getPitch());

        yaw = MathHelper.wrapDegrees(yaw);

        return new Rotate(yaw, pitch);
    }

    private float applyGCD(float value, float prev) {
        float gcd = Rotate.gcd();
        if (gcd > 0.0f && gcd < 10.0f && !Float.isInfinite(gcd) && !Float.isNaN(gcd)) {
            return (float) (value - (value - prev) % gcd);
        }
        return value;
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
        lastHitMs = 0;
        shakeStartTime = 0L;
        lastTarget = null;
    }
}