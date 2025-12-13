package dev.luxury.modules.impl.targetesp;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.modules.impl.elytraaura.ElytraAura;
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
        KillAura aura = ModuleManager.getModule(KillAura.class);
        ElytraAura elytraAura = ModuleManager.getModule(ElytraAura.class);

        LivingEntity currentTarget = null;

        if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
            currentTarget = aura.getTarget();
        } else if (elytraAura != null && elytraAura.isEnabled() && elytraAura.getTarget() != null) {
            currentTarget = elytraAura.getTarget();
        }

        if (currentTarget != null && currentTarget != lastTarget) {
            lastTarget = currentTarget;
            isTargetOutOfRange = false;
            targetLostTimer.reset();
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
                case "Маркер" -> Marker.render(target, matrixStack);
                case "Призраки" -> Ghosts.render(target);
                case "Круг" -> Circle.render(target, matrixStack);

            }
        }
    }

    public static void renderESP(String mode, EventRender3D event) {
        LivingEntity target = updateTargetInfo();

        if (shouldRenderESP(target)) {
            switch (mode) {
                case "Маркер" -> Marker.render(target, event.getMatrices());
                case "Призраки" -> Ghosts.render(target);
                case "Круг" -> Circle.render(target, event.getMatrices());
            }
        }
    }

    private static double getDistanceTo(LivingEntity entity) {
        if (mc.player == null) return Double.MAX_VALUE;
        assert mc.player != null;
        return mc.player.getEyePos().distanceTo(entity.getEyePos());
    }
} 