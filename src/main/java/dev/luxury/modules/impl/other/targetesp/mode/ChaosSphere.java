package dev.luxury.modules.impl.other.targetesp.mode;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.math.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;

public class ChaosSphere {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier BLOOM_TEXTURE = Identifier.of("luxury", "images/ghost.png");
    private static final int SEGMENTS = 36;
    private static final int LAYERS = 4;

    private static float rotationX = 0;
    private static float rotationY = 0;
    private static float rotationZ = 0;
    private static float pulsation = 0;

    public static void render(Entity target, MatrixStack matrices) {
        if (target == null || target == mc.player || target instanceof ArmorStandEntity) return;

        var camera = mc.gameRenderer.getCamera();

        double tPosX = MathUtil.interpolate(target.prevX, target.getX(), MathUtil.getTickDelta()) - camera.getPos().x;
        double tPosY = MathUtil.interpolate(target.prevY, target.getY(), MathUtil.getTickDelta()) - camera.getPos().y + target.getHeight() / 2;
        double tPosZ = MathUtil.interpolate(target.prevZ, target.getZ(), MathUtil.getTickDelta()) - camera.getPos().z;

        // Анимационные значения
        long time = System.currentTimeMillis();
        rotationX = (time * 0.05f) % 360;
        rotationY = (time * 0.07f) % 360;
        rotationZ = (time * 0.03f) % 360;
        pulsation = 1.0f + (float) Math.sin(time * 0.002) * 0.15f;

        matrices.push();
        matrices.translate(tPosX, tPosY, tPosZ);

        // Настройка рендера
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();

        // Настройка глубины в зависимости от видимости
        if (mc.player != null && mc.player.canSee(target)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        // Основные параметры - УВЕЛИЧИВАЕМ размеры!
        float baseRadius = target.getWidth() * 3.0f; // Было 1.5f
        Color primaryColor = new Color(0, 200, 255, 200);
        Color secondaryColor = new Color(255, 50, 150, 200);
        Color accentColor = new Color(150, 255, 100, 200);

        // Основная сфера
        renderMainSphere(matrices, baseRadius, primaryColor, secondaryColor);

        // Внешние эффекты
        renderExternalEffects(matrices, baseRadius, accentColor);

        // Восстановление состояний
        if (mc.player != null && mc.player.canSee(target)) {
            RenderSystem.depthMask(true);
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        matrices.pop();
    }

    private static void renderMainSphere(MatrixStack matrices, float baseRadius, Color primary, Color secondary) {
        matrices.push();

        // Вращаем всю сферу
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        // Сетчатая сфера (внешняя)
        renderWireSphere(matrices, baseRadius * 1.2f * pulsation,
                new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 100), true);

        // Полупрозрачная внутренняя сфера
        renderSolidSphere(matrices, baseRadius * 0.8f * pulsation, primary);

        // Анимированные кольца
        renderAnimatedRings(matrices, baseRadius, secondary);

        matrices.pop();
    }

    private static void renderWireSphere(MatrixStack matrices, float radius, Color color, boolean doublePass) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int colorInt = color.getRGB();

        // Вертикальные окружности
        int verticalCircles = 8;
        int pointsPerCircle = 24;

        for (int v = 0; v < verticalCircles; v++) {
            float vAngle = (float) (Math.PI * v / (verticalCircles - 1));
            float sinV = (float) Math.sin(vAngle);
            float cosV = (float) Math.cos(vAngle);

            for (int h = 0; h <= pointsPerCircle; h++) {
                float hAngle = (float) (2 * Math.PI * h / pointsPerCircle);
                float sinH = (float) Math.sin(hAngle);
                float cosH = (float) Math.cos(hAngle);

                float x = radius * sinV * cosH;
                float y = radius * cosV;
                float z = radius * sinV * sinH;

                buffer.vertex(matrix, x, y, z).color(colorInt);
            }
        }

        // Горизонтальные окружности
        int horizontalCircles = 6;
        for (int h = 0; h < horizontalCircles; h++) {
            float hAngle = (float) (2 * Math.PI * h / horizontalCircles);
            float sinBase = (float) Math.sin(hAngle);
            float cosBase = (float) Math.cos(hAngle);

            for (int v = 0; v <= pointsPerCircle; v++) {
                float vAngle = (float) (2 * Math.PI * v / pointsPerCircle);
                float sinV = (float) Math.sin(vAngle);
                float cosV = (float) Math.cos(vAngle);

                float x = radius * cosV * cosBase;
                float y = radius * sinV;
                float z = radius * cosV * sinBase;

                buffer.vertex(matrix, x, y, z).color(colorInt);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Второй проход для большего объема
        if (doublePass) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));

            buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );

