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
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;


import java.awt.Color;

public class Circle {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    private static final int segments = 360;

    public static void render(Entity target, MatrixStack matrices) {
        if (target == null || target == mc.player || target instanceof ArmorStandEntity) return;

        var camera = mc.gameRenderer.getCamera();

        double tPosX = MathUtil.interpolate(target.prevX, target.getX(), MathUtil.getTickDelta()) - camera.getPos().x;
        double tPosY = MathUtil.interpolate(target.prevY, target.getY(), MathUtil.getTickDelta()) - camera.getPos().y;
        double tPosZ = MathUtil.interpolate(target.prevZ, target.getZ(), MathUtil.getTickDelta()) - camera.getPos().z;

        float height = target.getHeight() + 0.1f;

        double duration = 2500.0;
        double elapsed = (System.currentTimeMillis() % duration);
        boolean side = elapsed > duration / 2.0;
        double progress = elapsed / (duration / 2.0);

        if (side) {
            --progress;
        } else {
            progress = 1 - progress;
        }

        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
        double eased = (double) (height / 1.5F) * (progress > 0.5 ? 1.0 - progress : progress) * (double) (side ? -1 : 1);

        matrices.push();
        matrices.translate(tPosX, tPosY, tPosZ);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        
        assert mc.player != null;
        if (mc.player.canSee(target)) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Color themeColor =new Color(Color.yellow.getRGB());
        int bloomColor = themeColor.getRGB();
        int coreColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 1).getRGB();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i <= segments; ++i) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * target.getWidth());
            float z = (float) (Math.sin(angle) * target.getWidth());

            buffer.vertex(matrix, x, (float) ((height * progress + eased)), z)
                    .color(coreColor);

            buffer.vertex(matrix, x, (float) ((height * progress)), z)
                    .color(bloomColor);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; ++i) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * target.getWidth());
            float z = (float) (Math.sin(angle) * target.getWidth());

            buffer.vertex(matrix, x, (float) (height * progress), z)
                    .color(bloomColor);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableCull();
        
        if (mc.player.canSee(target)) {
            RenderSystem.depthMask(true);
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        matrices.pop();
    }
} 