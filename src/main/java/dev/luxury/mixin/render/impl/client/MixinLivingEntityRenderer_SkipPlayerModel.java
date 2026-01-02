package dev.luxury.mixin.render.impl.client;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.render.CustomModels;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class MixinLivingEntityRenderer_SkipPlayerModel {

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL"), require = 0)
    private void hidePlayerModel(PlayerEntityRenderState state, CallbackInfo ci) {
        CustomModels cm = ModuleManager.getModule(CustomModels.class);
        PlayerEntityModel self = (PlayerEntityModel) (Object) this;

        if (cm != null && cm.isEnabled() && shouldHideModel(state)) {
            self.setVisible(false);
        } else {
            self.setVisible(true);
        }
    }
    private boolean shouldHideModel(PlayerEntityRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        String playerName = getPlayerNameFromState(state);

        if (playerName == null) {
            return isLocalPlayer(state);
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