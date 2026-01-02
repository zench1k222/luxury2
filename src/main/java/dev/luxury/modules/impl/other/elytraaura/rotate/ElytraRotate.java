package dev.luxury.modules.impl.other.elytraaura.rotate;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Getter
public class ElytraRotate {
    private final float yaw;
    private final float pitch;
    private final boolean normalized;

    public ElytraRotate(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public ElytraRotate(float yaw, float pitch, boolean normalized) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.normalized = normalized;
    }

    public static ElytraRotate lookingAt(Vec3d point, Vec3d from) {
        Vec3d diff = point.subtract(from);
        return fromVector(diff);
    }

    public static ElytraRotate fromVector(Vec3d lookVec) {
        double dx = lookVec.x;
        double dy = lookVec.y;
        double dz = lookVec.z;
        return new ElytraRotate(
                (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90f),
                (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))),
                true
        );
    }

    public Vec3d toVector() {
        return Vec3d.fromPolar(pitch, yaw);
    }

    public ElytraRotate normalize(ElytraRotate current) {
        if (normalized || this.equals(current)) return this;
        float dy = MathHelper.wrapDegrees(this.yaw - current.yaw);
        float dp = this.pitch - current.pitch;
        float gcd = gcd();
        int ty = (int) (dy / gcd);
        int tp = (int) (dp / gcd);
        return new ElytraRotate(current.yaw + ty * gcd, current.pitch + tp * gcd, true);
    }

    public static float gcd() {
        double f = net.minecraft.client.MinecraftClient.getInstance().options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
        return (float) (f * f * f * 8.0 * 0.15f);
    }
}

