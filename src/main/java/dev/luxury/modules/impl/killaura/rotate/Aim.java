package dev.luxury.modules.impl.killaura.rotate;


import dev.luxury.Luxury;
import dev.luxury.modules.impl.killaura.rotate.mods.AiMode;
import dev.luxury.modules.impl.killaura.rotate.mods.OrdinaryMode;
import dev.luxury.modules.impl.killaura.rotate.mods.Interpolation;
import dev.luxury.modules.impl.killaura.rotate.mods.config.AiConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.OrdinaryConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.InterpolationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationModeType;
import lombok.Getter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class Aim {
MinecraftClient mc = MinecraftClient.getInstance();
    private final Interpolation interpolationMod = new Interpolation();
    private final OrdinaryMode instantMod = new OrdinaryMode();
    private final AiMode smoothMod = new AiMode();
    @Getter
    private final RotationConfig instantSetup = new OrdinaryConfig();
    @Getter
    private final RotationConfig aiSetup = AiConfig.builder().build();


    public Rotate rotate(RotationConfig config, Rotate targetRotate) {
        if (config.getType() != RotationModeType.INSTANT) {
            DeltaRotate deltaToTarget = Luxury.getInstance().getRotationManager().getCurrentRotate().rotationDeltaTo(targetRotate);
            float maxInitialDiff = 270f; // 180 + 90
            float progress = MathHelper.clamp(1f - (Math.abs(deltaToTarget.getDeltaYaw()) + Math.abs(deltaToTarget.getDeltaPitch())) / maxInitialDiff, 0, 1);

            DeltaRotate offsets = getOffset(deltaToTarget, progress);

            targetRotate = targetRotate.add(offsets);
        }

        Rotate newRotate;

        switch (config.getType()) {
            case INSTANT -> newRotate = instantMod.process(targetRotate);
            case INTERPOLATION -> newRotate = interpolationMod.process((InterpolationConfig) config,Luxury.getInstance().getRotationManager().getCurrentRotate(), targetRotate);
            case AI -> newRotate = interpolationMod.process(((AiConfig) config ).getInterpolationConfig() ,smoothMod.process((AiConfig) config , targetRotate), targetRotate);
            default -> newRotate = Luxury.getInstance().getRotationManager().getCurrentRotate();
        }

        if(config.getType()!= RotationModeType.AI) {
           // smoothMod.resetLerp(targetRotation);
        }
        if(Luxury.getInstance().getRotationManager().getCurrentRotate().equals(newRotate)) {
            return newRotate;
        }
          return newRotate.normalize(new Rotate(mc.player.lastYaw,mc.player.lastPitch));
    }

    @Getter
    int index = 0;

    public void incrementIndex() {
        index++;
        if (index >= values.length) {
            index = 0;
        }
    }

    public float getDiff(float prevTargetYawDiff, float targetYawDiff) {


        return targetYawDiff * values[index];
    }


    float[] values = new float[]{
            0.0123967f,
            0.053719f,
            0.109504f,
            0.17562f,
            0.21281f,
            0.272727f,
            0.11157f,
            0.0392562f,
            0.0103306f,
            0.00206612f
    };

    public static DeltaRotate getOffset(DeltaRotate deltaToTarget, float progress) {
        float curveStrength = 3.0f;
        float smoothness = 1.0f - (float) Math.cos(progress * Math.PI);

        float yawSign = Math.signum(deltaToTarget.getDeltaYaw());
        float pitchSign = Math.signum(deltaToTarget.getDeltaPitch());

        float offsetYaw = 0f;
        float offsetPitch = 0f;

        boolean verticalAiming = Math.abs(deltaToTarget.getDeltaYaw()) < 1.5f && Math.abs(deltaToTarget.getDeltaPitch()) > 10f;

        if (verticalAiming) {
            offsetYaw = pitchSign * curveStrength * (1f - progress);
            offsetPitch = 0f;
        } else {

            if (pitchSign > 0 && yawSign >= 0) {
                offsetYaw = -curveStrength * (1f - progress);
                offsetPitch = -curveStrength * smoothness;
            } else if (pitchSign > 0 && yawSign < 0) {
                offsetYaw = curveStrength * (1f - progress);
                offsetPitch = -curveStrength * smoothness;
            } else if (pitchSign < 0 && yawSign >= 0) {
                offsetYaw = -curveStrength * (1f - progress);
                offsetPitch = curveStrength * smoothness;
            } else if (pitchSign < 0 && yawSign < 0) {
                offsetYaw = curveStrength * (1f - progress);
                offsetPitch = curveStrength * smoothness;
            }
        }

        return new DeltaRotate(offsetYaw, offsetPitch);
    }

    public void reset() {
        this.index = 0;
    }


}
