package dev.luxury.modules.impl.killaura;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.utils.math.TimerUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;

import java.util.Random;


@Setter
@Getter
@UtilityClass
public class Criticals {
    MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtils attackTimer = new TimerUtils();
    private final Random random = new Random();
    private int count = 0;
    private long nextAttackDelay = 0;
    private long lastAttackTime = 0;




    public void attackEntity(Entity entity) {
        long currentTime = System.currentTimeMillis();

        if (isSlothAIMode()) {
            if (currentTime - lastAttackTime < nextAttackDelay) {
                return;
            }
            nextAttackDelay = 50 + random.nextInt(100);
        }

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
        lastAttackTime = currentTime;
        count++;
    }

    private boolean isSlothAIMode() {
        try {
            KillAura killAura = (KillAura) Luxury.getInstance().getModuleManager()
                    .getModules().stream()
                    .filter(m -> m instanceof KillAura && m.isEnabled())
                    .findFirst()
                    .orElse(null);

            if (killAura != null) {
                String currentMode = killAura.getRotationMode().get();
                return currentMode != null && currentMode.equalsIgnoreCase("SlothAI");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }




    public boolean hasMovementRestrictions() {
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerHelper.isPlayerInBlock(Blocks.COBWEB)
                || mc.player.isSubmergedInWater()
                || mc.player.isInLava()
                || mc.player.isClimbing()
                || !PlayerHelper.canChangeIntoPose(EntityPose.STANDING) && mc.player.isInSneakingPose()
                || mc.player.getAbilities().flying;
    }

    public boolean hasPreMovementRestrictions(Simulation simulatedPlayer) {
        return simulatedPlayer.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerHelper.isBoxInBlock(simulatedPlayer.boundingBox, Blocks.COBWEB)
                || simulatedPlayer.isSubmergedInWater()
                || simulatedPlayer.isInLava()
                || simulatedPlayer.isClimbing()
                || !PlayerHelper.canChangeIntoPose(EntityPose.STANDING) && mc.player.isInSneakingPose()
                || mc.player.getAbilities().flying;
    }

    public boolean isPlayerInCriticalState() {
        boolean crit = mc.player.fallDistance > 0 && (mc.player.fallDistance < 0.08 || !Simulation.simulateLocalPlayer(1).onGround);
        return !mc.player.isOnGround() && (crit );
    }


    public boolean isPrePlayerInCriticalState(  Simulation simulatedPlayer) {
        boolean crit = simulatedPlayer.fallDistance > 0 && (simulatedPlayer.fallDistance < 0.08 || !Simulation.simulateLocalPlayer(2).onGround);
        return !simulatedPlayer.onGround && (crit );
    }

}