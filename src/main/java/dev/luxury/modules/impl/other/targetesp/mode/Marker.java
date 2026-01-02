package dev.luxury.modules.impl.other.targetesp.mode;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.math.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.Color;

public class Marker {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public  static Identifier targetPng = Identifier.of("luxury", "images/marker.png");
    private static float rotationAngle = 0.0f;
    private static float rotationSpeed = 0.0f;
    private static boolean isReversing = false;
    private static final float maxRotationSpeed = 4.0f;
    private static long lasthittime = 0L;
    private static final long hitduration = 750L;
    private static long lastUpdateTime = System.currentTimeMillis();
    private static final float TIME_FACTOR = 0.1f;
    public static void onHit(LivingEntity target) {
        if (target != null) {
            lasthittime = System.currentTimeMillis();
        }
    }
    public static void render(Entity target, MatrixStack matrices) {
        if (target == null || target == mc.player || target instanceof ArmorStandEntity) return;
        var camera = mc.gameRenderer.getCamera();

        var hitBox = target.getBoundingBox();
        double tPosX = MathUtil.interpolate(target.prevX, target.getX(), MathUtil.getTickDelta()) - camera.getPos().x;
        double tPosY = MathUtil.interpolate(target.prevY, target.getY(), MathUtil.getTickDelta()) - camera.getPos().y + (hitBox.maxY - hitBox.minY) / 2.0;
        double tPosZ = MathUtil.interpolate(target.prevZ, target.getZ(), MathUtil.getTickDelta()) - camera.getPos().z;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.push();
        matrices.translate(tPosX, tPosY, tPosZ);

        matrices.multiply(camera.getRotation());

        matrices.scale(0.6F, 0.6F, 0.6F);

        tick();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, targetPng);
        matrices.translate(-0.75, -0.75, -0.01);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - lasthittime;
        float hitprogress = 1.0f - MathHelper.clamp(timeSinceHit / (float)hitduration, 0.0f, 1.0f);

        int tintColor = applyDamageTint(0xFFFFFFFF, hitprogress);
        Color themeColor =new Color(tintColor);

        buffer.vertex(matrix, 0, 1.5f, 0).texture(0f, 1f).color(themeColor.getRGB());
        buffer.vertex(matrix, 1.5f, 1.5f, 0).texture(1f, 1f).color(themeColor.getRGB());
        buffer.vertex(matrix, 1.5f, 0, 0).texture(1f, 0).color(themeColor.getRGB());
        buffer.vertex(matrix, 0, 0, 0).texture(0, 0).color(themeColor.getRGB());

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        matrices.pop();
    }

    private static void tick() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        float deltaSeconds = deltaTime * TIME_FACTOR;

        if (!isReversing) {
            rotationSpeed += 0.02f * deltaSeconds;
            if (rotationSpeed > maxRotationSpeed) {
                rotationSpeed = maxRotationSpeed;
                isReversing = true;
            }
        } else {
            rotationSpeed -= 0.02f * deltaSeconds;
            if (rotationSpeed < -maxRotationSpeed) {
                rotationSpeed = -maxRotationSpeed;
                isReversing = false;
            }
        }

        rotationAngle += rotationSpeed * deltaSeconds;
        rotationAngle = (rotationAngle + 360) % 360;

        lastUpdateTime = currentTime;
    }
    private static int applyDamageTint(int baseColor, float strength) {
        int r = 255;
        int g = (int)(255 * (1.0f - strength * 0.7f));
        int b = (int)(255 * (1.0f - strength * 0.7f));
        return (baseColor & 0xFFff0000) | (r << 16) | (g << 8) | b;
    }
}