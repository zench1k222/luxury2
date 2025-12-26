package dev.luxury.modules.impl.targetesp.mode;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4i;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Crystals {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier BLOOM_TEXTURE = Identifier.of("luxury", "images/bloom.png");
    private static Entity lastRenderedTarget = null;
    private static final List<CrystalData> crystalList = new ArrayList<>();
    private static float globalRotation = 0;

    private static class CrystalData {
        private final Vec3d offset;
        private final float seed1, seed2, seed3;
        private final float sizeMultiplier;

        public CrystalData(float x, float y, float z, int index) {
            this.offset = new Vec3d(x, y, z);
            this.seed1 = (float) Math.sin(index * 1.7f + 0.3f) * 0.5f + 0.5f;
            this.seed2 = (float) Math.cos(index * 2.3f + 0.7f) * 0.5f + 0.5f;
            this.seed3 = (float) Math.sin(index * 3.1f + 1.1f) * 0.5f + 0.5f;
            this.sizeMultiplier = 0.8f + seed3 * 0.4f;
        }
    }

    private static void createCrystals() {
        crystalList.clear();
        int count = 14;
        for (int i = 0; i < count; i++) {
            float angle = i * (360f / count);
            float radius = 0.7f;
            float x = radius * (float) Math.cos(Math.toRadians(angle));
            float z = radius * (float) Math.sin(Math.toRadians(angle));
            float y = 0.5f + (float) Math.sin(i * 0.5f) * 0.5f;
            crystalList.add(new CrystalData(x, y, z, i));
        }
    }

    public static void render(Entity target, MatrixStack matrices) {
        if (target == null || target == mc.player) return;

        if (crystalList.isEmpty() || lastRenderedTarget != target) {
            createCrystals();
            lastRenderedTarget = target;
        }

        float tickDelta = MathUtil.getTickDelta();
        double interpolatedX = MathUtil.interpolate(target.prevX, target.getX(), tickDelta);
        double interpolatedY = MathUtil.interpolate(target.prevY, target.getY(), tickDelta);
        double interpolatedZ = MathUtil.interpolate(target.prevZ, target.getZ(), tickDelta);

        Vec3d targetPos = new Vec3d(interpolatedX, interpolatedY, interpolatedZ);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d renderPos = targetPos.subtract(cameraPos);

        float time = (mc.player.age + tickDelta) * 3.0f;
        globalRotation = time;

        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        renderBloom(matrices);

        renderCubes(matrices, time);

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void renderBloom(MatrixStack matrices) {
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        Camera camera = mc.getEntityRenderDispatcher().camera;

        for (int i = 0; i < crystalList.size(); i++) {
            CrystalData crystal = crystalList.get(i);

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(globalRotation));
            matrices.translate(crystal.offset.x, crystal.offset.y, crystal.offset.z);

            float pulseTime = (System.currentTimeMillis() % 2000) / 2000.0f;
            float pulse = 1.0f + (float) Math.sin(pulseTime * Math.PI * 2 + i * 0.5f) * 0.15f;
            float bloomSize = 0.35f * crystal.sizeMultiplier * pulse;

            int whiteColor = new Color(255, 255, 255, 180).getRGB();
            drawBillboardBloom(matrices, camera, 0, 0, 0, bloomSize, whiteColor, 0.6f);

            matrices.pop();
        }

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static void renderCubes(MatrixStack matrices, float time) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < crystalList.size(); i++) {
            CrystalData crystal = crystalList.get(i);

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(globalRotation));
            matrices.translate(crystal.offset.x, crystal.offset.y, crystal.offset.z);

            float selfRotation = time * (1.0f + crystal.seed1 * 0.5f) + i * 20f;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(selfRotation));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(selfRotation * 0.7f));

            float size = 0.12f * crystal.sizeMultiplier;

            float colorProgress = (float) i / crystalList.size();
            int color = interpolateColor(new Color(255, 255, 255).getRGB(), new Color(255, 255, 255).getRGB(), colorProgress);

            drawCube(buffer, matrices, size, color, 0.85f);

            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawBillboardBloom(MatrixStack matrices, Camera camera,
                                           float x, float y, float z,
                                           float size, int color, float alpha) {
        MatrixStack tempMatrix = new MatrixStack();
        tempMatrix.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        tempMatrix.translate(x, y, z);
        tempMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        tempMatrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        MatrixStack.Entry entry = tempMatrix.peek().copy();

        int finalColor = ColorUtil.multAlpha(color, alpha);
        Vector4i vColor = new Vector4i(finalColor, finalColor, finalColor, finalColor);

        RenderUtil3D.drawTexture(entry, BLOOM_TEXTURE, -size / 2, -size / 2, size, size, vColor, true);
    }

    private static void drawCube(BufferBuilder buffer, MatrixStack matrices,
                                 float size, int color, float alpha) {
        MatrixStack.Entry entry = matrices.peek();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * alpha);

        float s = size / 2;

        int frontColor = getBrighterColor(r, g, b, a, 1.2f);
        drawQuadAsTriangles(buffer, entry, -s, -s, s,  s, -s, s,  s, s, s,  -s, s, s, frontColor);

        int backColor = getDarkerColor(r, g, b, a, 0.5f);
        drawQuadAsTriangles(buffer, entry, s, -s, -s,  -s, -s, -s,  -s, s, -s,  s, s, -s, backColor);

        int topColor = getBrighterColor(r, g, b, a, 1.1f);
        drawQuadAsTriangles(buffer, entry, -s, s, s,  s, s, s,  s, s, -s,  -s, s, -s, topColor);

        int bottomColor = getDarkerColor(r, g, b, a, 0.6f);
        drawQuadAsTriangles(buffer, entry, -s, -s, -s,  s, -s, -s,  s, -s, s,  -s, -s, s, bottomColor);

        int rightColor = getColor(r, g, b, a, 0.9f);
        drawQuadAsTriangles(buffer, entry, s, -s, s,  s, -s, -s,  s, s, -s,  s, s, s, rightColor);

        int leftColor = getDarkerColor(r, g, b, a, 0.7f);
        drawQuadAsTriangles(buffer, entry, -s, -s, -s,  -s, -s, s,  -s, s, s,  -s, s, -s, leftColor);
    }

    private static void drawQuadAsTriangles(BufferBuilder buffer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        Matrix4f matrix = entry.getPositionMatrix();

        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
        buffer.vertex(matrix, x3, y3, z3).color(color);

        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x3, y3, z3).color(color);
        buffer.vertex(matrix, x4, y4, z4).color(color);
    }

    private static int getBrighterColor(int r, int g, int b, int a, float multiplier) {
        r = Math.min(255, (int) (r * multiplier));
        g = Math.min(255, (int) (g * multiplier));
        b = Math.min(255, (int) (b * multiplier));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getDarkerColor(int r, int g, int b, int a, float multiplier) {
        r = (int) (r * multiplier);
        g = (int) (g * multiplier);
        b = (int) (b * multiplier);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getColor(int r, int g, int b, int a, float multiplier) {
        r = (int) (r * multiplier);
        g = (int) (g * multiplier);
        b = (int) (b * multiplier);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0, Math.min(1, progress));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);

        return (r << 16) | (g << 8) | b;
    }
}