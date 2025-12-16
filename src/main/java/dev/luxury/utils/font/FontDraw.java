package dev.luxury.utils.font;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.render.RenderUtil;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;


@SuppressWarnings("All")
public class FontDraw implements Closeable {
    private static final ExecutorService async = Executors.newSingleThreadExecutor();
    private static final float scale = 0.5f;
    private static final float invscale = 2.0f;
    private static final int COLOR_MASK_ALPHA = 0xFF000000;
    private static final int COLOR_MASK_RED = 0x00FF0000;
    private static final int COLOR_MASK_GREEN = 0x0000FF00;
    private static final int COLOR_MASK_BLUE = 0x000000FF;
    private static final int COLOR_SHIFT_ALPHA = 24;
    private static final int COLOR_SHIFT_RED = 16;
    private static final int COLOR_SHIFT_GREEN = 8;
    private static final int COLOR_SHIFT_BLUE = 0;
    private static final float colordivisor= 255f;
    private static final float invcolordivisor = 1f / 255f;

    private final Map<Identifier, ObjectList<DrawEntry>> glyhcache = new ConcurrentHashMap<>();
    private final List<GlyphMap> maps = new ArrayList<>();
    private final Char2ObjectMap<Glyph> allGlyphs = new Char2ObjectArrayMap<>();
    private final int charsPerPage;
    private final int padding;
    private final String prebakeGlyphs;
    private final float originalSize;
    private final FontRenderContext sharedFontRenderContext;

    private float cachedMaxHeight = 0f;
    private boolean heightDirty = true;

    private final Map<String, Float> widthCache = new ConcurrentHashMap<>();
    private static final int maxglyphwidth = 1000;

    private Font font;
    private Future<Void> prebakeGlyphsFuture;


    private static final Char2ObjectMap<float[]> colorcodes = new Char2ObjectArrayMap<>();
    static {
        // Minecraft color codes
        colorcodes.put('0', new float[]{0f, 0f, 0f}); // Black
        colorcodes.put('1', new float[]{0f, 0f, 0.6666667f}); // Dark Blue
        colorcodes.put('2', new float[]{0f, 0.6666667f, 0f}); // Dark Green
        colorcodes.put('3', new float[]{0f, 0.6666667f, 0.6666667f}); // Dark Aqua
        colorcodes.put('4', new float[]{0.6666667f, 0f, 0f}); // Dark Red
        colorcodes.put('5', new float[]{0.6666667f, 0f, 0.6666667f}); // Dark Purple
        colorcodes.put('6', new float[]{1f, 0.6666667f, 0f}); // Gold
        colorcodes.put('7', new float[]{0.6666667f, 0.6666667f, 0.6666667f}); // Gray
        colorcodes.put('8', new float[]{0.33333334f, 0.33333334f, 0.33333334f}); // Dark Gray
        colorcodes.put('9', new float[]{0.33333334f, 0.33333334f, 1f}); // Blue
        colorcodes.put('a', new float[]{0.33333334f, 1f, 0.33333334f}); // Green
        colorcodes.put('b', new float[]{0.33333334f, 1f, 1f}); // Aqua
        colorcodes.put('c', new float[]{1f, 0.33333334f, 0.33333334f}); // Red
        colorcodes.put('d', new float[]{1f, 0.33333334f, 1f}); // Light Purple
        colorcodes.put('e', new float[]{1f, 1f, 0.33333334f}); // Yellow
        colorcodes.put('f', new float[]{1f, 1f, 1f}); // White

        // Additional formatting codes (RGB values are ignored for these)
        colorcodes.put('k', new float[]{0f, 0f, 0f}); // Obfuscated
        colorcodes.put('l', new float[]{0f, 0f, 0f}); // Bold
        colorcodes.put('m', new float[]{0f, 0f, 0f}); // Strikethrough
        colorcodes.put('n', new float[]{0f, 0f, 0f}); // Underline
        colorcodes.put('o', new float[]{0f, 0f, 0f}); // Italic
        colorcodes.put('r', new float[]{1f, 1f, 1f}); // Reset
    }

    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL = ThreadLocal.withInitial(StringBuilder::new);
    private static final ThreadLocal<char[]> CHAR_ARRAY_BUFFER = ThreadLocal.withInitial(() -> new char[1024]);

    public FontDraw(Font font, float sizePx, int charsPerPage, int padding, String prebakeGlyphs) {
        this.originalSize = sizePx;
        this.charsPerPage = charsPerPage;
        this.padding = padding;
        this.prebakeGlyphs = prebakeGlyphs;

        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tempImage.createGraphics();
        this.sharedFontRenderContext = g.getFontRenderContext();
        g.dispose();

        initializeFont(font, sizePx);
    }