            matrix = matrices.peek().getPositionMatrix();
            int secondaryColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50).getRGB();

            // Упрощенная версия для второго прохода
            for (int i = 0; i < 4; i++) {
                float angle = (float) (Math.PI * i / 2);

                for (int j = 0; j <= 12; j++) {
                    float jAngle = (float) (2 * Math.PI * j / 12);

                    float x = radius * (float) (Math.sin(angle) * Math.cos(jAngle));
                    float y = radius * (float) Math.cos(angle);
                    float z = radius * (float) (Math.sin(angle) * Math.sin(jAngle));

                    buffer.vertex(matrix, x, y, z).color(secondaryColor);
                }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }
    }

    private static void renderSolidSphere(MatrixStack matrices, float radius, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_STRIP,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int segments = 24;

        for (int i = 0; i <= segments; i++) {
            float theta1 = (float) (Math.PI * i / segments);
            float theta2 = (float) (Math.PI * (i + 1) / segments);

            for (int j = 0; j <= segments; j++) {
                float phi = (float) (2 * Math.PI * j / segments + rotationZ * 0.01f);

                float sinTheta1 = (float) Math.sin(theta1);
                float cosTheta1 = (float) Math.cos(theta1);
                float sinTheta2 = (float) Math.sin(theta2);
                float cosTheta2 = (float) Math.cos(theta2);
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);

                float x1 = radius * sinTheta1 * cosPhi;
                float y1 = radius * cosTheta1;
                float z1 = radius * sinTheta1 * sinPhi;

                float x2 = radius * sinTheta2 * cosPhi;
                float y2 = radius * cosTheta2;
                float z2 = radius * sinTheta2 * sinPhi;

                // Прозрачность зависит от положения на сфере
                int alpha1 = (int) (color.getAlpha() * (0.3f + 0.7f * Math.abs(cosTheta1)));
                int alpha2 = (int) (color.getAlpha() * (0.3f + 0.7f * Math.abs(cosTheta2)));

                buffer.vertex(matrix, x1, y1, z1).color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        alpha1
                );

                buffer.vertex(matrix, x2, y2, z2).color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        alpha2
                );
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderAnimatedRings(MatrixStack matrices, float baseRadius, Color color) {
        long time = System.currentTimeMillis();

        for (int ring = 0; ring < 3; ring++) {
            matrices.push();

            float ringSpeed = 0.5f + ring * 0.3f;
            float ringOffset = time * 0.001f * ringSpeed;
            float ringTilt = (float) Math.sin(time * 0.0005 + ring) * 30;

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ringTilt));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY * 1.5f + ringOffset * 50));

            float ringRadius = baseRadius * (1.0f + ring * 0.3f);
            float ringPulse = 1.0f + (float) Math.sin(time * 0.003 + ring) * 0.2f;

            renderRing(matrices, ringRadius * ringPulse,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 150 - ring * 40));

            matrices.pop();
        }
    }

    private static void renderRing(MatrixStack matrices, float radius, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.LINE_STRIP,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int points = 48;

        for (int i = 0; i <= points; i++) {
            float angle = (float) (2 * Math.PI * i / points);
            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);

            // Пульсирующая прозрачность
            float pulse = (float) (0.7f + 0.3f * Math.sin(angle * 3 + System.currentTimeMillis() * 0.002));
            int alpha = (int) (color.getAlpha() * pulse);

            buffer.vertex(matrix, x, 0, z).color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    alpha
            );
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderExternalEffects(MatrixStack matrices, float baseRadius, Color color) {
        long time = System.currentTimeMillis();

        // Энергетические частицы
        renderEnergyParticles(matrices, baseRadius, color);

        // Bloom-эффекты (БОЛЬШИЕ и правильно расположенные!)
        renderBigBloomEffects(matrices, baseRadius * 2.5f, color);
    }

    private static void renderEnergyParticles(MatrixStack matrices, float radius, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int particleCount = 16;
        long time = System.currentTimeMillis();

        for (int i = 0; i < particleCount; i++) {
            float baseAngle = (float) (2 * Math.PI * i / particleCount);
            float timeOffset = time * 0.001f + i * 0.2f;

            // Орбитальное движение
            float orbitRadius = radius * 1.5f;
            float orbitSpeed = 0.8f + i * 0.1f;
            float orbitAngle = baseAngle + timeOffset * orbitSpeed;

            // Дополнительное вертикальное движение
            float verticalOffset = (float) Math.sin(timeOffset * 2.0f + i) * radius * 0.4f;

            float x = orbitRadius * (float) Math.cos(orbitAngle);
            float y = verticalOffset;
            float z = orbitRadius * (float) Math.sin(orbitAngle);

            // Размер и прозрачность
            float size = 0.15f + 0.1f * (float) Math.sin(timeOffset * 3.0f);
            float alpha = 0.6f + 0.4f * (float) Math.sin(timeOffset * 4.0f + i);

            Color particleColor = new Color(
                    Math.min(255, color.getRed() + 50),
                    Math.min(255, color.getGreen() + 30),
                    color.getBlue(),
                    (int)(alpha * 200)
            );

            // Рендер частицы как квадрата
            renderBillboardParticle(buffer, matrix, x, y, z, size, particleColor.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderBillboardParticle(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float size, int color) {
        // Простой квадрат, всегда обращенный к камере
        buffer.vertex(matrix, x - size, y - size, z).color(color);
        buffer.vertex(matrix, x + size, y - size, z).color(color);
        buffer.vertex(matrix, x + size, y + size, z).color(color);
        buffer.vertex(matrix, x - size, y + size, z).color(color);
    }

    private static void renderBigBloomEffects(MatrixStack matrices, float radius, Color baseColor) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.depthMask(false);

        long time = System.currentTimeMillis();
        int bloomCount = 6;

        for (int i = 0; i < bloomCount; i++) {
            matrices.push();

            // Расположение bloom-эффектов на большем радиусе
            float angle = (float) (2 * Math.PI * i / bloomCount + time * 0.0003);
            float verticalAngle = (float) (Math.PI * (i % 3) / 3);

            float distance = radius * (0.9f + 0.1f * (float) Math.sin(time * 0.001 + i));

            float x = distance * (float) (Math.cos(angle) * Math.sin(verticalAngle));
            float y = distance * (float) Math.cos(verticalAngle);
            float z = distance * (float) (Math.sin(angle) * Math.sin(verticalAngle));

            matrices.translate(x, y, z);

            // Всегда направляем к камере
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw() + 180));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            // БОЛЬШОЙ размер bloom!
            float bloomSize = radius * 0.8f; // Значительно увеличенный размер
            float pulse = 1.0f + 0.3f * (float) Math.sin(time * 0.002 + i * 0.5f);
            bloomSize *= pulse;

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR
            );

            // Динамическая прозрачность
            int alpha = (int) (180 + 75 * Math.sin(time * 0.003 + i));
            int bloomColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    alpha
            ).getRGB();

            // Рендер большого квадрата с текстурой bloom
            buffer.vertex(matrix, -bloomSize, -bloomSize, 0).texture(0, 1).color(bloomColor);
            buffer.vertex(matrix, bloomSize, -bloomSize, 0).texture(1, 1).color(bloomColor);
            buffer.vertex(matrix, bloomSize, bloomSize, 0).texture(1, 0).color(bloomColor);
            buffer.vertex(matrix, -bloomSize, bloomSize, 0).texture(0, 0).color(bloomColor);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }

        // Дополнительные стационарные bloom-эффекты
        renderStationaryBlooms(matrices, radius * 1.2f, baseColor);

        RenderSystem.depthMask(true);
    }

    private static void renderStationaryBlooms(MatrixStack matrices, float radius, Color baseColor) {
        long time = System.currentTimeMillis();

        // 4 стационарных bloom по сторонам света
        for (int i = 0; i < 4; i++) {
            matrices.push();

            float angle = (float) (Math.PI / 2 * i + time * 0.0002);
            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);

            matrices.translate(x, 0, z);

            // Всегда к камере
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float bloomSize = radius * 0.5f;
            float pulse = 1.0f + 0.2f * (float) Math.sin(time * 0.0025 + i);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR
            );

            int alpha = (int) (100 + 50 * Math.sin(time * 0.004 + i));
            int bloomColor = new Color(255, 255, 255, alpha).getRGB(); // Белый bloom для контраста

            buffer.vertex(matrix, -bloomSize * pulse, -bloomSize * pulse, 0)
                    .texture(0, 1).color(bloomColor);
            buffer.vertex(matrix, bloomSize * pulse, -bloomSize * pulse, 0)
                    .texture(1, 1).color(bloomColor);
            buffer.vertex(matrix, bloomSize * pulse, bloomSize * pulse, 0)
                    .texture(1, 0).color(bloomColor);
            buffer.vertex(matrix, -bloomSize * pulse, bloomSize * pulse, 0)
                    .texture(0, 0).color(bloomColor);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }
    }
}