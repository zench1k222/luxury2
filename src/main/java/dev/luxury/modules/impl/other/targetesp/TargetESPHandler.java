package dev.luxury.modules.impl.other.targetesp;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.combat.KillAura;
import dev.luxury.modules.impl.combat.AimBot;
import dev.luxury.modules.impl.other.elytraaura.ElytraAura;
import dev.luxury.modules.impl.other.targetesp.mode.*;
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
        AimBot aimBot = ModuleManager.getModule(AimBot.class);

        LivingEntity currentTarget = null;

        if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
            currentTarget = aura.getTarget();
        } else if (elytraAura != null && elytraAura.isEnabled() && elytraAura.getTarget() != null) {
            currentTarget = elytraAura.getTarget();
        } else if (aimBot != null && aimBot.isEnabled() && aimBot.getTarget() != null) {
            currentTarget = aimBot.getTarget();
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
                case "Кристаллы" -> Crystals.render(target, matrixStack);
                case "Сфера" -> ChaosSphere.render(target, matrixStack);
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
                case "Кристаллы" -> Crystals.render(target, event.getMatrices());
                case "Сфера" -> ChaosSphere.render(target, event.getMatrices());
            }
        }
    }

    private static double getDistanceTo(LivingEntity entity) {
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.getEyePos().distanceTo(entity.getEyePos());
    }
}