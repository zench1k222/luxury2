package dev.luxury.mixin.render.impl.client;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.KillAura;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Unique private float luxury$storedPitch;
    @Unique private float luxury$storedPrevPitch;
    @Unique private float luxury$storedBodyYaw;
    @Unique private float luxury$storedPrevBodyYaw;
    @Unique private float luxury$storedHeadYaw;
    @Unique private float luxury$storedPrevHeadYaw;

    @Unique private float luxury$smoothBodyPitch1 = 0;
    @Unique private float luxury$smoothBodyPitch2 = 0;
    @Unique private float luxury$smoothBodyYaw1 = 0;
    @Unique private float luxury$smoothBodyYaw2 = 0;

    @Unique private float luxury$smoothHeadPitch1 = 0;
    @Unique private float luxury$smoothHeadPitch2 = 0;
    @Unique private float luxury$smoothHeadYaw1 = 0;
    @Unique private float luxury$smoothHeadYaw2 = 0;

    @Unique private final float[] luxury$bodyPitchHistory = new float[5];
    @Unique private final float[] luxury$bodyYawHistory = new float[5];
    @Unique private final float[] luxury$headPitchHistory = new float[5];
    @Unique private final float[] luxury$headYawHistory = new float[5];
    @Unique private int luxury$historyIndex = 0;

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void beforeRenderEntity(Entity entity, double x, double y, double z, float tickDelta,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity player) {
            KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

            if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {

                luxury$storedPitch = player.getPitch();
                luxury$storedPrevPitch = player.prevPitch;
                luxury$storedBodyYaw = player.bodyYaw;
                luxury$storedPrevBodyYaw = player.prevBodyYaw;
                luxury$storedHeadYaw = player.headYaw;
                luxury$storedPrevHeadYaw = player.prevHeadYaw;

                float baseBodyYaw = MathHelper.lerpAngleDegrees(tickDelta, killAura.getPrevBodyYaw(), killAura.getBodyYaw());
                float baseBodyPitch = MathHelper.lerp(tickDelta, killAura.getPrevBodyPitch(), killAura.getBodyPitch());

                float baseHeadYaw = MathHelper.lerpAngleDegrees(tickDelta, killAura.getPrevHeadYaw(), killAura.getHeadYaw());
                float baseHeadPitch = MathHelper.lerp(tickDelta, killAura.getPrevHeadPitch(), killAura.getHeadPitch());

                if (luxury$historyIndex == 0) {
                    for (int i = 0; i < luxury$bodyPitchHistory.length; i++) {
                        luxury$bodyPitchHistory[i] = baseBodyPitch;
                        luxury$bodyYawHistory[i] = baseBodyYaw;
                        luxury$headPitchHistory[i] = baseHeadPitch;
                        luxury$headYawHistory[i] = baseHeadYaw;
                    }
                    luxury$smoothBodyPitch1 = baseBodyPitch;
                    luxury$smoothBodyPitch2 = baseBodyPitch;
                    luxury$smoothBodyYaw1 = baseBodyYaw;
                    luxury$smoothBodyYaw2 = baseBodyYaw;
                    luxury$smoothHeadPitch1 = baseHeadPitch;
                    luxury$smoothHeadPitch2 = baseHeadPitch;
                    luxury$smoothHeadYaw1 = baseHeadYaw;
                    luxury$smoothHeadYaw2 = baseHeadYaw;
                }

                luxury$bodyPitchHistory[luxury$historyIndex % 5] = baseBodyPitch;
                luxury$bodyYawHistory[luxury$historyIndex % 5] = baseBodyYaw;
                luxury$headPitchHistory[luxury$historyIndex % 5] = baseHeadPitch;
                luxury$headYawHistory[luxury$historyIndex % 5] = baseHeadYaw;
                luxury$historyIndex++;

                float avgBodyPitch = calculateAverage(luxury$bodyPitchHistory);
                float avgBodyYaw = calculateAngleAverage(luxury$bodyYawHistory, baseBodyYaw);

                float avgHeadPitch = calculateAverage(luxury$headPitchHistory);
                float avgHeadYaw = calculateAngleAverage(luxury$headYawHistory, baseHeadYaw);

                float smoothFactor1 = 0.15f;
                luxury$smoothBodyPitch1 = MathHelper.lerp(smoothFactor1, luxury$smoothBodyPitch1, avgBodyPitch);
                luxury$smoothBodyYaw1 = lerpAngleSmoothly(luxury$smoothBodyYaw1, avgBodyYaw, smoothFactor1);

                float smoothFactor2 = 0.3f;
                luxury$smoothBodyPitch2 = MathHelper.lerp(smoothFactor2, luxury$smoothBodyPitch2, luxury$smoothBodyPitch1);
                luxury$smoothBodyYaw2 = lerpAngleSmoothly(luxury$smoothBodyYaw2, luxury$smoothBodyYaw1, smoothFactor2);

                luxury$smoothHeadPitch1 = MathHelper.lerp(smoothFactor1, luxury$smoothHeadPitch1, avgHeadPitch);
                luxury$smoothHeadYaw1 = lerpAngleSmoothly(luxury$smoothHeadYaw1, avgHeadYaw, smoothFactor1);

                luxury$smoothHeadPitch2 = MathHelper.lerp(smoothFactor2, luxury$smoothHeadPitch2, luxury$smoothHeadPitch1);
                luxury$smoothHeadYaw2 = lerpAngleSmoothly(luxury$smoothHeadYaw2, luxury$smoothHeadYaw1, smoothFactor2);

                ((EntityAccessor) player).setPitchField(luxury$smoothHeadPitch2);
                player.prevPitch = luxury$smoothHeadPitch2;

                player.bodyYaw = luxury$smoothBodyYaw2;
                player.prevBodyYaw = luxury$smoothBodyYaw2;

                player.headYaw = luxury$smoothHeadYaw2;
                player.prevHeadYaw = luxury$smoothHeadYaw2;
            }
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void afterRenderEntity(Entity entity, double x, double y, double z, float tickDelta,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity player) {
            KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

            if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {
                ((EntityAccessor) player).setPitchField(luxury$storedPitch);
                player.prevPitch = luxury$storedPrevPitch;
                player.bodyYaw = luxury$storedBodyYaw;
                player.prevBodyYaw = luxury$storedPrevBodyYaw;
                player.headYaw = luxury$storedHeadYaw;
                player.prevHeadYaw = luxury$storedPrevHeadYaw;
            } else {
                luxury$historyIndex = 0;
            }
        }
    }

    @Unique
    private float lerpAngleSmoothly(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * factor;
    }

    @Unique
    private float calculateAverage(float[] values) {
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    @Unique
    private float calculateAngleAverage(float[] angles, float reference) {
        float sumSin = 0;
        float sumCos = 0;
        for (float angle : angles) {
            double rad = Math.toRadians(angle);
            sumSin += Math.sin(rad);
            sumCos += Math.cos(rad);
        }
        return (float) Math.toDegrees(Math.atan2(sumSin / angles.length, sumCos / angles.length));
    }
}