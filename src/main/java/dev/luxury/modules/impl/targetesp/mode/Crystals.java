package dev.luxury.modules.impl.targetesp.mode;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.render.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Crystals {
    private static class Theme {
        public static final int BLUE = ColorUtil.getColor(100, 150, 255, 255);
        public static final int PURPLE = ColorUtil.getColor(200, 100, 255, 255);

        public static int getGradientColor(float t, float alpha) {
            t = MathHelper.clamp(t, 0, 1);
            int r1 = ColorUtil.red(BLUE);
            int g1 = ColorUtil.green(BLUE);
            int b1 = ColorUtil.blue(BLUE);
            int r2 = ColorUtil.red(PURPLE);
            int g2 = ColorUtil.green(PURPLE);
            int b2 = ColorUtil.blue(PURPLE);

            int r = (int)(r1 + (r2 - r1) * t);
            int g = (int)(g1 + (g2 - g1) * t);
            int b = (int)(b1 + (b2 - b1) * t);

            return ColorUtil.getColor(r, g, b, (int)(alpha * 255));
        }
    }
    public static final Crystals instance = new Crystals();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Crystal> crystalList = new ArrayList<>();
    private LivingEntity lastTarget = null;

    private int lastHurtTime = 0;
    private float rotationAngle = 0;
    private int rotationDirection = 1;
    private float hitAnimation = 0;
    private float spawnAnimation = 0;
    private float crackAmount = 0;
    private final Random crackRandom = new Random();
    private Vec3d lastRenderPosition = null;
    private boolean wasTargetVisible = false;

    private boolean pulseCrystalSetting;
    private int numSidesSetting;
    private boolean shouldRotate;

    private int getAccentColor(int hue, float alpha) {
        float h = (hue % 360) / 360f;
        float v = 1.0f;
        int c = (int) (v * 255);
        int x = (int) (c * (1 - Math.abs((h * 6) % 2 - 1)));
        int m = (int) (v * 255 - c);

        int r, g, b;
        if (h < 1f/6f) { r = c; g = x; b = 0; }
        else if (h < 2f/6f) { r = x; g = c; b = 0; }
        else if (h < 3f/6f) { r = 0; g = c; b = x; }
        else if (h < 4f/6f) { r = 0; g = x; b = c; }
        else if (h < 5f/6f) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        return ColorUtil.getColor(r + m, g + m, b + m, (int)(alpha * 255));
    }

    private void updateAnimations(LivingEntity target) {
        boolean targetPresent = target != null;
        boolean targetAlive = target != null && target.isAlive();

        if (targetAlive && spawnAnimation < 1.0f) {
            spawnAnimation += 0.05f;
        } else if ((!targetPresent || (crackAmount >= 1.0f && !targetAlive)) && spawnAnimation > 0.0f) {
            spawnAnimation -= 0.04f;
        }
        spawnAnimation = MathHelper.clamp(spawnAnimation, 0, 1);

        if (!targetPresent) return;

        if (targetAlive) {
            this.crackAmount = 1.0f - (target.getHealth() / target.getMaxHealth());
        } else {
            if (this.crackAmount < 1.0f) {
                this.crackAmount += 0.05f;
            }
        }
        this.crackAmount = MathHelper.clamp(this.crackAmount, 0, 1);

        if (target.hurtTime > this.lastHurtTime) {
            this.rotationDirection *= -1;
            this.hitAnimation = 1.0f;
        }
        this.lastHurtTime = target.hurtTime;

        if (this.hitAnimation > 0) {
            this.hitAnimation -= 0.02f;
        }
        this.hitAnimation = MathHelper.clamp(this.hitAnimation, 0, 1);

        if (shouldRotate) {
            rotationAngle += 1.5f * rotationDirection;
        }
    }

    public void onRenderWorldEvent(EventRender3D event3d, LivingEntity target, boolean pulseCrystal, Float numSides, boolean rotation) {
        this.pulseCrystalSetting = pulseCrystal;
        this.shouldRotate = rotation;

        boolean numSidesChanged = numSides.intValue() != this.numSidesSetting;
        if (numSidesChanged) {
            this.numSidesSetting = numSides.intValue();
        }

        boolean targetJustAppeared = target != null && !wasTargetVisible;
        final int EXPECTED_CRYSTAL_COUNT = 16;

        boolean needsCrystals = false;

        if (target != lastTarget) {
            if (target != null) {
                this.crackAmount = 0;
                this.hitAnimation = 0;
                this.rotationDirection = 1;
                needsCrystals = true;
            }
            lastTarget = target;
        } else if (target != null) {
            if (crystalList.isEmpty() || crystalList.size() != EXPECTED_CRYSTAL_COUNT || numSidesChanged) {
                needsCrystals = true;
            }
        }

        if (targetJustAppeared || (needsCrystals && target != null)) {
            if (needsCrystals && target != null) {
                createCrystals(target);
            }
            if (target != null) {
                spawnAnimation = 0;
            }
        }

        wasTargetVisible = target != null;

        updateAnimations(target);

        if (spawnAnimation <= 0) {
            if (target == null) {
                crystalList.clear();
                lastRenderPosition = null;
            }
            return;
        }

        if (target != null && (crystalList.isEmpty() || crystalList.size() != EXPECTED_CRYSTAL_COUNT)) {
            createCrystals(target);
            spawnAnimation = 0;
            return;
        }

        double x, y, z;
        if (target != null) {
            x = MathUtil.interpolate(target.prevX, target.getX(), MathUtil.getTickDelta());
            y = MathUtil.interpolate(target.prevY, target.getY(), MathUtil.getTickDelta());
            z = MathUtil.interpolate(target.prevZ, target.getZ(), MathUtil.getTickDelta());
            lastRenderPosition = new Vec3d(x, y, z);
        } else if (lastRenderPosition != null) {
            x = lastRenderPosition.x;
            y = lastRenderPosition.y;
            z = lastRenderPosition.z;
        } else {
            return;
        }

        MatrixStack ms = event3d.getMatrices();
        var camera = mc.gameRenderer.getCamera();
        ms.push();
        ms.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationAngle));

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (Crystal crystal : crystalList) {
            crystal.renderGlow(ms);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (Crystal crystal : crystalList) {
            crystal.renderMain(ms);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        ms.pop();

        RenderSystem.enableDepthTest();
    }

    private void createCrystals(LivingEntity target) {
        crystalList.clear();
        int numSides = this.numSidesSetting;

        double radius1 = 0.65;
        double radius2 = 0.85;
        double radius3 = 1.0;

        crystalList.add(new Crystal(target, new Vec3d(radius1, 2.2, 0), new Vector3f(0, 0, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius1, 2.2, 0), new Vector3f(0, 0, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(0, 2.2, radius1), new Vector3f(0, 0, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(0, 2.2, -radius1), new Vector3f(0, 0, 0), numSides));

        crystalList.add(new Crystal(target, new Vec3d(radius2 * 0.707, 1.5, radius2 * 0.707), new Vector3f(-30, 0, 30), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius2 * 0.707, 1.5, radius2 * 0.707), new Vector3f(30, 0, 30), numSides));
        crystalList.add(new Crystal(target, new Vec3d(radius2 * 0.707, 1.5, -radius2 * 0.707), new Vector3f(-30, 0, -30), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius2 * 0.707, 1.5, -radius2 * 0.707), new Vector3f(30, 0, -30), numSides));

        crystalList.add(new Crystal(target, new Vec3d(radius3, 1.0, 0), new Vector3f(0, 45, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius3, 1.0, 0), new Vector3f(0, -45, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.0, radius3), new Vector3f(45, 0, 0), numSides));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.0, -radius3), new Vector3f(-45, 0, 0), numSides));

        crystalList.add(new Crystal(target, new Vec3d(radius2 * 0.707, 0.5, radius2 * 0.707), new Vector3f(-20, 0, 20), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius2 * 0.707, 0.5, radius2 * 0.707), new Vector3f(20, 0, 20), numSides));
        crystalList.add(new Crystal(target, new Vec3d(radius2 * 0.707, 0.5, -radius2 * 0.707), new Vector3f(-20, 0, -20), numSides));
        crystalList.add(new Crystal(target, new Vec3d(-radius2 * 0.707, 0.5, -radius2 * 0.707), new Vector3f(20, 0, -20), numSides));
    }


    private class Crystal {
        private final Vec3d position;
        private final Vector3f rotation;
        private final float size;
        private final int numSides;

        public Crystal(LivingEntity entity, Vec3d position, Vector3f rotation, int numSides) {
            this.position = position;
            this.rotation = rotation;
            this.size = 0.09f;
            this.numSides = numSides;
        }

        public void drawForStencil(MatrixStack ms) {
            ms.push();
            float recoil = 1.0f + 0.4f * (float)Math.sin(hitAnimation * Math.PI);
            Vec3d animPos = position.multiply(recoil);
            ms.translate(animPos.x, animPos.y, animPos.z);

            ms.scale(1.0f, 1.0f, 1.0f);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));
            drawCrystal(ms, 0, 0, true, true);
            ms.pop();
        }

        public void renderGlow(MatrixStack ms) {
            if (!pulseCrystalSetting) return;

            ms.push();

            float recoil = 1.0f + 0.4f * (float)Math.sin(hitAnimation * Math.PI);
            Vec3d animPos = position.multiply(recoil);
            ms.translate(animPos.x, animPos.y, animPos.z);

            double offsetY = Math.sin(System.currentTimeMillis() / 400.0) * 0.05;
            ms.translate(0, offsetY, 0);

            float breath = 1.0f + 0.08f * (float)Math.sin(System.currentTimeMillis() / 300.0);
            ms.scale(breath, breath, breath);

            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y() + (System.currentTimeMillis() % 3600L) / 10f));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, 0, 0.12f, true, false);
            ms.pop();

            ms.pop();
        }

        public void renderMain(MatrixStack ms) {
            ms.push();

            float recoil = 1.0f + 0.4f * (float)Math.sin(hitAnimation * Math.PI);
            Vec3d animPos = position.multiply(recoil);
            ms.translate(animPos.x, animPos.y, animPos.z);

            double offsetY = Math.sin(System.currentTimeMillis() / 400.0) * 0.05;
            ms.translate(0, offsetY, 0);

            float breath = 1.0f;
            if (pulseCrystalSetting) {
                breath += 0.08f * (float)Math.sin(System.currentTimeMillis() / 300.0);
            }

            ms.scale(breath, breath, breath);

            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y() + (System.currentTimeMillis() % 3600L) / 10f));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            drawCrystal(ms, 0, 0.9f, true, false);

            RenderSystem.lineWidth(1.5f);

            drawCrystal(ms, 0, 1.0f, false, true);

            ms.pop();
        }

        private int brighter(int color, float factor) {
            int r = ColorUtil.red(color);
            int g = ColorUtil.green(color);
            int b = ColorUtil.blue(color);
            return ColorUtil.getColor(
                    Math.min(255, (int)(r * factor)),
                    Math.min(255, (int)(g * factor)),
                    Math.min(255, (int)(b * factor)),
                    ColorUtil.alpha(color)
            );
        }

        private int darker(int color, float factor) {
            int r = ColorUtil.red(color);
            int g = ColorUtil.green(color);
            int b = ColorUtil.blue(color);
            return ColorUtil.getColor(
                    (int)(r * factor),
                    (int)(g * factor),
                    (int)(b * factor),
                    ColorUtil.alpha(color)
            );
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, boolean outline) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

            float radius = size * 1.2f;
            int segments = Math.max(8, numSides);
            int rings = segments / 2;

            var matrix = ms.peek().getPositionMatrix();
            List<List<Vec3d>> sphereVertices = new ArrayList<>();
            List<List<Float>> gradientValues = new ArrayList<>();

            long time = System.currentTimeMillis();
            float timeOffset = (time % 5000) / 5000.0f;

            for (int ring = 0; ring <= rings; ring++) {
                List<Vec3d> ringVertices = new ArrayList<>();
                List<Float> ringGradients = new ArrayList<>();
                float phi = (float) (Math.PI * ring / rings);
                for (int seg = 0; seg <= segments; seg++) {
                    float theta = (float) (2 * Math.PI * seg / segments);
                    float x = radius * (float) (Math.sin(phi) * Math.cos(theta));
                    float y = radius * (float) Math.cos(phi);
                    float z = radius * (float) (Math.sin(phi) * Math.sin(theta));
                    ringVertices.add(new Vec3d(x, y, z));

                    float gradientT = (float)((phi / Math.PI + (theta / (2 * Math.PI)) * 0.5 + timeOffset * 0.3) % 1.0);
                    ringGradients.add(gradientT);
                }
                sphereVertices.add(ringVertices);
                gradientValues.add(ringGradients);
            }

            for (int ring = 0; ring < rings; ring++) {
                for (int seg = 0; seg < segments; seg++) {
                    Vec3d v1 = sphereVertices.get(ring).get(seg);
                    Vec3d v2 = sphereVertices.get(ring).get(seg + 1);
                    Vec3d v3 = sphereVertices.get(ring + 1).get(seg);
                    Vec3d v4 = sphereVertices.get(ring + 1).get(seg + 1);

                    float g1 = gradientValues.get(ring).get(seg);
                    float g2 = gradientValues.get(ring).get(seg + 1);
                    float g3 = gradientValues.get(ring + 1).get(seg);
                    float g4 = gradientValues.get(ring + 1).get(seg + 1);

                    drawTriangleGradient(matrix, bufferBuilder, v1, v2, v3, g1, g2, g3, alpha * spawnAnimation, filled, outline);
                    drawTriangleGradient(matrix, bufferBuilder, v2, v4, v3, g2, g4, g3, alpha * spawnAnimation, filled, outline);
                }
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

            if (filled && crackAmount > 0) {
                bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
                int crackColor = ColorUtil.multAlpha(0, alpha * spawnAnimation);

                int numCracks = (int)(crackAmount * 8);
                for (int i = 0; i < numCracks; i++) {
                    long seed = i * 12345L;
                    crackRandom.setSeed(seed);

                    float phi1 = crackRandom.nextFloat() * (float)Math.PI;
                    float theta1 = crackRandom.nextFloat() * 2 * (float)Math.PI;
                    float phi2 = crackRandom.nextFloat() * (float)Math.PI;
                    float theta2 = crackRandom.nextFloat() * 2 * (float)Math.PI;

                    float x1 = radius * (float)(Math.sin(phi1) * Math.cos(theta1));
                    float y1 = radius * (float)Math.cos(phi1);
                    float z1 = radius * (float)(Math.sin(phi1) * Math.sin(theta1));

                    float x2 = radius * (float)(Math.sin(phi2) * Math.cos(theta2));
                    float y2 = radius * (float)Math.cos(phi2);
                    float z2 = radius * (float)(Math.sin(phi2) * Math.sin(theta2));

                    Vec3d p1 = new Vec3d(x1, y1, z1);
                    Vec3d p2 = new Vec3d(x2, y2, z2);

                    float[] rgba = ColorUtil.rgba(crackColor);
                    bufferBuilder.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
                    bufferBuilder.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
                }
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            }
        }

        private void drawTriangleGradient(org.joml.Matrix4f matrix, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, float g1, float g2, float g3, float alpha, boolean filled, boolean outline) {
            Vec3d center = v1.add(v2).add(v3).multiply(1.0/3.0);
            Vec3d animV1 = center.lerp(v1, spawnAnimation);
            Vec3d animV2 = center.lerp(v2, spawnAnimation);
            Vec3d animV3 = center.lerp(v3, spawnAnimation);

            int color1 = Theme.getGradientColor(g1, alpha);
            int color2 = Theme.getGradientColor(g2, alpha);
            int color3 = Theme.getGradientColor(g3, alpha);

            float r1 = ColorUtil.red(color1) / 255.0f;
            float g1f = ColorUtil.green(color1) / 255.0f;
            float b1 = ColorUtil.blue(color1) / 255.0f;
            float a1 = ColorUtil.alpha(color1) / 255.0f;

            float r2 = ColorUtil.red(color2) / 255.0f;
            float g2f = ColorUtil.green(color2) / 255.0f;
            float b2 = ColorUtil.blue(color2) / 255.0f;
            float a2 = ColorUtil.alpha(color2) / 255.0f;

            float r3 = ColorUtil.red(color3) / 255.0f;
            float g3f = ColorUtil.green(color3) / 255.0f;
            float b3 = ColorUtil.blue(color3) / 255.0f;
            float a3 = ColorUtil.alpha(color3) / 255.0f;

            if (filled) {
                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(r1, g1f, b1, a1);
                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(r2, g2f, b2, a2);
                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(r3, g3f, b3, a3);
            } else if (outline) {
                float avgR = (r1 + r2 + r3) / 3.0f;
                float avgG = (g1f + g2f + g3f) / 3.0f;
                float avgB = (b1 + b2 + b3) / 3.0f;
                float avgA = (a1 + a2 + a3) / 3.0f;

                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(avgR, avgG, avgB, avgA);
                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(avgR, avgG, avgB, avgA);

                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(avgR, avgG, avgB, avgA);
                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(avgR, avgG, avgB, avgA);

                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(avgR, avgG, avgB, avgA);
                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(avgR, avgG, avgB, avgA);
            }
        }

        private void drawTriangle(org.joml.Matrix4f matrix, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, float alpha, boolean filled, boolean outline) {
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = alpha;

            Vec3d center = v1.add(v2).add(v3).multiply(1.0/3.0);
            Vec3d animV1 = center.lerp(v1, spawnAnimation);
            Vec3d animV2 = center.lerp(v2, spawnAnimation);
            Vec3d animV3 = center.lerp(v3, spawnAnimation);

            if (filled) {
                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(r * 0.8f, g * 0.8f, b * 0.8f, a);
                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(r, g, b, a);
                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(r, g, b, a);
            } else if (outline) {
                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(r, g, b, a);
                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(r, g, b, a);

                bb.vertex(matrix, (float)animV2.x, (float)animV2.y, (float)animV2.z).color(r, g, b, a);
                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(r, g, b, a);

                bb.vertex(matrix, (float)animV3.x, (float)animV3.y, (float)animV3.z).color(r, g, b, a);
                bb.vertex(matrix, (float)animV1.x, (float)animV1.y, (float)animV1.z).color(r, g, b, a);
            }
        }

        private void drawCracksOnTriangle(org.joml.Matrix4f matrix, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color) {
            Vec3d center = v1.add(v2).add(v3).multiply(1.0/3.0);
            Vec3d animV1 = center.lerp(v1, spawnAnimation);
            Vec3d animV2 = center.lerp(v2, spawnAnimation);
            Vec3d animV3 = center.lerp(v3, spawnAnimation);

            long seed = Double.doubleToLongBits(v1.x + v2.y + v3.z);
            crackRandom.setSeed(seed);

            int numCracks = (int)(crackAmount * 4);
            for (int i = 0; i < numCracks; i++) {
                Vec3d p1 = getRandomPointOnEdge(animV1, animV2, animV3, crackRandom.nextInt(3));
                Vec3d p2 = getRandomPointOnEdge(animV1, animV2, animV3, crackRandom.nextInt(3));

                float[] rgba = ColorUtil.rgba(color);
                bb.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
                bb.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            }
        }

        private Vec3d getRandomPointOnEdge(Vec3d v1, Vec3d v2, Vec3d v3, int edge) {
            float t = crackRandom.nextFloat();
            return switch (edge) {
                case 0 -> v1.lerp(v2, t);
                case 1 -> v2.lerp(v3, t);
                default -> v3.lerp(v1, t);
            };
        }
    }
}