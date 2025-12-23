package dev.luxury.mixin.render.impl.client;


import dev.luxury.ui.MainMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof MainMenu) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderPanoramaBackground(DrawContext context, float delta, CallbackInfo ci) {
        if ((Object) this instanceof MainMenu) {
            ci.cancel();
        }
    }
}