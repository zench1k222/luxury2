package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventSpawnEntity;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntity", at = @At(value = "RETURN"))
    public void injectAddEntity(Entity entity, CallbackInfo ci) {
        EventSpawnEntity eventSpawnEntity = new EventSpawnEntity(entity);
        EventManager.call(eventSpawnEntity);
    }
}