package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.targetesp.mode.Ghosts;
import dev.luxury.modules.impl.targetesp.mode.Marker;
import dev.luxury.mixin.render.impl.CooldownAccessor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Random;

@ModuleAnnotation(
        name = "KillAura",
        desc = "",
        category = Category.Combat,
        key = GLFW.GLFW_KEY_R
)
public class KillAura extends Module {
    @Getter
    private float fallDistance;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private LivingEntity target;

    private float bodyYaw = 0, prevBodyYaw = 0;
    private float bodyPitch = 0, prevBodyPitch = 0;
    private float headYaw = 0, prevHeadYaw = 0;
    private float headPitch = 0, prevHeadPitch = 0;

    private final Random random = new Random();
    private float randomYawOffset = 0, randomPitchOffset = 0;
    private int randomUpdateTicks = 0;
    private int rotationTicks = 0;

    private final float maxYawShake = 1.5f;
    private final float maxPitchShake = 1.45f;
    private final int updateInterval = 2;
    private final int minrotticks = 2;
    private final float tresholdatack = 8.0f;
    private int sprintDisableTicks = 0;

    private boolean stopSprint = true;
    private String rotationMode = "Spooky";
    public final double range = 3.5f;

    @EventTarget
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        target = findTarget();

        if (target == null) {
            randomYawOffset = 0;
            randomPitchOffset = 0;
            rotationTicks = 0;
            sprintDisableTicks = 0;
            return;
        }

        double yDiff = mc.player.prevY - mc.player.getY();
        if (mc.player.isOnGround()) fallDistance = 0;
        else if (yDiff > 0) fallDistance += (float) yDiff;

        updateRandomOffsets();
        Vec3d targetPos = getTargetPosition(target);
        Vec2f rotation = rotation(targetPos);
        if (rotation == null) return;

        switch (rotationMode.toLowerCase()) {
            case "grim" -> applyGrimRotation(rotation);
            case "spooky" -> applySpookyRotation(rotation);
        }

        if (isLookingAtTarget(rotation)) rotationTicks++;
        else rotationTicks = 0;

        if (!isAttackReady() || rotationTicks < minrotticks) return;

        if (stopSprint && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            sprintDisableTicks = 2;
            return;
        }

        if (sprintDisableTicks > 0) {
            sprintDisableTicks--;
            return;
        }

