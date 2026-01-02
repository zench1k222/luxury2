package dev.luxury.modules.impl.other.killaura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class InterpolationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vec3d getPosition(LivingEntity entity) {
        if (entity == null) return Vec3d.ZERO;

        if (entity instanceof PlayerEntity && entity != mc.player) {
            return entity.getPos();
        }

        return entity.getPos();
    }

    public static Vec3d getEyePosition(LivingEntity entity) {
        Vec3d pos = getPosition(entity);
        return pos.add(0, entity.getStandingEyeHeight(), 0);
    }

    public static Box getBoundingBox(LivingEntity entity) {
        if (entity == null) {
            return new Box(0, 0, 0, 0, 0, 0);
        }

        return entity.getBoundingBox();
    }

    public static Vec3d getBoxCenter(LivingEntity entity) {
        return getBoundingBox(entity).getCenter();
    }

    public static double getDistanceToBox(LivingEntity target) {
        if (mc.player == null || target == null) {
            return Double.MAX_VALUE;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Box targetBox = getBoundingBox(target);

        double closestX = MathHelper.clamp(eyePos.x, targetBox.minX, targetBox.maxX);
        double closestY = MathHelper.clamp(eyePos.y, targetBox.minY, targetBox.maxY);
        double closestZ = MathHelper.clamp(eyePos.z, targetBox.minZ, targetBox.maxZ);

        return eyePos.distanceTo(new Vec3d(closestX, closestY, closestZ));
    }

    public static double getDistanceToCenter(LivingEntity target) {
        if (mc.player == null || target == null) {
            return Double.MAX_VALUE;
        }

        return mc.player.getEyePos().distanceTo(getBoxCenter(target));
    }

    public static Vec3d getVectorToTarget(LivingEntity target, Vec3d point) {
        if (mc.player == null) {
            return Vec3d.ZERO;
        }

        return point.subtract(mc.player.getEyePos());
    }
}