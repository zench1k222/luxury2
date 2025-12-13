package dev.luxury.modules.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.animations.infinity.InfinityAnimation;
import dev.luxury.utils.animations.Easing;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.render.ColorUtil;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "FireFly",
        desc = "Светлячки вокруг игрока",
        category = Category.Render
)
public class FireFly extends Module {
    private final List<FireFlyEntity> particles = new ArrayList<>();

    private final SliderSetting count = new SliderSetting("Количество", "Количество светлячков", 100, 10, 300, 10);
    private final SliderSetting speed = new SliderSetting("Скорость", "Скорость движения", 0.15, 0.05, 0.5, 0.05);
    private final SliderSetting radius = new SliderSetting("Радиус спавна", "Радиус появления", 25, 10, 50, 5);
    private final SliderSetting trailLength = new SliderSetting("Длина шлейфа", "Длина следа", 20, 5, 40, 5);
    private final BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", true);

    private static final Identifier TEXTURE = Identifier.of("luxury", "images/firefly.png");

    public FireFly() {
        addSettings(count, speed, radius, trailLength, randomColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    private void spawnParticle() {
        if (mc.player == null) return;

        double distance = MathUtil.random(5, radius.getFloatValue());
        double yawRad = Math.toRadians(MathUtil.random(0, 360));
        double xOffset = -Math.sin(yawRad) * distance;
        double zOffset = Math.cos(yawRad) * distance;
        double yOffset = MathUtil.random(-5, 10);

        double velocitySpeed = speed.getFloatValue();
        double velocityYaw = Math.toRadians(MathUtil.random(0, 360));
        double velocityPitch = Math.toRadians(MathUtil.random(-30, 30));

        Vector3d initialVelocity = new Vector3d(
                -Math.sin(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed,
                Math.sin(velocityPitch) * velocitySpeed * 0.5,
                Math.cos(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed
        );

        particles.add(new FireFlyEntity(
                new Vector3d(
                        mc.player.getX() + xOffset,
                        mc.player.getY() + yOffset,
                        mc.player.getZ() + zOffset
                ),
                initialVelocity,
                particles.size(),
                generateRandomColor()
        ));
    }

    private int generateRandomColor() {
        return Color.getHSBColor(
                (float) Math.random(),
                0.7f + (float) Math.random() * 0.3f,
                0.8f + (float) Math.random() * 0.2f
        ).getRGB();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null) return;

        particles.removeIf(particle ->
                particle.time.finished(8000) ||
                        particle.position.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) >= 60
        );

        while (particles.size() < count.getIntValue()) {
            spawnParticle();
        }

        MatrixStack matrices = event.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (FireFlyEntity particle : particles) {
            particle.update();
            renderTrail(matrices, particle);
            renderParticle(matrices, particle);
        }

        cleanupRenderState();
    }

    private void renderTrail(MatrixStack matrices, FireFlyEntity particle) {
        List<Vector3d> trail = particle.getTrail();
        if (trail.size() < 2) return;

        updateParticleAlpha(particle);
        float baseAlpha = particle.getAlpha().getValue();

        int color = randomColor.get() ?
                particle.getColor() :
                ColorUtil.getColorStyle(particle.index * 50);

        for (int i = 0; i < trail.size(); i++) {
            Vector3d pos = trail.get(i);

            float fade = (float) i / (float) trail.size();
            float size = 0.15f * fade;
            int trailAlpha = (int) (baseAlpha * fade * 0.8f);
            int trailColor = ColorUtil.changeAlpha(color, trailAlpha);

            matrices.push();
            setupOrientationMatrix(matrices, (float) pos.x, (float) pos.y, (float) pos.z);

            drawQuad(matrices, -size, -size, size, size, trailColor);

            matrices.pop();

            if (i % 3 == 0 && fade > 0.3f) {
                int miniParticleCount = 2 + (int)(Math.random() * 3);
                for (int j = 0; j < miniParticleCount; j++) {
                    double offsetX = (Math.random() - 0.5) * 0.3;
                    double offsetY = (Math.random() - 0.5) * 0.3;
                    double offsetZ = (Math.random() - 0.5) * 0.3;

                    float miniSize = 0.04f + (float)(Math.random() * 0.03f);
                    int miniAlpha = (int) (trailAlpha * 0.6f);
                    int miniColor = ColorUtil.changeAlpha(color, miniAlpha);

                    matrices.push();
                    setupOrientationMatrix(matrices,
                            (float) (pos.x + offsetX),
                            (float) (pos.y + offsetY),
                            (float) (pos.z + offsetZ));

                    drawQuad(matrices, -miniSize, -miniSize, miniSize, miniSize, miniColor);

                    matrices.pop();
                }
            }
        }
    }

    private void renderParticle(MatrixStack matrices, FireFlyEntity particle) {
        updateParticleAlpha(particle);

        float baseAlpha = particle.getAlpha().getValue();
        int pulseAlpha = particle.getPulseAlpha();
        int finalAlpha = (int) Math.min(baseAlpha, pulseAlpha);

        int color = randomColor.get() ?
                particle.getColor() :
                ColorUtil.getColorStyle(particle.index * 50);

        Vector3d pos = particle.getPosition();

        matrices.push();
        setupOrientationMatrix(matrices, (float) pos.x, (float) pos.y, (float) pos.z);

        // Outer glow
        float glowSize = 0.35f;
        int glowColor = ColorUtil.changeAlpha(color, (int)(finalAlpha * 0.6f));
        drawQuad(matrices, -glowSize, -glowSize, glowSize, glowSize, glowColor);

        // Main particle
        float mainSize = 0.22f;
        int mainColor = ColorUtil.changeAlpha(color, finalAlpha);
        drawQuad(matrices, -mainSize, -mainSize, mainSize, mainSize, mainColor);

        // Bright core
        float coreSize = 0.10f;
        int coreColor = ColorUtil.changeAlpha(0xFFFFFFFF, finalAlpha);
        drawQuad(matrices, -coreSize, -coreSize, coreSize, coreSize, coreColor);

        matrices.pop();
    }

    private void updateParticleAlpha(FireFlyEntity particle) {
        long fadeInDuration = 500;
        long fadeOutStart = 8000 - fadeInDuration;

        // Fade in
        if (particle.getAlpha().getValue() < 255 && !particle.time.finished(fadeInDuration)) {
            particle.getAlpha().animate(255, fadeInDuration);
        }

        // Fade out
        if (particle.getAlpha().getValue() > 0 && particle.time.finished(fadeOutStart)) {
            particle.getAlpha().animate(0, fadeInDuration);
        }
    }

    private void cleanupRenderState() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // Helper methods for 3D rendering
    private void setupOrientationMatrix(MatrixStack matrices, float x, float y, float z) {
        if (mc.player == null || mc.gameRenderer == null) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(
                x - cameraPos.x,
                y - cameraPos.y,
                z - cameraPos.z
        );

        matrices.multiply(mc.gameRenderer.getCamera().getRotation());
    }

    private void drawQuad(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(red, green, blue, alpha);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    @Getter
    private class FireFlyEntity {
        private final int index;
        private final TimerUtils time = new TimerUtils();
        private final InfinityAnimation alpha = new InfinityAnimation(Easing.EASE_OUT_CUBIC);
        private final int color;
        private final Vector3d position;
        private final Vector3d velocity;
        private final List<Vector3d> trail = new ArrayList<>();

        public FireFlyEntity(Vector3d position, Vector3d velocity, int index, int color) {
            this.position = position;
            this.velocity = velocity;
            this.index = index;
            this.color = color;
            this.time.reset();

            this.trail.add(new Vector3d(position.x, position.y, position.z));
        }

        public void update() {
            // Add randomness to velocity
            double randomness = 0.01;
            velocity.x += (Math.random() - 0.5) * randomness;
            velocity.y += (Math.random() - 0.5) * randomness;
            velocity.z += (Math.random() - 0.5) * randomness;

            // Clamp velocity
            double maxSpeed = speed.getFloatValue() * 1.5;
            velocity.x = MathHelper.clamp(velocity.x, -maxSpeed, maxSpeed);
            velocity.y = MathHelper.clamp(velocity.y, -maxSpeed, maxSpeed);
            velocity.z = MathHelper.clamp(velocity.z, -maxSpeed, maxSpeed);

            // Update position
            position.x += velocity.x;
            position.y += velocity.y;
            position.z += velocity.z;

            // Update trail
            trail.add(new Vector3d(position.x, position.y, position.z));

            int maxTrailLength = trailLength.getIntValue();
            while (trail.size() > maxTrailLength) {
                trail.remove(0);
            }
        }

        public int getPulseAlpha() {
            double pulse = (Math.sin(time.getStartTime() / 300.0) + 1.0) / 2.0;
            return (int) (pulse * 255);
        }
    }
}