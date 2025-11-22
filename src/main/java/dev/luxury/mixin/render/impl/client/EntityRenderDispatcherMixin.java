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

    @Unique private float storedPitch;
    @Unique private float storedPrevPitch;
    @Unique private float storedBodyYaw;
    @Unique private float storedPrevBodyYaw;
    @Unique private float storedHeadYaw;
    @Unique private float storedPrevHeadYaw;

    @Unique private float smoothBodyPitch1 = 0;
    @Unique private float smoothBodyPitch2 = 0;
    @Unique private float smoothBodyYaw1 = 0;
    @Unique private float smoothBodyYaw2 = 0;

    @Unique private float smoothHeadPitch1 = 0;
    @Unique private float smoothHeadPitch2 = 0;
    @Unique private float smoothHeadYaw1 = 0;
    @Unique private float smoothHeadYaw2 = 0;

    @Unique private final float[] bodyPitchHistory = new float[5];
    @Unique private final float[] bodyYawHistory = new float[5];
    @Unique private final float[] headPitchHistory = new float[5];
    @Unique private final float[] headYawHistory = new float[5];
    @Unique private int historyIndex = 0;

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void beforeRenderEntity(Entity entity, double x, double y, double z, float tickDelta,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity player) {
            KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

            if (killAura != null && killAura.isEnabled() && killAura.hasTarget()) {

                storedPitch = player.getPitch();
                storedPrevPitch = player.prevPitch;
                storedBodyYaw = player.bodyYaw;
                storedPrevBodyYaw = player.prevBodyYaw;
                storedHeadYaw = player.headYaw;
                storedPrevHeadYaw = player.prevHeadYaw;

                float baseBodyYaw = MathHelper.lerpAngleDegrees(tickDelta, killAura.getPrevBodyYaw(), killAura.getBodyYaw());
                float baseBodyPitch = MathHelper.lerp(tickDelta, killAura.getPrevBodyPitch(), killAura.getBodyPitch());

                float baseHeadYaw = MathHelper.lerpAngleDegrees(tickDelta, killAura.getPrevHeadYaw(), killAura.getHeadYaw());
                float baseHeadPitch = MathHelper.lerp(tickDelta, killAura.getPrevHeadPitch(), killAura.getHeadPitch());

                if (historyIndex == 0) {
                    for (int i = 0; i < bodyPitchHistory.length; i++) {
                        bodyPitchHistory[i] = baseBodyPitch;
                        bodyYawHistory[i] = baseBodyYaw;
                        headPitchHistory[i] = baseHeadPitch;
                        headYawHistory[i] = baseHeadYaw;
                    }
                    smoothBodyPitch1 = baseBodyPitch;
                    smoothBodyPitch2 = baseBodyPitch;
                    smoothBodyYaw1 = baseBodyYaw;
                    smoothBodyYaw2 = baseBodyYaw;
                    smoothHeadPitch1 = baseHeadPitch;
                    smoothHeadPitch2 = baseHeadPitch;
                    smoothHeadYaw1 = baseHeadYaw;
                    smoothHeadYaw2 = baseHeadYaw;
                }

                bodyPitchHistory[historyIndex % 5] = baseBodyPitch;
                bodyYawHistory[historyIndex % 5] = baseBodyYaw;
                headPitchHistory[historyIndex % 5] = baseHeadPitch;
                headYawHistory[historyIndex % 5] = baseHeadYaw;
                historyIndex++;

                float avgBodyPitch = calculateAverage(bodyPitchHistory);
                float avgBodyYaw = calculateAngleAverage(bodyYawHistory, baseBodyYaw);

                float avgHeadPitch = calculateAverage(headPitchHistory);
                float avgHeadYaw = calculateAngleAverage(headYawHistory, baseHeadYaw);

                float smoothFactor1 = 0.15f;
                smoothBodyPitch1 = MathHelper.lerp(smoothFactor1, smoothBodyPitch1, avgBodyPitch);
                smoothBodyYaw1 = lerpAngleSmoothly(smoothBodyYaw1, avgBodyYaw, smoothFactor1);

                float smoothFactor2 = 0.3f;
                smoothBodyPitch2 = MathHelper.lerp(smoothFactor2, smoothBodyPitch2, smoothBodyPitch1);
                smoothBodyYaw2 = lerpAngleSmoothly(smoothBodyYaw2, smoothBodyYaw1, smoothFactor2);

                smoothHeadPitch1 = MathHelper.lerp(smoothFactor1, smoothHeadPitch1, avgHeadPitch);
                smoothHeadYaw1 = lerpAngleSmoothly(smoothHeadYaw1, avgHeadYaw, smoothFactor1);

                smoothHeadPitch2 = MathHelper.lerp(smoothFactor2, smoothHeadPitch2, smoothHeadPitch1);
               smoothHeadYaw2 = lerpAngleSmoothly(smoothHeadYaw2, smoothHeadYaw1, smoothFactor2);

                ((EntityAccessor) player).setPitchField(smoothHeadPitch2);
                player.prevPitch = smoothHeadPitch2;

                player.bodyYaw = smoothBodyYaw2;
                player.prevBodyYaw = smoothBodyYaw2;

                player.headYaw = smoothHeadYaw2;
                player.prevHeadYaw = smoothHeadYaw2;
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
                ((EntityAccessor) player).setPitchField(storedPitch);
                player.prevPitch = storedPrevPitch;
                player.bodyYaw = storedBodyYaw;
                player.prevBodyYaw = storedPrevBodyYaw;
                player.headYaw = storedHeadYaw;
                player.prevHeadYaw = storedPrevHeadYaw;
            } else {
                historyIndex = 0;
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