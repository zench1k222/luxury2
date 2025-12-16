package dev.luxury.mixin.render.impl.client;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.NoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (packet.getEntityId() != mc.player.getId()) return;

        NoPush noPush = ModuleManager.getModule(NoPush.class);
        if (noPush == null || !noPush.isEnabled()) return;

        if (noPush.mods.getValueByName("Удочка").get()) {
            boolean hasFishingBobber = mc.world.getEntities()
                    .spliterator()
                    .estimateSize() > 0 &&
                    streamEntities(mc).anyMatch(e -> e instanceof FishingBobberEntity bobber && bobber.getHookedEntity() == mc.player);

            if (hasFishingBobber) {
                ci.cancel();
            }
        }
    }

    private java.util.stream.Stream<Entity> streamEntities(MinecraftClient mc) {
        return java.util.stream.StreamSupport.stream(
                mc.world.getEntities().spliterator(), false);
    }
}