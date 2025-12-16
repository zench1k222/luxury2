package dev.luxury.mixin.render.impl.render;



import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.NoPush;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(at = @At(value = "HEAD"), method = "render")
    public void renderHook(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo ci) {

        RenderSystem.enableDepthTest();
        MatrixStack matrices = drawContext.getMatrices();
        EventManager.call(new EventRender2D(drawContext,matrices,tickCounter));

        RenderSystem.disableDepthTest();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    public void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

    }
}
