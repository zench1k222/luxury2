package dev.luxury.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@UtilityClass
public class RotationUtil {
    public static Vec2f get(Vec3d from, Vec3d target) {
        Vec3d diff = target.subtract(from);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontalDist));
        
        return new Vec2f(yaw, pitch);
    }
}

