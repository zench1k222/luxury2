package dev.luxury.modules.impl.killaura.rotate.mods;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.mods.api.RotationMode;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class ReallyWorldMode extends RotationMode {
    private final Random random = new Random();

    public Rotate process(Rotate target) {
        Rotate current = Luxury.getInstance().getRotationManager().getCurrentRotate();
        Rotate toTarget = current.add(current.rotationDeltaTo(target));

        float jitterYaw = (float) (randomLerp(3f, 6f) * Math.sin(System.currentTimeMillis() / 15.0));
        float jitterPitch = (float) (randomLerp(2f, 5f) * Math.sin(System.currentTimeMillis() / 17.0));

        float finalYaw = toTarget.getYaw() + jitterYaw;
        float finalPitch = MathHelper.clamp(toTarget.getPitch() + jitterPitch, -90.0f, 90.0f);

        finalYaw = MathHelper.wrapDegrees(finalYaw);

        Rotate finalRotation = new Rotate(finalYaw, finalPitch);

        Luxury.getInstance().getRotationManager().setBodyRotation(toTarget);

        return finalRotation;
    }

    private float randomLerp(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}