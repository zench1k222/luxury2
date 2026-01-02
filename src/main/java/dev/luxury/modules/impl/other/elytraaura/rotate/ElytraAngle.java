package dev.luxury.modules.impl.other.elytraaura.rotate;

import lombok.Data;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Data
public class ElytraAngle {
    private float yaw;
    private float pitch;

    public ElytraAngle(float yaw, float pitch) {
        this.yaw = Float.isNaN(yaw) ? 0 : yaw;
        this.pitch = Float.isNaN(pitch) ? 0 : pitch;
    }

    public ElytraAngle(Vec3d diff) {
        double distance = Math.hypot(diff.x, diff.z);
        this.yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f);
        this.pitch = (float) MathHelper.wrapDegrees((-Math.toDegrees(Math.atan2(diff.y, distance))));
    }

    public Vec3d toVector() {
        return Vec3d.fromPolar(pitch, yaw);
    }

    public ElytraAngle delta(ElytraAngle other) {
        float dy = MathHelper.wrapDegrees(other.yaw - this.yaw);
        float dp = other.pitch - this.pitch;
        return new ElytraAngle(dy, MathHelper.clamp(dp, -90, 90));
    }

    public float length() {
        ElytraAngle d = delta(new ElytraAngle(0, 0));
        return MathHelper.sqrt(d.yaw * d.yaw + d.pitch * d.pitch);
    }
}

