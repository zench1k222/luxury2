package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.utils.font.FontDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "<init>", at = @At("TAIL"))
    void postWindowInit(RunArgs args, CallbackInfo ci) {
        try {
            FontDraw.sf_medium = FontDraw.create(16f, "sf_medium");
            FontDraw.Montserrat_Medium = FontDraw.create(16f, "Montserrat_Medium");
            FontDraw.Montserrat_Big = FontDraw.create(24f, "Montserrat_Medium");
            FontDraw.icons = FontDraw.create(16f, "icons");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

            EventManager.call(new EventTick());
        }
}