package dev.luxury.mixin.render.impl.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luxury.Luxury;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Entity.class)
public abstract class EntityMixin {
public final MinecraftClient mc = MinecraftClient.getInstance();
    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    public boolean fixFalldistanceValue(boolean original) {
        if ((Object) this == mc.player) {
            return false;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
    public float movementCorrection(Entity instance) {

        if (instance instanceof ClientPlayerEntity) { //ПРИВЕТ ЛЮДИ С БОЛЬШИМ МОНИТОРОМ
            return Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw();
        }

        return instance.getYaw();
    }
    @ModifyVariable(
            method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float modifyPitch(float pitch) {
        if ((Object) this instanceof ClientPlayerEntity) {
            return Luxury.getInstance().getRotationManager().getCurrentRotate().getPitch();
        }
        return pitch;
    }

    @ModifyVariable(
            method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private float modifyYaw(float yaw) {
        if ((Object) this instanceof ClientPlayerEntity) {
            return Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw();
        }
        return yaw;
    }


}
