package dev.luxury.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.Math;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
@UtilityClass
public class MathUtil {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static double PI2 = Math.PI * 2;
    public static double roundToDecimal(double n, int point) {
    if (point == 0) {
        return Math.floor(n);
    }
    double factor = Math.pow(10, point);
    return Math.round(n * factor) / factor;
}

    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();


    public static boolean isHovered(double x,
                                    double y,
                                    double width,
                                    double height,
                                    double mouseX,
                                    double mouseY) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }

    public static float lerp(float end, float start, float multiple) {
        return (float) (end + (start - end) * MathHelper.clamp(deltaTime() * multiple, 0, 1));
    }
    private static double deltaTime() {
        return mc.getCurrentFps() > 0 ? (1.0000 / mc.getCurrentFps()) : 1;
    }

    public static double round(double num, double increment) {
        double v = (double) Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                double d = min;
                min = max;
                max = d;
            }

            return ThreadLocalRandom.current().nextDouble(min, max);
        }
    }
    public Vec3d interpolate(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(interpolate(entity.prevX, entity.getX()), interpolate(entity.prevY, entity.getY()), interpolate(entity.prevZ, entity.getZ()));
    }
    public static float getPow2Value(float value) {
        return value * value;
    }

    public static float[] colorToArray(int hex) {
        float[] rgba = new float[4];

        rgba[0] = getRed(hex) / 255f;
        rgba[1] = getGreen(hex) / 255f;
        rgba[2] = getBlue(hex) / 255f;
        rgba[3] = getAlpha(hex) / 255f;

        return rgba;
    }


    public static @NotNull Vec3d projectCoordinates(@NotNull Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (displayHeight - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }

    public static float random(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float randomFloat(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }


    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color interpolateColor(Color startColor, Color endColor, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));

        int r = (int) (startColor.getRed() + ratio * (endColor.getRed() - startColor.getRed()));
        int g = (int) (startColor.getGreen() + ratio * (endColor.getGreen() - startColor.getGreen()));
        int b = (int) (startColor.getBlue() + ratio * (endColor.getBlue() - startColor.getBlue()));

        return new Color(r, g, b);
    }
    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static void scale(MatrixStack stack,
                             float x,
                             float y,
                             float scale,
                             Runnable data) {

        stack.push();
        stack.translate(x, y, 0);
        stack.scale(scale, scale, 1);
        stack.translate(-x, -y, 0);
        data.run();
        stack.pop();
    }

    public static double computeGcd() {
        double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15f;
    }

    public static double interpolate(double previous, double current) {
        return previous + (current - previous) * getTickDelta();
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static int getRed(int hex) {
        return hex >> 16 & 255;
    }

    public static int getGreen(int hex) {
        return hex >> 8 & 255;
    }

    public static int getBlue(int hex) {
        return hex & 255;
    }

    public static int getAlpha(int hex) {
        return hex >> 24 & 255;
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static float centerX(float containerX, float containerWidth, float elementWidth) {
        return containerX + (containerWidth - elementWidth) / 2;
    }

    public static float centerY(float containerY, float containerHeight, float elementHeight) {
        return containerY + (containerHeight - elementHeight) / 2;
    }
}
