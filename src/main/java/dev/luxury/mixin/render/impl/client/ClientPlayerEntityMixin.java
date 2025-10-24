package dev.luxury.mixin.render.impl.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.KillAura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Shadow @Final
    protected MinecraftClient client;

    @Unique private float storedYaw;
    @Unique private float storedPitch;

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"
            )
    )
    public boolean unpressSprintKey(boolean original) {
        KillAura killAura = (KillAura)ModuleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.shouldStopSprinting()) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"
            )
    )
    private boolean disallowSprinting(boolean original) {
        KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.shouldStopSprinting()) {
            return false;
        }
        return original;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHead(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

        if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {
            storedYaw = player.getYaw();
            storedPitch = player.getPitch();

            player.setYaw(killAura.getBodyYaw());
            player.setPitch(killAura.getBodyPitch());
        }
    }

    @Inject(method = "tickNewAi", at = @At("HEAD"))
    public void tickNewAiHead(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

        if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {
            player.setYaw(storedYaw);
            player.setPitch(storedPitch);
        }
    }

    @Inject(method = "tickNewAi", at = @At("RETURN"))
    public void tickNewAiReturn(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

        if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {
            player.setYaw(killAura.getHeadYaw());
            player.setPitch(killAura.getHeadPitch());
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickReturn(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

        if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {
            player.setYaw(storedYaw);
            player.setPitch(storedPitch);
        }
    }
}