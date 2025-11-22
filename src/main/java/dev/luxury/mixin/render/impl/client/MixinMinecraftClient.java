package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        EventManager.call(new EventTick());
    }

}
