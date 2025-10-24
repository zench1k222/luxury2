package dev.luxury.mixin.render.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public interface CooldownAccessor {

    @Invoker("getAttackCooldownProgress")
    float invokeGetAttackCooldownProgress(float baseTime);

    @Invoker("getAttackCooldownProgressPerTick")
    float invokeGetAttackCooldownProgressPerTick();
}