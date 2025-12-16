package dev.luxury.mixin.render.impl.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.luxury.Luxury;
import dev.luxury.modules.impl.SeeInvisible;
import dev.luxury.utils.player.EntityColorEvent;
import dev.luxury.utils.render.RenderUtil3D;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Shadow
    protected abstract RenderLayer getRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer onGetRenderLayer(LivingEntityRenderer instance, S state, boolean showBody, boolean translucent, boolean showOutline) {
        if (state != null && state.invisibleToPlayer) {
            EntityColorEvent event = new EntityColorEvent(-1);

            if (mc.world != null && mc.player != null) {
                if (!SeeInvisible.state) {
                    EventManager.call(event);

                    if (event.isCancelled()) {
                        translucent = true;
                    }
                }
            }
        }

        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void onRenderModel(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer,
                               int i, int j, int color,
                               @Local(argsOnly = true) S renderState) {

        EntityColorEvent event = new EntityColorEvent(color);

        if (renderState != null && renderState.invisibleToPlayer) {
            EventManager.call(event);
        }

        instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void onGetRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        if (state != null && state.invisibleToPlayer) {
            if (!SeeInvisible.state) {
                cir.setReturnValue(this.getRenderLayer(state, showBody, true, showOutline));
            }
        }
    }
    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    public float changeYaw(float oldValue, LivingEntity entity, @Local(argsOnly = true) float delta) {
        if (entity.equals(mc.player) && Luxury.getInstance().getRotationManager() != null &&
                !Luxury.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(delta,
                    Luxury.getInstance().getRotationManager().getPreviousRotate().getYaw(),
                    Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    public float changeHeadYaw(float oldValue, LivingEntity entity, @Local(argsOnly = true) float delta) {
        if (entity.equals(mc.player) && Luxury.getInstance().getRotationManager() != null &&
                !Luxury.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(delta,
                    Luxury.getInstance().getRotationManager().getPreviousRotate().getYaw(),
                    Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    public float changePitch(float oldValue, LivingEntity entity, @Local(argsOnly = true) float delta) {
        if (entity.equals(mc.player) && Luxury.getInstance().getRotationManager() != null &&
                !Luxury.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(delta,
                    Luxury.getInstance().getRotationManager().getPreviousRotate().getPitch(),
                    Luxury.getInstance().getRotationManager().getCurrentRotate().getPitch());
        }
        return oldValue;
    }
}