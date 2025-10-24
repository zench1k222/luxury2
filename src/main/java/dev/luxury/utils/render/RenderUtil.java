package dev.luxury.utils.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.shaders.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.util.function.Supplier;

public class RenderUtil {
    static MinecraftClient mc = MinecraftClient.getInstance();
    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false));

    private static Framebuffer getMainFbo() {
        return mc.getFramebuffer();
    }

    public static void drawImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, int color) {
        drawImage(matrices, texture, x, y, width, height, new Vector4f(0, 0, 0, 0), 1.0f, color);
    }

    public static void drawImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                 float u1, float v1, float u2, float v2, int color) {
        drawImage(matrices, texture, x, y, width, height, u1, v1, u2, v2, new Vector4f(0, 0, 0, 0), 1.0f, color);
    }

    public static void drawImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                 Vector4f rounding, float smoothness, int color) {
        drawImage(matrices, texture, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, rounding, smoothness, color);
    }


    public static void drawImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                 float u1, float v1, float u2, float v2, Vector4f rounding, float smoothness, int color) {
        enableRender();

        RenderSystem.setShaderTexture(0, texture);

        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.TEXTURE_SHADER_KEY);

        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(rounding.x, rounding.y, rounding.z, rounding.w);
        shader.getUniform("Smoothness").set(smoothness);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        bufferBuilder.vertex(matrix, x, y, 0.0F).texture(u1, v1).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(u1, v2).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(u2, v2).color(red, green, blue, alpha);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(u2, v1).color(red, green, blue, alpha);

        endBuilding(bufferBuilder);

        RenderSystem.setShaderTexture(0, 0);
        disableRender();
    }
    public static void drawRoundedRectGradient(MatrixStack matrices, float x, float y, float width, float height,
                                               Vector4f rounding, int colorLeft, int colorRight) {
        enableRender();

        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.RECTANGLE_SHADER_KEY);
        setRoundedRectShaderUniforms(shader, width, height, rounding, 1.0f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float alphaL = (float)(colorLeft >> 24 & 255) / 255.0F;
        float redL = (float)(colorLeft >> 16 & 255) / 255.0F;
        float greenL = (float)(colorLeft >> 8 & 255) / 255.0F;
        float blueL = (float)(colorLeft & 255) / 255.0F;

        float alphaR = (float)(colorRight >> 24 & 255) / 255.0F;
        float redR = (float)(colorRight >> 16 & 255) / 255.0F;
        float greenR = (float)(colorRight >> 8 & 255) / 255.0F;
        float blueR = (float)(colorRight & 255) / 255.0F;

        bufferBuilder.vertex(matrix, x, y, 0.0F).color(redL, greenL, blueL, alphaL);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(redL, greenL, blueL, alphaL);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(redR, greenR, blueR, alphaR);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(redR, greenR, blueR, alphaR);

        endBuilding(bufferBuilder);
        disableRender();
    }
    public static void drawRoundedRectGradient(MatrixStack matrices, float x, float y, float width, float height,
                                               Vector4f rounding,
                                               int colorTopLeft, int colorTopRight,
                                               int colorBottomRight, int colorBottomLeft) {
        enableRender();

        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.RECTANGLE_SHADER_KEY);
        setRoundedRectShaderUniforms(shader, width, height, rounding, 1.0f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float aTL = (float)(colorTopLeft >> 24 & 255) / 255.0F;
        float rTL = (float)(colorTopLeft >> 16 & 255) / 255.0F;
        float gTL = (float)(colorTopLeft >> 8 & 255) / 255.0F;
        float bTL = (float)(colorTopLeft & 255) / 255.0F;

        float aTR = (float)(colorTopRight >> 24 & 255) / 255.0F;
        float rTR = (float)(colorTopRight >> 16 & 255) / 255.0F;
        float gTR = (float)(colorTopRight >> 8 & 255) / 255.0F;
        float bTR = (float)(colorTopRight & 255) / 255.0F;

        float aBR = (float)(colorBottomRight >> 24 & 255) / 255.0F;
        float rBR = (float)(colorBottomRight >> 16 & 255) / 255.0F;
        float gBR = (float)(colorBottomRight >> 8 & 255) / 255.0F;
        float bBR = (float)(colorBottomRight & 255) / 255.0F;

        float aBL = (float)(colorBottomLeft >> 24 & 255) / 255.0F;
        float rBL = (float)(colorBottomLeft >> 16 & 255) / 255.0F;
        float gBL = (float)(colorBottomLeft >> 8 & 255) / 255.0F;
        float bBL = (float)(colorBottomLeft & 255) / 255.0F;

        bufferBuilder.vertex(matrix, x, y, 0.0F).color(rTL, gTL, bTL, aTL);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(rBL, gBL, bBL, aBL);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(rBR, gBR, bBR, aBR);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(rTR, gTR, bTR, aTR);

        endBuilding(bufferBuilder);
        disableRender();
    }
    public static void drawRoundedRectGradientAnimated(MatrixStack matrices, float x, float y, float width, float height,
                                                       Vector4f rounding,
                                                       int colorTopLeft, int colorTopRight,
                                                       int colorBottomRight, int colorBottomLeft,
                                                       float speed) {
        enableRender();

        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.RECTANGLE_SHADER_KEY);
        setRoundedRectShaderUniforms(shader, width, height, rounding, 1.0f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float t = (System.currentTimeMillis() % (long) speed) / speed;

        float wave = (float) Math.sin(t * Math.PI * 2.0) * 0.5f + 0.5f;

        int topLeft = ColorUtil.overCol(colorTopLeft, colorTopRight, wave);
        int bottomRight = ColorUtil.overCol(colorBottomRight, colorBottomLeft, 1.0f - wave);

        float[] tl = ColorUtil.toRGBAf(topLeft);
        float[] tr = ColorUtil.toRGBAf(colorTopRight);
        float[] br = ColorUtil.toRGBAf(bottomRight);
        float[] bl = ColorUtil.toRGBAf(colorBottomLeft);

        bufferBuilder.vertex(matrix, x, y, 0.0F).color(tl[0], tl[1], tl[2], tl[3]);                 // top-left
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(bl[0], bl[1], bl[2], bl[3]);        // bottom-left
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(br[0], br[1], br[2], br[3]);// bottom-right
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(tr[0], tr[1], tr[2], tr[3]);         // top-right

        endBuilding(bufferBuilder);
        disableRender();
    }

    public static void drawImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height) {
        drawImage(matrices, texture, x, y, width, height, 0xFFFFFFFF);
    }


    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, float radius, int color) {
        drawImage(matrices, texture, x, y, width, height, new Vector4f(radius, radius, radius, radius), 1.0f, color);
    }

    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height, Vector4f rounding, int color) {
        drawImage(matrices, texture, x, y, width, height, rounding, 1.0f, color);
    }

    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                        float u1, float v1, float u2, float v2, float radius, int color) {
        drawImage(matrices, texture, x, y, width, height, u1, v1, u2, v2, new Vector4f(radius, radius, radius, radius), 1.0f, color);
    }

    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                        float u1, float v1, float u2, float v2, Vector4f rounding, int color) {
        drawImage(matrices, texture, x, y, width, height, u1, v1, u2, v2, rounding, 1.0f, color);
    }

    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x, float y, float width, float height,
                                        float u1, float v1, float u2, float v2, Vector4f rounding, float smoothness, int color) {
        drawImage(matrices, texture, x, y, width, height, u1, v1, u2, v2, rounding, smoothness, color);
    }

    public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, Vector4f rounding, float blurRadius, int color) {
        final SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        final Framebuffer mainFbo = getMainFbo();

        if (fbo.textureWidth != mainFbo.textureWidth || fbo.textureHeight != mainFbo.textureHeight) {
            fbo.resize(mainFbo.textureWidth, mainFbo.textureHeight);
        }

        enableRender();
        fbo.beginWrite(false);
        mainFbo.draw(fbo.textureWidth, fbo.textureHeight);
        mainFbo.beginWrite(false);

        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.RECTANGLE_BLUR_SHADER_KEY);
        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(rounding.x, rounding.y, rounding.z, rounding.w);
        shader.getUniform("Smoothness").set(1.2f);
        shader.getUniform("BlurRadius").set(blurRadius);

        ShaderManager.vertexShader(matrices, x, y, width, height, color);

        RenderSystem.setShaderTexture(0, 0);
        disableRender();
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, Vector4f rounding, int color) {
        enableRender();
        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.RECTANGLE_SHADER_KEY);
        setRoundedRectShaderUniforms(shader, width, height, rounding, 1.0f);
        ShaderManager.vertexShader(matrices, x, y, width, height, color);
        disableRender();
    }

    public static void drawBorder(MatrixStack matrices, float x, float y, float width, float height,
                                  Vector4f radius, int color, float thickness,
                                  float internalSmoothness, float externalSmoothness, boolean shadow) {
        enableRender();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ShaderProgram shader = RenderSystem.setShader(ResourceProvider.BORDER_SHADER_KEY);

        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(radius.x, radius.y, radius.z, radius.w);
        shader.getUniform("Thickness").set(thickness);
        shader.getUniform("Smoothness").set(internalSmoothness, externalSmoothness);
        shader.getUniform("Shadow").set(shadow ? 1.0f : 0.0f);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(color);

        endBuilding(bufferBuilder);
        disableRender();
    }

    private static void setRoundedRectShaderUniforms(ShaderProgram shader, float width, float height, Vector4f radius, float smoothness) {
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(radius.x, radius.y, radius.z, radius.w);
        shader.getUniform("Smoothness").set(smoothness);
    }

    private static void enableRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void disableRender() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null)
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
    }

    public static final Render3D render3D = new Render3D();

    public static class Render3D {
        public void endBuilding(BufferBuilder builder) {
            BufferRenderer.drawWithGlobalProgram(builder.end());
        }
    }
}