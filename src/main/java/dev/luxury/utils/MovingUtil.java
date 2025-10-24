package dev.luxury.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;

import java.util.Objects;
import java.util.stream.Stream;

public class MovingUtil {
   private static MinecraftClient mc = MinecraftClient.getInstance();
    @Getter
    @Setter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

    public static class DirectionalInput {
        boolean forwards;
        boolean backwards;
        boolean left;
        boolean right;

        public DirectionalInput(Input input) {
            this(input.playerInput.forward(), input.playerInput.backward(),
                    input.playerInput.left(), input.playerInput.right());
        }

        public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);
        public static final DirectionalInput FORWARDS = new DirectionalInput(true, false, false, false);
        public static final DirectionalInput BACKWARDS = new DirectionalInput(false, true, false, false);
        public static final DirectionalInput LEFT = new DirectionalInput(false, false, true, false);
        public static final DirectionalInput RIGHT = new DirectionalInput(false, false, false, true);

        public static float getMovementMultiplier(boolean positive, boolean negative) {
            if (positive == negative) {
                return 0.0F;
            } else {
                return positive ? 1.0F : -1.0F;
            }
        }
    }

    public static KeyBinding[] getMovementKeys(boolean includeSneak) {
        return Stream.of(
                        mc.options.forwardKey,
                        mc.options.backKey,
                        mc.options.leftKey,
                        mc.options.rightKey,
                        mc.options.jumpKey,
                        mc.options.sprintKey,
                        includeSneak ? mc.options.sneakKey : null
                ).filter(Objects::nonNull)
                .toArray(KeyBinding[]::new);
    }

    public static boolean hasPlayerMovement() {
        return mc.player.input.movementForward != 0f || mc.player.input.movementSideways != 0f;
    }

    public static boolean isMoving() {
        assert mc.player != null;
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    public static double[] calculateDirection(final double distance) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }

    public static void setVelocity(float velocity, float y) {
        final double[] direction = MovingUtil.calculateDirection(velocity);
        mc.player.setVelocity(direction[0], y, direction[1]);
    }

    public static double[] forward(final double d) {
        assert mc.player != null;
        float f = mc.player.input.movementForward;
        float f2 = mc.player.input.movementSideways;
        float f3 = mc.player.getYaw();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }
}
