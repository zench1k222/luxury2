package dev.luxury.mixin.render.impl;


import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("itemUseCooldown")
    int getUseCooldown();

    @Accessor("itemUseCooldown")
    void setUseCooldown(int val);
}