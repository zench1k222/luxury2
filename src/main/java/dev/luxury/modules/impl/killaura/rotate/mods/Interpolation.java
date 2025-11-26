package dev.luxury.modules.impl.killaura.rotate.mods;


import dev.luxury.Luxury;
import dev.luxury.modules.impl.killaura.rotate.mods.api.RotationMode;
import dev.luxury.modules.impl.killaura.rotate.mods.config.InterpolationConfig;
import dev.luxury.utils.math.IntRange;
import kotlin.Pair;
import net.minecraft.util.math.MathHelper;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.DeltaRotate;

import java.util.Random;

public class Interpolation extends RotationMode {


    public Rotate process(InterpolationConfig config, Rotate modelOut, Rotate targetRotate
    ) {

        Pair<Float, Float> pair = calculateFactors(Luxury.getInstance().getRotationManager().getCurrentRotate(), targetRotate,config.getHorizontalSpeedSetting(),config.getVerticalSpeedSetting(),config.getDirectionChangeFactor(),config.getMidPoint());
        return modelOut.towardsLinear(targetRotate, pair.getFirst(), pair.getSecond());
    }

    private final Sigmoid sigmoid = new Sigmoid();
    private final Bezier bezier = new Bezier();
    private final Random random = new Random();


    public Pair<Float, Float> calculateFactors(Rotate currentRotate, Rotate targetRotate, IntRange horizontalSpeedSetting, IntRange verticalSpeedSetting, IntRange directionChangeFactor, float midpoint) {

        DeltaRotate diff = currentRotate.rotationDeltaTo(targetRotate);
        float yawDiff = diff.getDeltaYaw();
        float pitchDiff = diff.getDeltaPitch();


        float directionChange = 0f;
        if (targetRotate != null && Luxury.getInstance().getRotationManager().getPreviousTargetRotate() != null) {
            directionChange = Luxury.getInstance().getRotationManager().getPreviousTargetRotate().targetRotate()
                    .angleTo(targetRotate);
            directionChange = MathHelper.clamp(directionChange, 0f, 1f);
            directionChange *= directionChangeFactor.random() / 100.0f;
        }

        float horizontalSpeed = (targetRotate != null ? horizontalSpeedSetting.random() : horizontalSpeedSetting.getStart()) / 100.0f;
        float verticalSpeed = (targetRotate != null ? verticalSpeedSetting.random() : verticalSpeedSetting.getStart()) / 100.0f;


        float horizontalFactor = calculateFactor("Yaw", Math.abs(yawDiff), MathHelper.clamp(horizontalSpeed, 0f, 1f), directionChange,midpoint);
        float verticalFactor = calculateFactor("Pitch", Math.abs(pitchDiff), MathHelper.clamp(verticalSpeed, 0f, 1f), directionChange,midpoint);

        return new Pair<>(horizontalFactor * Math.abs(yawDiff), verticalFactor * Math.abs(pitchDiff));
    }

    private float calculateFactor(String name, float rotationDifference, float turnSpeed, float directionChange,float midpoint) {
        float t = MathHelper.clamp(rotationDifference / 180f, 0f, 1f);

        float bezierSpeed = bezier.transform(0.05f, 1f, 1f - t);
        float sigmoidSpeed = sigmoid.transform(t);


        if (t > midpoint) {

            return bezierSpeed * turnSpeed;
        } else {
            return sigmoidSpeed * MathHelper.clamp(turnSpeed + directionChange, 0f, 1f);
        }
    }

    private static class Sigmoid {
        public float transform(float t) {
            return (float) (1.0 / (1.0 + Math.exp(-0.5 * (t - 0.3))));
        }
    }

    private static class Bezier {
        public float transform(float start, float end, float t) {
            return (1 - t) * (1 - t) * start + 2 * (1 - t) * t * 1.0f + t * t * end;
        }
    }
}
