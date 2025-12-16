package dev.luxury.modules.impl.killaura.rotate;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class RotateUtils {
    MinecraftClient mc = MinecraftClient.getInstance();
    public Rotate getClientRotation() {
        return new Rotate(mc.player.getYaw(), mc.player.getPitch());
    }
    public Rotate fromVec3d(Vec3d vector) {
        return new Rotate((float) wrapDegrees(toDegrees(Math.atan2(vector.z, vector.x)) - 90), (float) wrapDegrees(toDegrees(-Math.atan2(vector.y, hypot(vector.x, vector.z)))));
    }
    public Rotate calculateAngle(Vec3d to) {
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

    public static float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

}
