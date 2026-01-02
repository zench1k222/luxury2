package dev.luxury.modules.impl.other.killaura.rotate;


import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


@Getter
public class Rotate {
    private final float yaw;
    private final float pitch;
    private boolean isNormalized;

    public static final Rotate ZERO = new Rotate(0f, 0f);

    public Rotate(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotate(float yaw, float pitch, boolean isNormalized) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.isNormalized = isNormalized;
    }

    public static Rotate lookingAt(Vec3d point, Vec3d from) {
        return fromRotationVec(point.subtract(from));
    }

    public static Rotate fromRotationVec(Vec3d lookVec) {
        double diffX = lookVec.x;
        double diffY = lookVec.y;
        double diffZ = lookVec.z;

        return new Rotate(
                (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f),
                (float) MathHelper.wrapDegrees((-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))))
        );
    }


    public float angleTo(Rotate other) {
        return Math.min(rotationDeltaTo(other).length(), 180.0f);
    }

    public DeltaRotate rotationDeltaTo(Rotate other) {
        return new DeltaRotate(
                angleDifference(other.yaw, this.yaw),
                angleDifference(other.pitch, this.pitch)
        );
    }


    private float angleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }


    public boolean approximatelyEquals(Rotate other, float tolerance) {
        return angleTo(other) <= tolerance;
    }

    public boolean isNormalized() {
        return isNormalized;
    }

    public Vec3d getDirectionVector() {
        return Vec3d.fromPolar(this.pitch,this.yaw);
    }
    public final Vec3d toVector() {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
    public Rotate towardsLinear(Rotate other, float horizontalFactor, float verticalFactor) {
        DeltaRotate diff = this.rotationDeltaTo(other);
        float rotationDifference = diff.length();

        float straightLineYaw = Math.abs(diff.getDeltaYaw() / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.getDeltaPitch() / rotationDifference) * verticalFactor;

        float limitedYaw = MathHelper.clamp(diff.getDeltaYaw(), -straightLineYaw, straightLineYaw);
        float limitedPitch = MathHelper. clamp(diff.getDeltaPitch(), -straightLinePitch, straightLinePitch);

        return new Rotate(this.yaw + limitedYaw, this.pitch + limitedPitch);
    }
    public boolean check(){
        return  (Float.isInfinite(yaw) || Float.isNaN(yaw) || Float.isInfinite(pitch) || Float.isNaN(pitch));
    }
    public static float gcd() {
        double f = MinecraftClient.getInstance().options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
        return (float) (f * f * f * 8.0*0.15f );
    }
    public Rotate normalize(Rotate currentRotate) {
        if (isNormalized ||this.equals(currentRotate)) return this;
        DeltaRotate deltaRotate = currentRotate.rotationDeltaTo(this);
        double gcd = gcd();
        int targetX = (int) (deltaRotate.getDeltaYaw()/(gcd));
        int targetY = (int) (deltaRotate.getDeltaPitch()/(gcd));


        return new Rotate((float) (currentRotate.getYaw()+(targetX*gcd)), (float) (currentRotate.getPitch()+(targetY*gcd)), true);
    }

    public Rotate add(DeltaRotate diff) {
        return new Rotate(this.yaw+diff.getDeltaYaw(), this.pitch+diff.getDeltaPitch());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Rotate o2) return o2.yaw == this.yaw && o2.pitch == this.pitch;
        else return false;
    }
}