    public FontDraw(Font font, float sizePx) {
        this(font, sizePx, 256, 5, null);
    }

    private void initializeFont(Font baseFont, float sizePx) {
        this.font = baseFont.deriveFont(sizePx);
        if (prebakeGlyphs != null && !prebakeGlyphs.isEmpty()) {
            prebakeGlyphsFuture = async.submit(() -> {
                for (char c : prebakeGlyphs.toCharArray()) {
                    if (Thread.interrupted()) break;
                    locateGlyph(c);
                }
                return null;
            });
        }
    }

    // Методы для DrawContext
    public void drawFontLeft(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        draw(matrices, text, x, y, color, false);
        matrices.pop();
    }

    public void drawFontRight(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        float width = getWidth(text);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        draw(matrices, text, x - width, y, color, false);
        matrices.pop();
    }

    public void drawCenteredText(MatrixStack matrices, String text, float x, float y, int color) {
        float width = getWidth(text);
        matrices.push();
        draw(matrices, text, x - width * 0.5f, y, color, false);
        matrices.pop();
    }

    public void drawCenteredText(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        float width = getWidth(text);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        draw(matrices, text, x - width * 0.5f, y, color, false);
        matrices.pop();
    }

    public void drawAnimatedGradientText(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color1, int color2, float time) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        drawGradientInternalText(matrices, text, x, y, color1, color2, time, true);
        matrices.pop();
    }

    // Оригинальные методы с MatrixStack (для обратной совместимости)
    public void drawFontLeft(MatrixStack ms, String text, float x, float y, int color) {
        draw(ms, text, x, y, color, false);
    }

    public void drawFontRight(MatrixStack ms, String text, float x, float y, int color) {
        float width = getWidth(text);
        draw(ms, text, x - width, y, color, false);
    }

    public void drawCentered(MatrixStack ms, String text, float x, float y, int color) {
        float width = getWidth(text);
        draw(ms, text, x - width * 0.5f, y, color, false);
    }

    public void drawAnimatedGradientText(MatrixStack ms, String text, float x, float y, int color1, int color2, float time) {
        drawGradientInternalText(ms, text, x, y, color1, color2, time, true);
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0f;

        Float cached = widthCache.get(text);
        if (cached != null) return cached;

        float width = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if ((c == '§' || c == '&') && i + 1 < len) {
                i++;
                continue;
            }
            Glyph glyph = locateGlyph(c);
            if (glyph != null) width += glyph.width() * scale;
        }

        if (widthCache.size() < maxglyphwidth) {
            widthCache.put(text, width);
        }

        return width;
    }

    public float getHeight() {
        if (heightDirty) {
            float max = 0;
            for (Glyph glyph : allGlyphs.values()) {
                if (glyph != null) {
                    float h = glyph.height();
                    if (h > max) max = h;
                }
            }
            cachedMaxHeight = max * scale;
            heightDirty = false;
        }
        return cachedMaxHeight;
    }

    public void drawClipped(MatrixStack ms, String text, float maxWidth, float x, float y, int color) {
        if (text == null || text.isEmpty()) return;

        StringBuilder clipped = STRING_BUILDER_POOL.get();
        clipped.setLength(0);

        float width = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if ((c == '§' || c == '&') && i + 1 < len) {
                i++;
                continue;
            }

            Glyph glyph = locateGlyph(c);
            if (glyph == null) continue;

            float glyphWidth = glyph.width() * scale;
            if (width + glyphWidth > maxWidth) break;

            clipped.append(c);
            width += glyphWidth;
        }
        draw(ms, clipped.toString(), x, y, color, false);
    }

    // Метод для DrawContext
    public void drawClipped(net.minecraft.client.gui.DrawContext context, String text, float maxWidth, float x, float y, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        drawClipped(matrices, text, maxWidth, x, y, color);
        matrices.pop();
    }

    public List<String> splitTextToLines(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            if (currentLine.length() > 0) {
                String testLine = currentLine + " " + word;
                if (getWidth(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            } else {
                if (getWidth(word) <= maxWidth) {
                    currentLine.append(word);
                } else {
                    StringBuilder part = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (getWidth(part.toString() + c) <= maxWidth) {
                            part.append(c);
                        } else {
                            lines.add(part.toString());
                            part = new StringBuilder("" + c);
                        }
                    }
                    currentLine = part;
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    public float getStringWidthWithShadow(String text) {
        return getWidth(text) + 1; // +1 пиксель для тени
    }

    public float getLineHeight() {
        return getHeight() + 2; // +2 пикселя для междустрочного интервала
    }

    public void drawWithShadow(MatrixStack ms, String text, float x, float y, int color) {
        // Рисуем тень (немного смещенную)
        int shadowColor = (color & 0x00FFFFFF) | 0x44000000; // Полупрозрачная черная тень
        draw(ms, text, x + 1, y + 1, shadowColor, false);
        // Рисуем основной текст
        draw(ms, text, x, y, color, false);
    }

    // Метод для DrawContext
    public void drawWithShadow(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        drawWithShadow(matrices, text, x, y, color);
        matrices.pop();
    }

    private void draw(MatrixStack ms, String text, float x, float y, int color, boolean isGradient) {
        if (text == null || text.isEmpty()) return;

        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone()) {
            try {
                prebakeGlyphsFuture.get();
            } catch (Exception ignored) {}
        }

        float a = ((color >>> COLOR_SHIFT_ALPHA) & 0xFF) * invcolordivisor;
        float r = ((color >>> COLOR_SHIFT_RED) & 0xFF) * invcolordivisor;
        float g = ((color >>> COLOR_SHIFT_GREEN) & 0xFF) * invcolordivisor;
        float b = (color & 0xFF) * invcolordivisor;

        RenderSystem.setShaderColor(r, g, b, a);
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(scale, scale, 1);

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix4f = ms.peek().getPositionMatrix();

        Map<Identifier, List<DrawEntry>> localCache = new Object2ObjectOpenHashMap<>();
        glyhcache.forEach((key, value) -> localCache.put(key, new ObjectArrayList<>(value)));
        glyhcache.clear();

        float cursorX = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (!isGradient && (c == '§' || c == '&') && i + 1 < len) {
                char code = Character.toLowerCase(chars[++i]);
                float[] col = colorcodes.get(code);
                if (col != null) {
                    r = col[0];
                    g = col[1];
                    b = col[2];
                }
                continue;
            }
            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                Identifier tex = glyph.owner().bindToTexture;
                localCache.computeIfAbsent(tex, k -> new ObjectArrayList<>()).add(new DrawEntry(cursorX, 0, r, g, b, glyph));
                cursorX += glyph.width();
            }
        }

        Tessellator tessellator = tessellator();
        localCache.forEach((identifier, entries) -> {
            RenderSystem.setShaderTexture(0, identifier);
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (DrawEntry entry : entries) {
                Glyph g2 = entry.toDraw;
                GlyphMap gm = g2.owner();
                float w = g2.width();
                float h = g2.height();
                float u1 = g2.u() * gm.invWidth;
                float v1 = g2.v() * gm.invHeight;
                float u2 = (g2.u() + w) * gm.invWidth;
                float v2 = (g2.v() + h) * gm.invHeight;

                bufferBuilder.vertex(matrix4f, entry.atX, entry.atY + h, 0).texture(u1, v2).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX + w, entry.atY + h, 0).texture(u2, v2).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX + w, entry.atY, 0).texture(u2, v1).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX, entry.atY, 0).texture(u1, v1).color(entry.r, entry.g, entry.b, a);
            }
            RenderUtil.render3D.endBuilding(bufferBuilder);
        });

        ms.pop();
        RenderUtil.disableRender();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void drawGradientText(MatrixStack matrixStack, String text, float x, float y, int color1, int color2) {
        drawGradientInternalText(matrixStack, text, x, y, color1, color2, 0f, false);
    }

    // Метод для DrawContext
    public void drawGradientText(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color1, int color2) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        drawGradientInternalText(matrices, text, x, y, color1, color2, 0f, false);
        matrices.pop();
    }

    private void drawGradientInternalText(MatrixStack matrixStack, String text, float x, float y,
                                          int color1, int color2, float time, boolean animated) {
        if (text == null || text.isEmpty()) return;

        int length = text.length();
        if (length == 0) return;
        final float rStart = ((color1 >>> COLOR_SHIFT_RED) & 0xFF) * invcolordivisor;
        final float gStart = ((color1 >>> COLOR_SHIFT_GREEN) & 0xFF) * invcolordivisor;
        final float bStart = (color1 & 0xFF) * invcolordivisor;
        final float aStart = ((color1 >>> COLOR_SHIFT_ALPHA) & 0xFF) * invcolordivisor;

        final float rEnd = ((color2 >>> COLOR_SHIFT_RED) & 0xFF) * invcolordivisor;
        final float gEnd = ((color2 >>> COLOR_SHIFT_GREEN) & 0xFF) * invcolordivisor;
        final float bEnd = (color2 & 0xFF) * invcolordivisor;
        final float aEnd = ((color2 >>> COLOR_SHIFT_ALPHA) & 0xFF) * invcolordivisor;

        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone()) {
            try {
                prebakeGlyphsFuture.get();
            } catch (Exception ignored) {}
        }

        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale(scale, scale, 1);

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        Map<Identifier, List<GradientDrawEntry>> textureMap = new Object2ObjectOpenHashMap<>();

        float cursorX = 0;
        final float invLength = length == 1 ? 0 : 1f / (length - 1);

        char[] chars = getCharArray(text);
        for (int i = 0; i < length; i++) {
            char c = chars[i];

            float t = i * invLength;
            if (animated) {
                float tRaw = t + (time % 1.0f);
                t = tRaw % 1.0f;
                t = t < 0.5f ? t * 2f : (1f - t) * 2f;
            }

            final float r = rStart + (rEnd - rStart) * t;
            final float g = gStart + (gEnd - gStart) * t;
            final float b = bStart + (bEnd - bStart) * t;
            final float a = aStart + (aEnd - aStart) * t;

            Glyph glyph = locateGlyph(c);
            if (glyph == null) continue;

            textureMap.computeIfAbsent(glyph.owner().bindToTexture, k -> new ObjectArrayList<>()).add(new GradientDrawEntry(cursorX, r, g, b, a, glyph));
            cursorX += glyph.width();
        }

        Tessellator tessellator = tessellator();
        textureMap.forEach((texture, entries) -> {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (GradientDrawEntry entry : entries) {
                Glyph glyph = entry.glyph;
                float w = glyph.width();
                float h = glyph.height();
                float u1 = glyph.u() * glyph.owner().invWidth;
                float v1 = glyph.v() * glyph.owner().invHeight;
                float u2 = (glyph.u() + w) * glyph.owner().invWidth;
                float v2 = (glyph.v() + h) * glyph.owner().invHeight;

                bufferBuilder.vertex(matrix4f, entry.x, h, 0).texture(u1, v2).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x + w, h, 0).texture(u2, v2).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x + w, 0, 0).texture(u2, v1).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x, 0, 0).texture(u1, v1).color(entry.r, entry.g, entry.b, entry.a);
            }
            RenderUtil.render3D.endBuilding(bufferBuilder);
        });

        matrixStack.pop();
        RenderUtil.disableRender();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void drawString(MatrixStack matrices, String text, float x, float y, int color) {
        draw(matrices, text, x, y, color, false);
    }

    public void drawString(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        draw(matrices, text, x, y, color, false);
        matrices.pop();
    }

    public void drawCenteredString(MatrixStack matrices, String text, float x, float y, int color) {
        float width = getWidth(text);
        matrices.push();
        draw(matrices, text, x - width * 0.5f, y, color, false);
        matrices.pop();
    }

    public void drawCenteredString(net.minecraft.client.gui.DrawContext context, String text, float x, float y, int color) {
        float width = getWidth(text);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        draw(matrices, text, x - width * 0.5f, y, color, false);
        matrices.pop();
    }

    @Nullable
    private Glyph locateGlyph(char glyphChar) {
        Glyph existing = allGlyphs.get(glyphChar);
        if (existing != null) return existing;

        Glyph newGlyph = createGlyph(glyphChar);
        if (newGlyph != null) {
            allGlyphs.put(glyphChar, newGlyph);
            heightDirty = true;
        }
        return newGlyph;
    }

    @Nullable
    private Glyph createGlyph(char glyphChar) {
        for (GlyphMap map : maps) {
            if (map.contains(glyphChar)) return map.getGlyph(glyphChar);
        }

        int base = charsPerPage * (glyphChar / charsPerPage);
        String id = generateRandomId(16);
        GlyphMap newMap = new GlyphMap((char) base, (char) (base + charsPerPage), font,
                Identifier.of("font", "temp/" + id), padding, sharedFontRenderContext);
        maps.add(newMap);
        return newMap.getGlyph(glyphChar);
    }

    private char[] getCharArray(String text) {
        char[] buffer = CHAR_ARRAY_BUFFER.get();
        int len = text.length();
        if (len > buffer.length) {
            buffer = new char[len];
            CHAR_ARRAY_BUFFER.set(buffer);
        }
        text.getChars(0, len, buffer, 0);
        return buffer;
    }

    private static String generateRandomId(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private record Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {}

    private record DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {}

    private record GradientDrawEntry(float x, float r, float g, float b, float a, Glyph glyph) {}

    @Override
    public void close() {
        try {
            if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone() && !prebakeGlyphsFuture.isCancelled()) {
                prebakeGlyphsFuture.cancel(true);
                prebakeGlyphsFuture.get();
                prebakeGlyphsFuture = null;
            }
            maps.forEach(GlyphMap::destroy);
            maps.clear();
            allGlyphs.clear();
            glyhcache.clear();
            widthCache.clear();
        } catch (Exception ignored) {}
    }

    private class GlyphMap {
        private final Char2ObjectMap<Glyph> glyphs = new Char2ObjectArrayMap<>();
        private final Font font;
        private final Identifier bindToTexture;
        private final char fromIncl, toExcl;
        private final int pixelPadding;
        private final FontRenderContext fontRenderContext;
        private int width, height;
        private float invWidth, invHeight;
        private boolean generated = false;

        GlyphMap(char from, char to, Font font, Identifier id, int padding, FontRenderContext frc) {
            this.fromIncl = from;
            this.toExcl = to;
            this.font = font;
            this.bindToTexture = id;
            this.pixelPadding = padding;
            this.fontRenderContext = frc;
        }

        Glyph getGlyph(char c) {
            if (!generated) generate();
            return glyphs.get(c);
        }

        boolean contains(char c) {
            return c >= fromIncl && c < toExcl;
        }

        void destroy() {
            mc.getTextureManager().destroyTexture(bindToTexture);
            glyphs.clear();
            width = -1;
            height = -1;
            generated = false;
        }

        private void generate() {
            if (generated) return;

            int range = toExcl - fromIncl;
            int charsPerRow = (int) Math.ceil(Math.sqrt(range));
            int charsPerCol = (int) Math.ceil((double) range / charsPerRow);

            int maxCharWidth = 0;
            int maxCharHeight = 0;
            CharMetrics[] metrics = new CharMetrics[range];

            for (int i = 0; i < range; i++) {
                char c = (char) (fromIncl + i);
                Rectangle2D bounds = font.getStringBounds(String.valueOf(c), fontRenderContext);
                int w = (int) Math.ceil(bounds.getWidth());
                int h = (int) Math.ceil(bounds.getHeight());
                maxCharWidth = Math.max(maxCharWidth, w);
                maxCharHeight = Math.max(maxCharHeight, h);
                metrics[i] = new CharMetrics(c, w, h);
            }

            this.width = Math.max((maxCharWidth + pixelPadding) * charsPerRow + pixelPadding, 1);
            this.height = Math.max((maxCharHeight + pixelPadding) * charsPerCol + pixelPadding, 1);
            this.invWidth = 1f / width;
            this.invHeight = 1f / height;

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();

            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setColor(Color.WHITE);
            g2d.setFont(font);

            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics fm = g2d.getFontMetrics();
            int baseAscent = fm.getAscent();

            for (int i = 0; i < metrics.length; i++) {
                CharMetrics cm = metrics[i];
                int row = i / charsPerRow;
                int col = i % charsPerRow;

                int x = col * (maxCharWidth + pixelPadding) + pixelPadding;
                int y = row * (maxCharHeight + pixelPadding) + pixelPadding + baseAscent;

                Glyph glyph = new Glyph(x, y - baseAscent, cm.width, cm.height, cm.character, this);
                glyphs.put(cm.character, glyph);
                g2d.drawString(String.valueOf(cm.character), x, y);
            }

            g2d.dispose();
            registerBufferedImageTexture(bindToTexture, img);
            generated = true;
        }

        private void registerBufferedImageTexture(Identifier id, BufferedImage img) {
            try (NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, img.getWidth(), img.getHeight(), false)) {
                int[] pixels = new int[img.getWidth() * img.getHeight()];
                img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

                for (int i = 0; i < pixels.length; i++) {
                    int argb = pixels[i];
                    int a = (argb >>> 24);
                    int r = (argb >>> 16) & 0xFF;
                    int g = (argb >>> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setColorArgb(i % img.getWidth(), i / img.getWidth(), abgr);
                }

                NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                texture.upload();
                mc.getTextureManager().registerTexture(id, texture);
            } catch (Throwable ignored) {}
        }
    }

    static MinecraftClient mc = MinecraftClient.getInstance();

    static RenderTickCounter tickCounter() {
        return Holder.tickCounter;
    }

    static Tessellator tessellator() {
        return Holder.tessellator;
    }

    static MinecraftClient getMc() {
        return Holder.minecraftClient;
    }

    static class Holder {
        private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        private static final RenderTickCounter tickCounter = mc.getRenderTickCounter();
        private static final Tessellator tessellator = Tessellator.getInstance();
    }

    private record CharMetrics(char character, int width, int height) {}
}