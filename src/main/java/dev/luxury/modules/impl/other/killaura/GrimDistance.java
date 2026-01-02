package dev.luxury.modules.impl.other.killaura;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;

public class GrimDistance {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final float VANILLA_REACH = 3.0f;
    private static final float GRIM_MAX_BUFFER = 0.03f;
    private static final int BACKTRACK_TICKS = 3;

    private static final LinkedList<Vec3d> targetPositionHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 10;
    private static LivingEntity lastTrackedTarget = null;

    public static float calculateSafeReach(LivingEntity target, float baseReach) {
        if (mc.player == null || target == null) return baseReach;

        float safeReach = Math.min(baseReach, VANILLA_REACH);

        float pingBonus = calculatePingCompensation();

        float playerMoveBonus = calculatePlayerMovementBonus();

        float targetMoveBonus = calculateTargetMovementBonus(target);

        float sprintBonus = 0f;
        if (mc.player.isSprinting()) {
            sprintBonus = 0.05f;
        }

        float swingBonus = 0f;
        if (target.handSwingProgress > 0) {
            swingBonus = 0.02f;
        }

        float totalBonus = pingBonus + playerMoveBonus + targetMoveBonus + sprintBonus + swingBonus;

        safeReach = Math.min(safeReach + totalBonus, VANILLA_REACH + GRIM_MAX_BUFFER + pingBonus);

        return Math.min(safeReach, baseReach);
    }

    private static float calculatePingCompensation() {
        int ping = getPlayerPing();

        float compensation = ping * 0.0001f;
        return MathHelper.clamp(compensation, 0f, 0.1f);
    }

    private static float calculatePlayerMovementBonus() {
        if (mc.player == null) return 0f;

        Vec3d velocity = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        return (float) MathHelper.clamp(horizontalSpeed * 0.15f, 0f, 0.03f);
    }

    private static float calculateTargetMovementBonus(LivingEntity target) {
        if (mc.player == null || target == null) return 0f;

        Vec3d targetVelocity = target.getVelocity();
        double targetSpeed = Math.sqrt(targetVelocity.x * targetVelocity.x + targetVelocity.z * targetVelocity.z);

        Vec3d toPlayer = mc.player.getPos().subtract(target.getPos()).normalize();
        Vec3d targetDir = targetVelocity.lengthSquared() > 0.0001 ? targetVelocity.normalize() : Vec3d.ZERO;

        double dot = toPlayer.dotProduct(targetDir);

        float bonus;
        if (dot > 0.3) {
            bonus = (float) (targetSpeed * 0.25f);
        } else if (dot < -0.3) {
            bonus = (float) (targetSpeed * 0.05f);
        } else {
            bonus = (float) (targetSpeed * 0.15f);
        }

        return MathHelper.clamp(bonus, 0f, 0.05f);
    }

    public static int getPlayerPing() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    public static boolean canSafelyAttack(LivingEntity target, float maxReach) {
        if (mc.player == null || target == null) return false;

        double distance = getDistanceToTarget(target);
        float safeReach = calculateSafeReach(target, maxReach);

        return distance <= (safeReach - 0.01f);
    }

    public static double getDistanceToTarget(LivingEntity target) {
        if (mc.player == null || target == null) return Double.MAX_VALUE;

        Vec3d eyePos = mc.player.getEyePos();
        Box targetBox = target.getBoundingBox();

        double closestX = MathHelper.clamp(eyePos.x, targetBox.minX, targetBox.maxX);
        double closestY = MathHelper.clamp(eyePos.y, targetBox.minY, targetBox.maxY);
        double closestZ = MathHelper.clamp(eyePos.z, targetBox.minZ, targetBox.maxZ);

        return eyePos.distanceTo(new Vec3d(closestX, closestY, closestZ));
    }

    public static double getDistanceToCenter(LivingEntity target) {
        if (mc.player == null || target == null) return Double.MAX_VALUE;
        return mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
    }

    public static void updateTargetHistory(LivingEntity target) {
        if (target == null) {
            targetPositionHistory.clear();
            lastTrackedTarget = null;
            return;
        }

        if (lastTrackedTarget != target) {
            targetPositionHistory.clear();
            lastTrackedTarget = target;
        }

        targetPositionHistory.addFirst(target.getPos());

        while (targetPositionHistory.size() > MAX_HISTORY_SIZE) {
            targetPositionHistory.removeLast();
        }
    }

    public static Vec3d getBacktrackPosition(int ticksBack) {
        if (targetPositionHistory.isEmpty()) return null;

        int index = Math.min(ticksBack, targetPositionHistory.size() - 1);
        return targetPositionHistory.get(index);
    }

    public static boolean wasInRangeRecently(LivingEntity target, float maxReach, int ticksBack) {
        if (mc.player == null || targetPositionHistory.isEmpty()) return false;

        Vec3d eyePos = mc.player.getEyePos();
        float safeReach = calculateSafeReach(target, maxReach);

        int checkTicks = Math.min(ticksBack, targetPositionHistory.size());

        for (int i = 0; i < checkTicks; i++) {
            Vec3d historicPos = targetPositionHistory.get(i);

            Box historicBox = target.getBoundingBox().offset(historicPos.subtract(target.getPos()));

            double closestX = MathHelper.clamp(eyePos.x, historicBox.minX, historicBox.maxX);
            double closestY = MathHelper.clamp(eyePos.y, historicBox.minY, historicBox.maxY);
            double closestZ = MathHelper.clamp(eyePos.z, historicBox.minZ, historicBox.maxZ);

            double distance = eyePos.distanceTo(new Vec3d(closestX, closestY, closestZ));

            if (distance <= safeReach) {
                return true;
            }
        }

        return false;
    }

    public static String getDebugInfo(LivingEntity target, float maxReach) {
        if (target == null) return "No target";

        double distance = getDistanceToTarget(target);
        float safeReach = calculateSafeReach(target, maxReach);
        int ping = getPlayerPing();

        return String.format("Dist: %.2f | Safe: %.2f | Ping: %dms", distance, safeReach, ping);
    }

    public static void reset() {
        targetPositionHistory.clear();
        lastTrackedTarget = null;
    }
}