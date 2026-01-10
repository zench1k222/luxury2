package dev.luxury.modules.impl.other.killaura.rotate.mods;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.combat.KillAura;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import dev.luxury.modules.impl.other.killaura.rotate.mods.api.RotationMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

public class HolyWorldMode extends RotationMode {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private LivingEntity lastTarget = null;
    private boolean isAttacking = false;

    private final float twitchIntensity = 0.15f;
    private final float twitchFrequency = 0.03f;
    private final float maxYawChange = 40.0f;
    private final float maxPitchChange = 35.0f;
    private final float randomOffset = 0.03f;

    private float rotationYawSpeed = 15.0f;
    private float rotationPitchSpeed = 10.0f;

    public static float gcd() {
        double f = net.minecraft.client.MinecraftClient.getInstance().options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
        return (float) (f * f * f * 8.0 * 0.15f);
    }

    public Rotate process(Rotate target) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        LivingEntity targetEntity = getTargetEntity();
        boolean attack = killAura != null && killAura.isCanAttack();
        boolean hasTarget = targetEntity != null;

        isAttacking = attack && hasTarget;

        if (!hasTarget) {
            lastTarget = null;
            return current;
        }

        if (lastTarget != targetEntity) {
            lastTarget = targetEntity;
            lastYaw = 0.0f;
            lastPitch = 0.0f;
        }

        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());

        float yaw;
        float pitch;

        if (isAttacking && shouldAccelerateOnAttack()) {
            yaw = current.getYaw() + yawDelta;
            pitch = MathHelper.clamp(current.getPitch() + pitchDelta, -89.0F, 89.0F);
        } else {
            float yawSpeed = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed * 2.0f);
            float pitchSpeed = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed * 2.0f);

            yaw = current.getYaw() + (yawDelta > 0 ? yawSpeed : -yawSpeed);
            pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed), -89.0F, 89.0F);
        }

        if (mc.player.age % Math.max(1, (int)(twitchFrequency * 15)) == 0) {
            yaw += (float) (random.nextDouble() - 0.5) * twitchIntensity;
            pitch += (float) (random.nextDouble() - 0.5) * twitchIntensity;
        }

        yaw += (float) (random.nextDouble() - 0.5) * randomOffset;
        pitch += (float) (random.nextDouble() - 0.5) * randomOffset;

        float gcd = gcd();
        if (gcd > 0.0f && gcd < 10.0f) {
            float gcdRandomizer = (float) (random.nextDouble() * 0.008f + 0.996f);
            yaw = current.getYaw() + ((yaw - current.getYaw()) - ((yaw - current.getYaw()) % (gcd * gcdRandomizer)));
            pitch = current.getPitch() + ((pitch - current.getPitch()) - ((pitch - current.getPitch()) % (gcd * gcdRandomizer)));
        }

        float yawChange = yaw - current.getYaw();
        float pitchChange = pitch - current.getPitch();

        yawChange = MathHelper.clamp(yawChange, -maxYawChange, maxYawChange);
        pitchChange = MathHelper.clamp(pitchChange, -maxPitchChange, maxPitchChange);

        yaw = current.getYaw() + yawChange;
        pitch = MathHelper.clamp(current.getPitch() + pitchChange, -89.0F, 89.0F);

        lastYaw = yaw;
        lastPitch = pitch;

        if (shouldCorrectMovement()) {
            mc.player.setYaw(yaw);
        }

        return new Rotate(yaw, pitch);
    }

    public Rotate processWithOptions(Rotate target, boolean accelerateOnAttack, boolean correctMovement) {
        if (mc.player == null) return target;

        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();

        KillAura killAura = getKillAura();
        LivingEntity targetEntity = getTargetEntity();
        boolean attack = killAura != null && killAura.isCanAttack();
        boolean hasTarget = targetEntity != null;
        boolean selected = lastTarget != null && lastTarget.isAlive();

        if (!hasTarget) {
            lastTarget = null;
            return current;
        }

        if (lastTarget != targetEntity) {
            lastTarget = targetEntity;
            lastYaw = 0.0f;
            lastPitch = 0.0f;
        }

        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());

        float yaw;
        float pitch;

        if (attack && selected && accelerateOnAttack) {
            yaw = current.getYaw() + yawDelta;
            pitch = MathHelper.clamp(current.getPitch() + pitchDelta, -89.0F, 89.0F);
        } else {
            float yawSpeed = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed * 2.0f);
            float pitchSpeed = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed * 2.0f);

            yaw = current.getYaw() + (yawDelta > 0 ? yawSpeed : -yawSpeed);
            pitch = MathHelper.clamp(current.getPitch() + (pitchDelta > 0 ? pitchSpeed : -pitchSpeed), -89.0F, 89.0F);
        }

        if (mc.player.age % Math.max(1, (int)(twitchFrequency * 15)) == 0) {
            yaw += (float) (random.nextDouble() - 0.5) * twitchIntensity;
            pitch += (float) (random.nextDouble() - 0.5) * twitchIntensity;
        }

        yaw += (float) (random.nextDouble() - 0.5) * randomOffset;
        pitch += (float) (random.nextDouble() - 0.5) * randomOffset;

        float gcd = gcd();
        if (gcd > 0.0f && gcd < 10.0f) {
            float gcdRandomizer = (float) (random.nextDouble() * 0.008f + 0.996f);
            yaw = current.getYaw() + ((yaw - current.getYaw()) - ((yaw - current.getYaw()) % (gcd * gcdRandomizer)));
            pitch = current.getPitch() + ((pitch - current.getPitch()) - ((pitch - current.getPitch()) % (gcd * gcdRandomizer)));
        }

        yaw = current.getYaw() + MathHelper.clamp(yaw - current.getYaw(), -maxYawChange, maxYawChange);
        pitch = MathHelper.clamp(current.getPitch() + MathHelper.clamp(pitch - current.getPitch(), -maxPitchChange, maxPitchChange), -89.0F, 89.0F);

        lastYaw = yaw;
        lastPitch = pitch;

        if (correctMovement) {
            mc.player.setYaw(yaw);
        }

        return new Rotate(yaw, pitch);
    }

    private boolean shouldAccelerateOnAttack() {
        return true;
    }

    private boolean shouldCorrectMovement() {
        return true;
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
            return (KillAura) Luxury.getInstance().getModuleManager().getModules().stream()
                    .filter(m -> m instanceof KillAura)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public void reset() {
        lastYaw = 0.0f;
        lastPitch = 0.0f;
        lastTarget = null;
        isAttacking = false;
    }

    public void setRotationSpeed(float yawSpeed, float pitchSpeed) {
        this.rotationYawSpeed = yawSpeed;
        this.rotationPitchSpeed = pitchSpeed;
    }

    public void setTwitchParameters(float intensity, float frequency) {
    }

    public void setMaxChanges(float maxYaw, float maxPitch) {
    }
}