package dev.luxury.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.math.ProjectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class RenderUtil3D {
    private static Tessellator tessellatorFaces = null;
    private static Tessellator tessellatorOutlines = null;
    public static final List<Texture> TEXTURE_DEPTH = new ArrayList<>();
    public static final List<Texture> TEXTURE = new ArrayList<>();
    public static final int ALL_FACES = 0xFFFFFF;
    public static final int ALL_LINES = 0xFFFFFF;
    public static final List<Line> LINE_DEPTH = new ArrayList<>();
    public static final List<Line> LINE = new ArrayList<>();
    public static List<DebugLineAction> debugLineQueue = new ArrayList<>();
    public static List<LineAction> lineQueue = new ArrayList<>();

    public static void onRender3D(MatrixStack stack) {
        renderDebugLines(stack);
        renderLines(stack);
    }
    public static void renderTextures() {
        // Рендеринг текстур без depth test
        if (!TEXTURE.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE.stream()
                    .map(texture -> texture.id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR
                );

                TEXTURE.stream()
                        .filter(texture -> texture.id.equals(id))
                        .forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));

                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            TEXTURE.clear();
        }

        // Рендеринг текстур с depth test
        if (!TEXTURE_DEPTH.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE_DEPTH.stream()
                    .map(texture -> texture.id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR
                );

                TEXTURE_DEPTH.stream()
                        .filter(texture -> texture.id.equals(id))
                        .forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));

                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            TEXTURE_DEPTH.clear();
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE_DEPTH.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE_DEPTH.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE_DEPTH.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
    }
    public void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public static void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        if (entry == null) entry = ProjectionUtil.lastWorldSpaceMatrix;
        Vector3f vec = getNormal(start, end);
        buffer.vertex(entry, start).color(startColor).normal(entry, vec);
        buffer.vertex(entry, end).color(endColor).normal(entry, vec);
    }
    public static Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        return normal.div(sqrt);
    }
    private static void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        Matrix4f matrix = entry.getPositionMatrix();

        buffer.vertex(matrix, x, y + height, 0).texture(0, 1).color(color.x);

        buffer.vertex(matrix, x + width, y + height, 0).texture(1, 1).color(color.y);

        buffer.vertex(matrix, x + width, y, 0).texture(1, 0).color(color.z);

        buffer.vertex(matrix, x, y, 0).texture(0, 0).color(color.w);
    }
    private static MinecraftClient mc = MinecraftClient.getInstance();
    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(false);
    }

    public static void hookEvent3d() {
        Camera camera = mc.gameRenderer.getCamera();
        MatrixStack matrixStack = new MatrixStack();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        MathUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
        MathUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
        MathUtil.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());

        RenderUtil3D.onRender3D(matrixStack);

        EventRender3D eventRender3D = new EventRender3D(matrixStack, mc.getRenderTickCounter().getTickDelta(false));
        EventManager.call(eventRender3D);
        renderTextures();


        RenderSystem.getModelViewStack().popMatrix();
    }

    private static void initTessellators() {
        if (tessellatorFaces == null) {
            tessellatorFaces = Tessellator.getInstance();
        }
        if (tessellatorOutlines == null) {
            tessellatorOutlines = new Tessellator(0x200000);
        }
    }

    public static void drawBox(Box box, MatrixStack matrices, Color faceColor, Color outlineColor) {
        drawBox(box, matrices, faceColor, outlineColor, ALL_FACES, ALL_LINES);
    }

    public static void drawBox(Box box, MatrixStack matrices, Color faceColor, Color outlineColor, int facesToDraw, int linesToDraw) {
        initTessellators();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        double posX = -camera.getPos().x;
        double posY = -camera.getPos().y;
        double posZ = -camera.getPos().z;

        setupRender();

        if (faceColor != null && faceColor.getAlpha() > 0) {
            drawBoxFaces(box, matrices, posX, posY, posZ, faceColor, facesToDraw);
        }

        if (outlineColor != null && outlineColor.getAlpha() > 0) {
            drawBoxOutlines(box, matrices, posX, posY, posZ, outlineColor, linesToDraw);
        }

        endRender();
    }

    public static void drawBoxFaces(Box box, MatrixStack matrices, Color faceColor) {
        drawBoxFaces(box, matrices, faceColor, ALL_FACES);
    }

    public static void drawBoxFaces(Box box, MatrixStack matrices, Color faceColor, int facesToDraw) {
        if (faceColor == null || faceColor.getAlpha() <= 0) return;

        initTessellators();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        double posX = -camera.getPos().x;
        double posY = -camera.getPos().y;
        double posZ = -camera.getPos().z;

        setupRender();
        drawBoxFaces(box, matrices, posX, posY, posZ, faceColor, facesToDraw);
        endRender();
    }

    private static void drawBoxFaces(Box box, MatrixStack matrices, double posX, double posY, double posZ, Color color, int facesToDraw) {
        matrices.push();
        matrices.translate(posX, posY, posZ);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellatorFaces.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (facesToDraw == ALL_FACES || (facesToDraw & 0x0F) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
        }

        if (facesToDraw == ALL_FACES || (facesToDraw & 0xF0) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        if (facesToDraw == ALL_FACES || (facesToDraw & 0xF00) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
        }

        if (facesToDraw == ALL_FACES || (facesToDraw & 0xF000) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
        }

        if (facesToDraw == ALL_FACES || (facesToDraw & 0xF0000) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        }

        if (facesToDraw == ALL_FACES || (facesToDraw & 0xF00000) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    public static void drawBoxOutlines(Box box, MatrixStack matrices, Color outlineColor) {
        drawBoxOutlines(box, matrices, outlineColor, ALL_LINES);
    }

    public static void drawBoxOutlines(Box box, MatrixStack matrices, Color outlineColor, int linesToDraw) {
        if (outlineColor == null || outlineColor.getAlpha() <= 0) return;

        initTessellators();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        double posX = -camera.getPos().x;
        double posY = -camera.getPos().y;
        double posZ = -camera.getPos().z;

        setupRender();
        drawBoxOutlines(box, matrices, posX, posY, posZ, outlineColor, linesToDraw);
        endRender();
    }
    public static void drawEntity(Entity entity, Vec3d pos, float yaw, int alpha, MatrixStack matrices, float tickDelta) {
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity livingEntity = (LivingEntity) entity;
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.scale(1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer != null) {
            int light = renderer.getLight(livingEntity, tickDelta);
            VertexConsumerProvider vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            EntityRenderState renderState = renderer.getAndUpdateRenderState(livingEntity, tickDelta);
            if (renderState != null) {
                renderer.render(renderState, matrices, vertexConsumers, light);
            }
            ((VertexConsumerProvider.Immediate)vertexConsumers).draw();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        matrices.pop();
    }
    private static void drawBoxOutlines(Box box, MatrixStack matrices, double posX, double posY, double posZ, Color color, int linesToDraw) {
        matrices.push();
        matrices.translate(posX, posY, posZ);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellatorOutlines.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        if (linesToDraw == ALL_LINES || (linesToDraw & 1) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 2)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 4)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 6)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 8)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 10)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 12)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 14)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.minY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 16)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 18)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 20)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
        }

        if (linesToDraw == ALL_LINES || (linesToDraw & (1 << 22)) != 0) {
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.maxZ).color(color.getRGB());
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) box.minX, (float) box.maxY, (float) box.minZ).color(color.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        matrices.pop();

    }
    private static void renderDebugLines(MatrixStack stack) {
        if (debugLineQueue.isEmpty()) {
            return;
        }

        setupRender();
        RenderSystem.disableDepthTest();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.LINES);

        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        for (DebugLineAction action : debugLineQueue) {
            MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
            vertexLine(matrices, buffer, 0f, 0f, 0f,
                    (float) (action.end.getX() - action.start.getX()),
                    (float) (action.end.getY() - action.start.getY()),
                    (float) (action.end.getZ() - action.start.getZ()),
                    action.color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        endRender();

        debugLineQueue.clear();
    }

    private static void renderLines(MatrixStack stack) {
        if (lineQueue.isEmpty()) {
            return;
        }

        setupRender();
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(2f);
        RenderSystem.disableDepthTest();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        for (LineAction action : lineQueue) {
            MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
            vertexLine(matrices, buffer, 0f, 0f, 0f,
                    (float) (action.end.getX() - action.start.getX()),
                    (float) (action.end.getY() - action.start.getY()),
                    (float) (action.end.getZ() - action.start.getZ()),
                    action.color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1f);
        RenderSystem.enableDepthTest();
        endRender();

        lineQueue.clear();
    }

    public static void setupRender() {
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void endRender() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

        return matrices;
    }

    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);

        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
    }
    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public static void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }
    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public record DebugLineAction(Vec3d start, Vec3d end, Color color) {
    }

    public record LineAction(Vec3d start, Vec3d end, Color color) {
    }
    public static void drawTexture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
        Texture texture = new Texture(entry, id, x, y, width, height, color);
        if (depth) TEXTURE_DEPTH.add(texture); else TEXTURE.add(texture);
    }
    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {}
    public record Texture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {}
}
