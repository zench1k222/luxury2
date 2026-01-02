package dev.luxury.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.render.ColorUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "FireFly",
        desc = "Светлячки вокруг игрока",
        category = Category.Render
)
public class FireFly extends Module {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final List<FireFlyEntity> particles = new ArrayList<>();

    private final SliderSetting count = new SliderSetting("Количество", "Количество светлячков", 100, 10, 500, 10);
    private final SliderSetting speed = new SliderSetting("Скорость", "Скорость движения", 0.15f, 0.05f, 0.5f, 0.05f);
    private final SliderSetting radius = new SliderSetting("Радиус спавна", "Радиус появления", 25f, 10f, 50f, 5f);
    private final SliderSetting trailLength = new SliderSetting("Длина шлейфа", "Длина следа", 20, 5, 40, 5);
    private final SliderSetting particleSize = new SliderSetting("Размер", "Размер частиц", 0.22f, 0.1f, 0.5f, 0.05f);
    private final ModeSetting renderMode = new ModeSetting("Режим рендера", "Квады", new String[]{"Квады", "Текстура"});
    private final BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", true);
    private final BooleanSetting bloomEffect = new BooleanSetting("Свечение", true);
    private final BooleanSetting trailEnabled = new BooleanSetting("Шлейф", true);

    private static final Identifier FIREFLY_TEXTURE = Identifier.of("luxury", "images/firefly.png");
    private static final Identifier BLOOM_TEXTURE = Identifier.of("luxury", "images/ghost.png");

    private BufferBuilder bufferBuilder;
    private boolean textureMode = false;
    private float lastCameraPitch = 0;
    private float lastCameraYaw = 0;

