package dev.luxury.mixin.render.impl.client;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.CustomModels;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer_Skip {

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void skipArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        CustomModels cm = ModuleManager.getModule(CustomModels.class);

        if (cm != null && cm.isEnabled() && shouldHideArmor(state)) {
            ci.cancel();
        }
    }

    private boolean shouldHideArmor(BipedEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        String playerName = getPlayerNameFromState(playerState);

        if (playerName == null) {
            return isLocalPlayer(playerState);
        }

        String localPlayerName = mc.player.getName().getString();
        return playerName.equalsIgnoreCase(localPlayerName) ||
                FriendManager.getInstance().isFriend(playerName);
    }

    private String getPlayerNameFromState(PlayerEntityRenderState state) {
        try {
            if (state.name != null) {
                return state.name;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean isLocalPlayer(PlayerEntityRenderState state) {
        return false;
    }
}