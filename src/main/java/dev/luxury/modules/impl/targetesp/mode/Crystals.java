package dev.luxury.modules.impl.targetesp.mode;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.math.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Crystals {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Entity lastRenderedTarget = null;
    private static final List<Crystal> crystalList = new ArrayList<>();
    private static float rotationAngle = 0;

    private static class Crystal {
        private final Entity entity;
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size;
        private final float rotationSpeed;

        public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
            this.entity = entity;
            this.position = position;
            this.rotation = rotation;
            this.size = 0.05f;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms, float anim, float red) {
            ms.push();
            ms.translate(position.x, position.y, position.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            int baseColor = interpolateColor(new Color(255, 101, 57).getRGB(), new Color(255, 0, 0).getRGB(), red);

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, anim);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            drawCrystal(ms, baseColor, 0.8f, false, anim);

            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            ms.pop();

            drawBloomSphere(ms, baseColor, anim);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            Identifier bloomTexture = Identifier.of("luxury", "images/ghost.png");
            RenderSystem.setShaderTexture(0, bloomTexture);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);

            int bloomColor = setAlpha(baseColor, (int) (0.4f * 25 * anim));
            float bloomSize = size * 13.0f;
            float pitch = mc.gameRenderer.getCamera().getPitch();
            float yaw = mc.gameRenderer.getCamera().getYaw();
            int segments = 6;

            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }

            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }

            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float anim) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;
            List<Vec3d> topVertices = new ArrayList<>();
            List<Vec3d> bottomVertices = new ArrayList<>();

            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vec3d(x, h_prism / 2, z));
                bottomVertices.add(new Vec3d(x, -h_prism / 2, z));
            }

            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBottom = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);
            int finalColor = setAlpha(baseColor, (int) (alpha * 255 * anim));

            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = topVertices.get((i + 1) % numSides);
                Vec3d v4 = topVertices.get(i);
                drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }

            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = topVertices.get(i);
                Vec3d v2 = topVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }

            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            Matrix4f matrix = ms.peek().getPositionMatrix();
            if (filled) {
                bb.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            } else {
                bb.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                drawTriangle(ms, bb, v1, v2, v3, color, true);
                drawTriangle(ms, bb, v1, v3, v4, color, true);
            } else {
                Matrix4f matrix = ms.peek().getPositionMatrix();
                bb.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }
    }

    private static void createCrystals(Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(target, new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(target, new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(target, new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 0.75, 0), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0, 0, 0)));
    }

    public static void render(Entity target, MatrixStack ms) {
        if (target == null || target == mc.player) return;

        if (crystalList.isEmpty() || lastRenderedTarget != target) {
            createCrystals(target);
            lastRenderedTarget = target;
        }

        double interpolatedX = MathUtil.interpolate(target.prevX, target.getX(), MathUtil.getTickDelta());
        double interpolatedY = MathUtil.interpolate(target.prevY, target.getY(), MathUtil.getTickDelta());
        double interpolatedZ = MathUtil.interpolate(target.prevZ, target.getZ(), MathUtil.getTickDelta());
        Vec3d targetPos = new Vec3d(interpolatedX, interpolatedY, interpolatedZ);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        targetPos = targetPos.subtract(cameraPos);

        rotationAngle = (rotationAngle + 0.5f) % 360;

        RenderSystem.enableDepthTest();
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        for (Crystal crystal : crystalList) {
            crystal.render(ms, 1.0f, 0.0f);
        }

        ms.pop();
    }

    private static int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0, Math.min(1, progress));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (r << 16) | (g << 8) | b;
    }

    private static int setAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}