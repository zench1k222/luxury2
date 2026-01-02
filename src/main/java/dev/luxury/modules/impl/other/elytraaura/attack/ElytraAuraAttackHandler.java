package dev.luxury.modules.impl.other.elytraaura.attack;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import static dev.luxury.modules.api.Module.mc;

public class ElytraAuraAttackHandler {

    public static void updateAttack(LivingEntity entity, float range) {
        if (mc.player == null || mc.world == null || entity == null) return;
        if (mc.player.distanceTo(entity) > range) return;
        if (mc.player.getAttackCooldownProgress(1.0f) < 0.92f) return;

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

