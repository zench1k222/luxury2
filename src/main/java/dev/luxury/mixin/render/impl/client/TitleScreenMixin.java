package dev.luxury.mixin.render.impl.client;


import dev.luxury.modules.impl.render.ClientSounds;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    private static boolean hasPlayedStartSound = false;

    @Inject(method = "init()V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!hasPlayedStartSound) {
            hasPlayedStartSound = true;
            ClientSounds.getInstance().playClientStartSound();
        }
    }
}