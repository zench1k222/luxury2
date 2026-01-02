package dev.luxury.mixin.render.impl.client;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void tickMovement(CallbackInfo ci) {

    }

    @Mutable
    float savedYaw;
    @Mutable
    float savedPitch;

    @Inject(method = "travel", at = @At(value = "HEAD"))
    public void fixElytra(CallbackInfo ci) {
        if (((Object) this) instanceof ClientPlayerEntity player) {
            Rotate currentRotate = Luxury.getInstance().getRotationManager().getCurrentRotate();
            savedYaw = mc.player.getYaw();
            savedPitch = mc.player.getPitch();
            player.setYaw(currentRotate.getYaw());
            player.setPitch(currentRotate.getPitch());
        }

    }

    @Inject(method = "travel", at = @At(value = "RETURN"))
    public void fixElytraEnd(CallbackInfo ci) {
        if (((Object) this) instanceof ClientPlayerEntity player) {

            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
        }

    }
}