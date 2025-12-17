package dev.luxury.mixin.render.impl.client;


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luxury.Luxury;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>  {
    MinecraftClient mc = MinecraftClient.getInstance();
    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    public float changeYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Luxury.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(RenderUtil3D.getTickDelta(),Luxury.getInstance().getRotationManager().getPreviousRotate().getYaw(),Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    public float changeHeadYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player)&&!Luxury.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(RenderUtil3D.getTickDelta(),Luxury.getInstance().getRotationManager().getPreviousRotate().getYaw(),Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    public float changePitch(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Luxury.getInstance().getRotationManager().isSetRotation()) {
            return   MathHelper.lerpAngleDegrees(RenderUtil3D.getTickDelta(),Luxury.getInstance().getRotationManager().getPreviousRotate().getPitch(),Luxury.getInstance().getRotationManager().getCurrentRotate().getPitch());

        }
        return oldValue;
    }
}