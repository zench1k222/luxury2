package dev.luxury.modules.impl.targetesp;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.modules.impl.targetesp.mode.Crystals;
import dev.luxury.modules.impl.targetesp.mode.Ghosts;
import dev.luxury.modules.impl.targetesp.mode.Circle;
import dev.luxury.modules.impl.targetesp.mode.Marker;
import dev.luxury.utils.math.StopWatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;



public class TargetESPHandler {
    private static final double EXTENDED_RANGE = 10.0;
    private static final int FADE_OUT_TICKS = 100;
    public static MinecraftClient mc = MinecraftClient.getInstance();
    private static LivingEntity lastTarget = null;
    private static final StopWatch targetLostTimer = new StopWatch();
    private static boolean isTargetOutOfRange = false;

    public static LivingEntity updateTargetInfo() {
       KillAura attackAura = ModuleManager.getModule(KillAura.class);

        if (attackAura == null || !attackAura.isEnabled()) {
            lastTarget = null;
            return null;
        }

        LivingEntity currentTarget = attackAura.getTarget();

        if (currentTarget != null && currentTarget != lastTarget) {
            lastTarget = currentTarget;
            isTargetOutOfRange = false;
            targetLostTimer.reset();
        }

        if (lastTarget != null && lastTarget.isAlive()) {
            double distance = getDistanceTo(lastTarget);
            double attackRange = attackAura.distance.getValue();

            if (distance <= attackRange) {
                isTargetOutOfRange = false;
                targetLostTimer.reset();
            }
            else if (distance <= EXTENDED_RANGE) {
                isTargetOutOfRange = false;
                targetLostTimer.reset();
            }
            else if (!isTargetOutOfRange) {
                isTargetOutOfRange = true;
                targetLostTimer.reset();
            }

            if (isTargetOutOfRange && targetLostTimer.hasElapsed(FADE_OUT_TICKS * 50)) {
                lastTarget = null;
            }
        }

        return lastTarget;
    }

    public static boolean shouldRenderESP(LivingEntity target) {
        return target != null && target != mc.player && target.isAlive() && target.getHealth() > 0 && mc.world != null;
    }

    public static void renderESP(String mode, MatrixStack matrixStack) {
        LivingEntity target = updateTargetInfo();

        if (shouldRenderESP(target)) {
            switch (mode) {
                case "Marker" -> Marker.render(target, matrixStack);
                case "Ghosts" -> Ghosts.render(target);
                case "Circle" -> Circle.render(target, matrixStack);
                case "Crystals" -> {

                }
            }
        }
    }

    public static void renderESP(String mode, EventRender3D event) {
        LivingEntity target = updateTargetInfo();

        if (shouldRenderESP(target)) {
            switch (mode) {
                case "Crystals" -> Crystals.instance.onRenderWorldEvent(event, target, true, 8F, true);
                case "Marker" -> Marker.render(target, event.getMatrices());
                case "Ghosts" -> Ghosts.render(target);
                case "Circle" -> Circle.render(target, event.getMatrices());
            }
        }
    }

    private static double getDistanceTo(LivingEntity entity) {
        if (mc.player == null) return Double.MAX_VALUE;
        assert mc.player != null;
        return mc.player.getEyePos().distanceTo(entity.getEyePos());
    }
} 