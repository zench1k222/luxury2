package dev.luxury.mixin.render.impl.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Mutable
    @Accessor("pitch")
    void setPitchField(float pitch);

    @Accessor("pitch")
    float getPitchField();
}