    public FireFly() {
        addSettings(count, speed, radius, trailLength, particleSize, renderMode, randomColor, bloomEffect, trailEnabled);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();

        for (int i = 0; i < count.getIntValue(); i++) {
            spawnParticle();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    private void spawnParticle() {
        if (MC.player == null) return;

        double angle = Math.random() * Math.PI * 2;
        double distance = 5 + Math.random() * (radius.getFloatValue() - 5);
        double height = MC.player.getY() + Math.random() * 10 - 5;

        double x = MC.player.getX() + Math.cos(angle) * distance;
        double y = height;
        double z = MC.player.getZ() + Math.sin(angle) * distance;

        double velocityX = (Math.random() - 0.5) * speed.getFloatValue() * 2;
        double velocityY = (Math.random() - 0.5) * speed.getFloatValue();
        double velocityZ = (Math.random() - 0.5) * speed.getFloatValue() * 2;

        particles.add(new FireFlyEntity(
                new Vector3d(x, y, z),
                new Vector3d(velocityX, velocityY, velocityZ),
                particles.size()
        ));
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (MC.player == null || MC.world == null) return;

        textureMode = renderMode.is("Текстура");

        updateParticles();

        Camera camera = MC.gameRenderer.getCamera();
        lastCameraPitch = camera.getPitch();
        lastCameraYaw = camera.getYaw() + 180.0F;
        Vec3d cameraPos = camera.getPos();

        setupRenderState();

        renderAllParticles(event.getMatrices(), cameraPos);

        restoreRenderState();
    }

    private void updateParticles() {
        particles.removeIf(particle -> {
            if (MC.player == null) return true;

            double distance = particle.position.distance(
                    MC.player.getX(),
                    MC.player.getY(),
                    MC.player.getZ()
            );

            return particle.time.finished(15000) || distance > radius.getFloatValue() + 10;
        });
        while (particles.size() < count.getIntValue()) {
            spawnParticle();
        }

        for (FireFlyEntity particle : particles) {
            particle.update();
        }
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (textureMode) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }
    }

    private void renderAllParticles(MatrixStack matrices, Vec3d cameraPos) {
        beginBatch();

        for (FireFlyEntity particle : particles) {
            renderParticleBatch(particle, matrices, cameraPos);
        }
        endBatch();
    }

    private void beginBatch() {
        if (textureMode) {
            RenderSystem.setShaderTexture(0, FIREFLY_TEXTURE);
            bufferBuilder = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR
            );
        } else {
            bufferBuilder = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR
            );
        }
    }

    private void renderParticleBatch(FireFlyEntity particle, MatrixStack matrices, Vec3d cameraPos) {
        double x = particle.position.x - cameraPos.x;
        double y = particle.position.y - cameraPos.y;
        double z = particle.position.z - cameraPos.z;

        float alpha = particle.getAlpha();

        if (alpha <= 0.01f) return;

        float size = particleSize.getFloatValue();

        int color = getParticleColor(particle, alpha);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(lastCameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lastCameraPitch));

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        renderParticleQuad(matrix, size, color);

        if (bloomEffect.get()) {
            renderBloomEffect(matrix, size, color);
        }

        if (trailEnabled.get() && particle.trail.size() > 1) {
            renderTrail(particle, matrices, cameraPos, color);
        }

        matrices.pop();
    }

    private void renderParticleQuad(Matrix4f matrix, float size, int color) {
        float halfSize = size / 2;
        float a = (color >> 24 & 255) / 255.0f;
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        if (textureMode) {
            bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).texture(0, 1).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).texture(1, 1).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, halfSize, 0).texture(1, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).texture(0, 0).color(r, g, b, a);
        } else {
            bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, halfSize, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).color(r, g, b, a);
        }
    }

    private void renderBloomEffect(Matrix4f matrix, float baseSize, int color) {
        float bloomSize = baseSize * 1.8f;
        float halfBloomSize = bloomSize / 2;

        int bloomColor = ColorUtil.changeAlpha(color, (int)((color >> 24 & 255) * 0.4f));
        float ba = (bloomColor >> 24 & 255) / 255.0f;
        float br = (bloomColor >> 16 & 255) / 255.0f;
        float bg = (bloomColor >> 8 & 255) / 255.0f;
        float bb = (bloomColor & 255) / 255.0f;

        if (textureMode) {
            RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);

            bufferBuilder.vertex(matrix, -halfBloomSize, -halfBloomSize, 0).texture(0, 1).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, halfBloomSize, -halfBloomSize, 0).texture(1, 1).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, halfBloomSize, halfBloomSize, 0).texture(1, 0).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, -halfBloomSize, halfBloomSize, 0).texture(0, 0).color(br, bg, bb, ba);

            RenderSystem.setShaderTexture(0, FIREFLY_TEXTURE);
        } else {
            bufferBuilder.vertex(matrix, -halfBloomSize, -halfBloomSize, 0).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, halfBloomSize, -halfBloomSize, 0).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, halfBloomSize, halfBloomSize, 0).color(br, bg, bb, ba);
            bufferBuilder.vertex(matrix, -halfBloomSize, halfBloomSize, 0).color(br, bg, bb, ba);
        }
    }

    private void renderTrail(FireFlyEntity particle, MatrixStack matrices, Vec3d cameraPos, int color) {
        List<Vector3d> trail = particle.trail;
        int trailSize = Math.min(trail.size(), trailLength.getIntValue());

        for (int i = 0; i < trailSize - 1; i++) {
            Vector3d pos1 = trail.get(i);
            Vector3d pos2 = trail.get(i + 1);

            float trailAlpha = (1.0f - (float)i / trailSize) * 0.5f;
            int trailColor = ColorUtil.changeAlpha(color, (int)((color >> 24 & 255) * trailAlpha));

            renderTrailSegment(pos1, pos2, matrices, cameraPos, trailColor, i);
        }
    }

    private void renderTrailSegment(Vector3d pos1, Vector3d pos2, MatrixStack matrices, Vec3d cameraPos, int color, int index) {
        float segmentSize = particleSize.getFloatValue() * 0.4f * (1.0f - (float)index / trailLength.getIntValue());

        matrices.push();

        double x = ((pos1.x + pos2.x) / 2) - cameraPos.x;
        double y = ((pos1.y + pos2.y) / 2) - cameraPos.y;
        double z = ((pos1.z + pos2.z) / 2) - cameraPos.z;

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(lastCameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lastCameraPitch));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        renderParticleQuad(matrix, segmentSize, color);

        matrices.pop();
    }

    private void endBatch() {
        if (bufferBuilder != null) {
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }
    }

    private void restoreRenderState() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private int getParticleColor(FireFlyEntity particle, float alpha) {
        int baseColor;

        if (randomColor.get()) {
            baseColor = particle.color;
        } else {
            int hue = (int)(System.currentTimeMillis() / 50 + particle.index * 10) % 360;
            baseColor = ColorUtil.getColorStyle(hue);
        }

        return ColorUtil.changeAlpha(baseColor, (int)(alpha * 255));
    }

    @Getter
    private class FireFlyEntity {
        private final int index;
        private final TimerUtils time = new TimerUtils();
        private final Vector3d position;
        private final Vector3d velocity;
        private final int color;
        private final List<Vector3d> trail = new ArrayList<>();
        private float alpha = 0;
        private boolean fadingIn = true;

        public FireFlyEntity(Vector3d position, Vector3d velocity, int index) {
            this.position = position;
            this.velocity = velocity;
            this.index = index;
            this.time.reset();
            this.alpha = 0;
            this.fadingIn = true;

            this.color = Color.getHSBColor(
                    (float) Math.random(),
                    0.6f + (float) Math.random() * 0.4f,
                    0.8f + (float) Math.random() * 0.2f
            ).getRGB();

            for (int i = 0; i < trailLength.getIntValue(); i++) {
                trail.add(new Vector3d(position.x, position.y, position.z));
            }
        }

        public void update() {
            velocity.x += (Math.random() - 0.5) * 0.01;
            velocity.y += (Math.random() - 0.5) * 0.01;
            velocity.z += (Math.random() - 0.5) * 0.01;

            double maxSpeed = speed.getFloatValue() * 2;
            double currentSpeed = Math.sqrt(
                    velocity.x * velocity.x +
                            velocity.y * velocity.y +
                            velocity.z * velocity.z
            );

            if (currentSpeed > maxSpeed) {
                double scale = maxSpeed / currentSpeed;
                velocity.x *= scale;
                velocity.y *= scale;
                velocity.z *= scale;
            }

            position.x += velocity.x;
            position.y += velocity.y;
            position.z += velocity.z;

            updateAlpha();

            updateTrail();

            if (MC.player != null) {
                double dx = MC.player.getX() - position.x;
                double dy = (MC.player.getY() + 1) - position.y;
                double dz = MC.player.getZ() - position.z;

                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > radius.getFloatValue()) {
                    double pullStrength = 0.02;
                    velocity.x += dx * pullStrength;
                    velocity.y += dy * pullStrength * 0.5;
                    velocity.z += dz * pullStrength;
                } else if (distance < 3) {
                    double pushStrength = 0.01;
                    velocity.x -= dx * pushStrength;
                    velocity.y -= dy * pushStrength;
                    velocity.z -= dz * pushStrength;
                }
            }
        }

        private void updateAlpha() {
            long elapsed = time.getElapsedTime();

            if (fadingIn && alpha < 1.0f) {
                alpha = Math.min(1.0f, alpha + 0.02f);
                if (alpha >= 1.0f) {
                    fadingIn = false;
                }
            } else if (!fadingIn && elapsed > 10000) {
                long fadeStart = elapsed - 10000;
                if (fadeStart < 5000) {
                    alpha = 1.0f - (fadeStart / 5000.0f);
                } else {
                    alpha = 0;
                }
            }

            float pulse = (float)(Math.sin(elapsed / 300.0) * 0.1f + 0.9f);
            alpha *= pulse;
        }

        private void updateTrail() {
            trail.add(0, new Vector3d(position.x, position.y, position.z));

            while (trail.size() > trailLength.getIntValue()) {
                trail.remove(trail.size() - 1);
            }
        }

        public float getAlpha() {
            return alpha;
        }
    }

    private boolean isParticleVisible(Vector3d particlePos) {
        if (MC.player == null) return false;

        double distance = particlePos.distance(
                MC.player.getX(),
                MC.player.getY(),
                MC.player.getZ()
        );

        return distance <= radius.getFloatValue() + 5;
    }
}