        if (canCritical(target) && shouldAttack()) {
            attack(target);
            rotationTicks = 0;
        }
    }


    public float getCooldown() {
        if (mc.player.getMainHandStack().getItem() == Items.AIR) return 1;

        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.LEVITATION) || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.isInLava() || mc.player.isGliding() || mc.player.getAbilities().flying)
            return 0.944f;

        if (mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof ShovelItem)
            return 0.99f;

        if (mc.player.isGliding()) return 1;

        return 0.944f;
    }

    public boolean canFall() {
        return ((getBlock(0, 3, 0) == Blocks.AIR && getBlock(0, 2, 0) == Blocks.AIR && getBlock(0, 1, 0) == Blocks.AIR) || fallDistance < (getBlock(0, 2, 0) != Blocks.AIR ? 0.08f : 0.6f) || fallDistance > 1.2f);
    }

    public boolean canCritical(LivingEntity target) {
        double yDiff = (double)((int) mc.player.getY()) - mc.player.getY();
        boolean bl4 = yDiff == -0.01250004768371582;
        boolean bl5 = yDiff == -0.1875;

        return (!mc.player.isOnGround() &&fallDistance > 0f && canFall() || target != null && getBlock(0, 2, 0) != Blocks.AIR && getBlock(0, -1, 0) != Blocks.AIR) || ((bl5 || bl4) && !mc.player.isSneaking() || mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.LEVITATION) || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.isInLava() || mc.player.getAbilities().flying);// mc.player.isOnGround() && !mc.options.jumpKey.isPressed());
    }
    public Block getBlock(double x, double y, double z) {
        return !fullNullCheck() ? Blocks.AIR : mc.world.getBlockState(mc.player.getBlockPos().add((int) x, (int) y, (int) z)).getBlock();
    }

    public boolean findFall(float fallDistance) {
        Vec3d rotationVec = mc.player.getRotationVector();
        double tempVelocityX = mc.player.getVelocity().x;
        double tempVelocityY = mc.player.getVelocity().y;
        double tempVelocityZ = mc.player.getVelocity().z;

        float n = MathHelper.cos(mc.player.getPitch() * 0.017453292f);
        n = (float) (n * n * Math.min(rotationVec.length() / 0.4, 1.0));

        Vec3d vec3d = new Vec3d(tempVelocityX, tempVelocityY, tempVelocityZ).add(0.0, 0.08 * (-1.0 + n * 0.75), 0.0);
        tempVelocityY = vec3d.y * 0.9800000190734863;

        return tempVelocityY < fallDistance;
    }

    public boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }

    private boolean isAttackReady() {
        try {
            float progress = ((CooldownAccessor) mc.player).invokeGetAttackCooldownProgress(0.5f);
            return progress >= getCooldown();
        } catch (Exception ex) {
            return true;
        }
    }

    private boolean shouldAttack() {
        if (mc.player.getAttackCooldownProgress(0f) < getCooldown()) return false;

        return canCritical(target);
    }

    private void attack(LivingEntity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        Ghosts.onHit(target);
        Marker.onHit(target);
    }

    private void applyGrimRotation(Vec2f rot) {
        Vec3d vec = getVectorToTarget(target);
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
        updateRotations(yaw, pitch);
    }

    private void applySpookyRotation(Vec2f rot) {
        Vec3d vec = getVectorToTarget(target);
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
        updateRotations(yaw, pitch);
    }

    private void updateRotations(float yaw, float pitch) {
        float yawDelta = MathHelper.wrapDegrees(yaw - bodyYaw);
        float pitchDelta = MathHelper.wrapDegrees(pitch - bodyPitch);

        float newYaw = bodyYaw + yawDelta;
        float newPitch = MathHelper.clamp(bodyPitch + pitchDelta, -89.0F, 89.0F);

        float gcd = getGCDValue();
        newYaw -= (newYaw - bodyYaw) % gcd;
        newPitch -= (newPitch - bodyPitch) % gcd;

        prevBodyYaw = bodyYaw;
        prevBodyPitch = bodyPitch;
        prevHeadYaw = headYaw;
        prevHeadPitch = headPitch;

        bodyYaw = headYaw = newYaw;
        bodyPitch = headPitch = newPitch;
    }

    private Vec3d getVectorToTarget(LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() / 1.3, 0.0)
                .subtract(mc.player.getEyePos());
        double dx = target.getX() - target.prevX;
        double dy = target.getY() - target.prevY;
        double dz = target.getZ() - target.prevZ;
        return pos.add(dx, dy, dz);
    }

    private float getGCDValue() {
        double sens = mc.options.getMouseSensitivity().getValue();
        float f = (float) (sens * 0.6 + 0.2);
        return f * f * f * 1.2F;
    }

    private boolean isLookingAtTarget(Vec2f rot) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees(rot.y - bodyYaw));
        float pitchDiff = Math.abs(rot.x - bodyPitch);
        return yawDiff <= tresholdatack && pitchDiff <= tresholdatack;
    }

    private void updateRandomOffsets() {
        randomUpdateTicks++;
        if (randomUpdateTicks >= updateInterval) {
            randomUpdateTicks = 0;
            randomYawOffset = (random.nextFloat() * 2 - 1) * maxYawShake;
            randomPitchOffset = (random.nextFloat() * 2 - 1) * maxPitchShake;
        }
    }

    private LivingEntity findTarget() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range), this::isValidTarget)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(mc.player)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == null || e == mc.player || !e.isAlive()) return false;
        if (mc.player.distanceTo(e) > range) return false;
        if (e instanceof PlayerEntity p) {
            AntiBot antiBot = ModuleManager.getModule(AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && antiBot.isBot(p)) return false;
        }
        return true;
    }

    private Vec3d getTargetPosition(LivingEntity target) {
        Vec3d center = target.getBoundingBox().getCenter();
        double y = target.getY() + target.getEyeHeight(target.getPose()) * 0.85;
        return new Vec3d(center.x, y, center.z);
    }

    private Vec2f rotation(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        return new Vec2f((float) (-(Math.atan2(dy, hDist) * 180 / Math.PI)),
                (float) ((Math.atan2(dz, dx) * 180 / Math.PI) - 90.0f));
    }

    @Override
    public void onEnable() { super.onEnable(); }

    @Override
    public void onDisable() { super.onDisable(); }

    public boolean shouldStopSprinting() { return stopSprint && target != null; }
    public boolean hasTarget() { return target != null; }
    public LivingEntity getTarget() { return target; }
    public float getBodyYaw() { return bodyYaw; }
    public float getBodyPitch() { return bodyPitch; }
    public float getHeadYaw() { return headYaw; }
    public float getHeadPitch() { return headPitch; }
    public float getPrevBodyYaw() { return prevBodyYaw; }
    public float getPrevBodyPitch() { return prevBodyPitch; }
    public float getPrevHeadYaw() { return prevHeadYaw; }
    public float getPrevHeadPitch() { return prevHeadPitch; }

    public void setRotationMode(String mode) { this.rotationMode = mode; }
    public String getRotationMode() { return rotationMode; }
    public void setStopSprint(boolean v) { this.stopSprint = v; }
    public boolean isStopSprint() { return stopSprint; }
}