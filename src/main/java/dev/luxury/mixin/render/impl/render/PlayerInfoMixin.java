package dev.luxury.mixin.render.impl.render;

import com.mojang.authlib.GameProfile;
import dev.luxury.utils.render.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.util.Constants;

import java.util.function.Supplier;
@Mixin(PlayerListEntry.class)
public class PlayerInfoMixin {
    @Unique
    private final static Identifier CAPE = Identifier.of("luxury", "images/cape.png");

    @Inject(method = "texturesSupplier", at = @At("RETURN"), cancellable = true)
    private static void textureSupplier(GameProfile gameProfile, CallbackInfoReturnable<Supplier<SkinTextures>> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean bl = mc.getSession().getUuidOrNull() != null && mc.getSession().getUuidOrNull().equals(gameProfile.getId());

        if (bl) {
            SkinTextures playerSkin = cir.getReturnValue().get();
            SkinTextures newPlayerSkin = new SkinTextures(
                    playerSkin.texture(),
                    playerSkin.textureUrl(),
                    CAPE,
                    playerSkin.elytraTexture(),
                    playerSkin.model(),
                    playerSkin.secure()
            );
            cir.setReturnValue(() -> newPlayerSkin);
        }
    }
}