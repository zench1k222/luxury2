package dev.luxury.utils.font;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.mixin.render.impl.INativeImage;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;


import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.Closeable;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.luxury.utils.math.MathUtil.roundToDecimal;


public class FontHelper implements Closeable {
    private static final Char2IntArrayMap colorCodes = new Char2IntArrayMap() {{
        put('0', 0x000000);
        put('1', 0x0000AA);
        put('2', 0x00AA00);
        put('3', 0x00AAAA);
        put('4', 0xAA0000);
        put('5', 0xAA00AA);
        put('6', 0xFFAA00);
        put('7', 0xAAAAAA);
        put('8', 0x555555);
        put('9', 0x5555FF);
        put('A', 0x55FF55);
        put('B', 0x55FFFF);
        put('C', 0xFF5555);
        put('D', 0xFF55FF);
        put('E', 0xFFFF55);
        put('F', 0xFFFFFF);
    }};
private static MinecraftClient mc = MinecraftClient.getInstance();
    private static final ExecutorService asyncworker = Executors.newCachedThreadPool();
    private final Object2ObjectMap<Identifier, ObjectList<DrawEntry>>glyphpagekey = new Object2ObjectOpenHashMap<>();
    private final float originalsize;
    private final ObjectList<GlyphMap> maps = new ObjectArrayList<>();
    private final Char2ObjectArrayMap<Glyph> allglyphs = new Char2ObjectArrayMap<>();
    private final int charsperpage;
    private final int padding;
    private final String prebakeglyphs;
    private int scalemul = 0;
    private Font font;
    private int previousgamescale = -1;
    private Future<Void> prebakeglyphsfuture;
    private boolean initialized;

    public FontHelper(Font font, float sizePx, int charactersPerPage, int paddingBetweenCharacters, @Nullable String prebakeCharacters) {
        this.originalsize = sizePx;
        this.charsperpage = charactersPerPage;
        this.padding = paddingBetweenCharacters;
        this.prebakeglyphs = prebakeCharacters;
        init(font, sizePx);
    }

    public FontHelper(Font font, float sizePx) {
        this(font, sizePx, 256, 5, null);
    }

    private static int floorNearestMulN(int x, int n) {
        return n * (int) Math.floor((double) x / (double) n);
    }

    public static String stripControlCodes(String text) {
        char[] chars = text.toCharArray();
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '§') {
                i++;
                continue;
            }
            f.append(c);
        }
        return f.toString();
    }

    private void sizeCheck() {
        int gs = (int) mc.getWindow().getScaleFactor();
        if (gs != this.previousgamescale) {
            close();
            init(this.font, this.originalsize);
        }
    }

    public void drawGradientString(MatrixStack stack, String s, float x, float y, int colorLeft, int colorRight) {
        drawGradientString(stack, s, x, y, colorLeft, colorRight, false, 0, 0);
    }

    public void drawGradientString(MatrixStack stack, String s, float x, float y, int color1, int color2,
                                   boolean useWave, int waveSpeed, int waveIndex) {
        if (prebakeglyphsfuture != null && !prebakeglyphsfuture.isDone()) {
            try {
                prebakeglyphsfuture.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }

        sizeCheck();

        stack.push();
        y -= 3f;
        stack.translate(roundToDecimal(x, 1), roundToDecimal(y, 1), 0);
        stack.scale(1f / this.scalemul, 1f / this.scalemul, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
       // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
     //  GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder bb;
        Matrix4f mat = stack.peek().getPositionMatrix();
        char[] chars = stripControlCodes(s).toCharArray();
        float xOffset = 0;
        float yOffset = 0;

        float totalWidth = getStringWidth(s) * scalemul;

        synchronized (glyphpagekey) {
            for (char c : chars) {
                if (c == '\n') {
                    yOffset += getStringHeight(String.valueOf(c)) * scalemul;
                    xOffset = 0;
                    continue;
                }

                Glyph glyph = locateGlyph1(c);
                if (glyph != null) {
                    if (glyph.value() != ' ') {
                        float r, g, b, a;

                        if (useWave && totalWidth > 0) {
                            float position = xOffset / totalWidth;
                            int currentColor = ColorUtil.getWaveGradientAtPosition(color1, color2, waveSpeed, waveIndex, position);

                            a = ((currentColor >> 24) & 0xff) / 255f;
                            r = ((currentColor >> 16) & 0xff) / 255f;
                            g = ((currentColor >> 8) & 0xff) / 255f;
                            b = ((currentColor) & 0xff) / 255f;
                        } else {
                            float progress = totalWidth > 0 ? xOffset / totalWidth : 0;

                            float aL = ((color1 >> 24) & 0xff) / 255f;
                            float rL = ((color1 >> 16) & 0xff) / 255f;
                            float gL = ((color1 >> 8) & 0xff) / 255f;
                            float bL = ((color1) & 0xff) / 255f;

                            float aR = ((color2 >> 24) & 0xff) / 255f;
                            float rR = ((color2 >> 16) & 0xff) / 255f;
                            float gR = ((color2 >> 8) & 0xff) / 255f;
                            float bR = ((color2) & 0xff) / 255f;

                            r = rL + (rR - rL) * progress;
                            g = gL + (gR - gL) * progress;
                            b = bL + (bR - bL) * progress;
                            a = aL + (aR - aL) * progress;
                        }

                        Identifier i1 = glyph.owner().bindToTexture;
                        DrawEntry entry = new DrawEntry(xOffset, yOffset, r, g, b, glyph);
                        glyphpagekey.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                    }
                    xOffset += glyph.width();
                }
            }

            for (Identifier identifier : glyphpagekey.keySet()) {
                RenderSystem.setShaderTexture(0, identifier);
                List<DrawEntry> objects = glyphpagekey.get(identifier);

                bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (DrawEntry object : objects) {
                    float xo = object.atX;
                    float yo = object.atY;
                    float cr = object.r;
                    float cg = object.g;
                    float cb = object.b;

                    float ca;
                    if (useWave && totalWidth > 0) {
                        float position = xo / totalWidth;
                        int currentColor = ColorUtil.getWaveGradientAtPosition(color1, color2, waveSpeed, waveIndex, position);
                        ca = ((currentColor >> 24) & 0xff) / 255f;
                    } else {
                        float progress = totalWidth > 0 ? xo / totalWidth : 0;
                        float aL = ((color1 >> 24) & 0xff) / 255f;
                        float aR = ((color2 >> 24) & 0xff) / 255f;
                        ca = aL + (aR - aL) * progress;
                    }

                    Glyph glyph = object.toDraw;
                    GlyphMap owner = glyph.owner();
                    float w = glyph.width();
                    float h = glyph.height();
                    float u1 = (float) glyph.u() / owner.width;
                    float v1 = (float) glyph.v() / owner.height;
                    float u2 = (float) (glyph.u() + glyph.width()) / owner.width;
                    float v2 = (float) (glyph.v() + glyph.height()) / owner.height;

                    bb.vertex(mat, xo + 0, yo + h, 0).texture(u1, v2).color(cr, cg, cb, ca);
                    bb.vertex(mat, xo + w, yo + h, 0).texture(u2, v2).color(cr, cg, cb, ca);
                    bb.vertex(mat, xo + w, yo + 0, 0).texture(u2, v1).color(cr, cg, cb, ca);
                    bb.vertex(mat, xo + 0, yo + 0, 0).texture(u1, v1).color(cr, cg, cb, ca);
                }
                RenderUtil.endBuilding(bb);
            }

            glyphpagekey.clear();
        }
        stack.pop();
        GlStateManager._disableBlend();
        GlStateManager._enableCull();
    }
    public void drawCenteredGradientString(MatrixStack stack, String s, float x, float y, int colorLeft, int colorRight) {
        drawGradientString(stack, s, x - getStringWidth(s) / 2f, y, colorLeft, colorRight);
    }
    private void init(Font font, float sizePx) {
        if (initialized) throw new IllegalStateException("Double call to init()");
        initialized = true;
        this.previousgamescale = (int) mc.getWindow().getScaleFactor();
        this.scalemul = this.previousgamescale;
        this.font = font.deriveFont(sizePx * this.scalemul);
        if (prebakeglyphs != null && !prebakeglyphs.isEmpty()) {
            prebakeglyphsfuture = this.prebake();
        }
    }

    private Future<Void> prebake() {
        return asyncworker.submit(() -> {
            for (char c : prebakeglyphs.toCharArray()) {
                if (Thread.interrupted()) break;
                locateGlyph1(c);
            }
            return null;
        });
    }

    private GlyphMap generateMap(char from, char to) {
        GlyphMap gm = new GlyphMap(from, to, this.font, randomIdentifier(), padding);
        maps.add(gm);
        return gm;
    }

    private Glyph locateGlyph0(char glyph) {
        for (GlyphMap map : maps) {
            if (map.contains(glyph)) {
                return map.getGlyph(glyph);
            }
        }
        int base = floorNearestMulN(glyph, charsperpage);
        GlyphMap glyphMap = generateMap((char) base, (char) (base + charsperpage));
        return glyphMap.getGlyph(glyph);
    }

    @Nullable
    private Glyph locateGlyph1(char glyph) {
        return allglyphs.computeIfAbsent(glyph, this::locateGlyph0);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, int color) {
        float r = ((color >> 16) & 0xff) / 255f;
        float g = ((color >> 8) & 0xff) / 255f;
        float b = ((color) & 0xff) / 255f;
        float a = ((color >> 24) & 0xff) / 255f;
        drawString(stack, s, (float) x, (float) y, r, g, b, a);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, Color color) {
        drawString(stack, s, (float) x, (float) y, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha());
    }

    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        drawString(stack, s, x, y, r, g, b, a, false, 0);
    }

    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean gradient, int offset) {
        if (prebakeglyphsfuture != null && !prebakeglyphsfuture.isDone()) {
            try {
                prebakeglyphsfuture.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }

        sizeCheck();
        float r2 = r, g2 = g, b2 = b;
        stack.push();
        y -= 3f;
        stack.translate(roundToDecimal(x, 1), roundToDecimal(y, 1), 0);
        stack.scale(1f / this.scalemul, 1f / this.scalemul, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder bb;
        Matrix4f mat = stack.peek().getPositionMatrix();
        char[] chars = s.toCharArray();
        float xOffset = 0;
        float yOffset = 0;
        boolean inSel = false;
        int lineStart = 0;
        synchronized (glyphpagekey) {
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (inSel) {
                    inSel = false;
                    char c1 = Character.toUpperCase(c);
                    if (colorCodes.containsKey(c1)) {
                        int ii = colorCodes.get(c1);
                        int[] col = RGBIntToRGB(ii);
                        r2 = col[0] / 255f;
                        g2 = col[1] / 255f;
                        b2 = col[2] / 255f;
                    } else if (c1 == 'R') {
                        r2 = r;
                        g2 = g;
                        b2 = b;
                    }
                    continue;
                }

                if (c == '§') {
                    inSel = true;
                    continue;
                } else if (c == '\n') {
                    yOffset += getStringHeight(s.substring(lineStart, i)) * scalemul;
                    xOffset = 0;
                    lineStart = i + 1;
                    continue;
                }

                if (gradient) {
                    try {
                        float hue = ((i * (long) offset) % 360L) / 360f;
                        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);

                        r2 = color.getRed() / 255f;
                        g2 = color.getGreen() / 255f;
                        b2 = color.getBlue() / 255f;
                    } catch (Exception e) {
                        r2 = 1.0f;
                        g2 = 1.0f;
                        b2 = 1.0f;
                    }
                }
                Glyph glyph = locateGlyph1(c);
                if (glyph != null) {
                    if (glyph.value() != ' ') {
                        Identifier i1 = glyph.owner().bindToTexture;
                        DrawEntry entry = new DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                        glyphpagekey.computeIfAbsent(i1, integer -> new ObjectArrayList<>()).add(entry);
                    }
                    xOffset += glyph.width();
                }
            }

            for (Identifier identifier : glyphpagekey.keySet()) {
                RenderSystem.setShaderTexture(0, identifier);
                List<DrawEntry> objects = glyphpagekey.get(identifier);

                bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (DrawEntry object : objects) {
                    float xo = object.atX;
                    float yo = object.atY;
                    float cr = object.r;
                    float cg = object.g;
                    float cb = object.b;
                    Glyph glyph = object.toDraw;
                    GlyphMap owner = glyph.owner();
                    float w = glyph.width();
                    float h = glyph.height();
                    float u1 = (float) glyph.u() / owner.width;
                    float v1 = (float) glyph.v() / owner.height;
                    float u2 = (float) (glyph.u() + glyph.width()) / owner.width;
                    float v2 = (float) (glyph.v() + glyph.height()) / owner.height;

                    bb.vertex(mat, xo + 0, yo + h, 0).texture(u1, v2).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + w, yo + h, 0).texture(u2, v2).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + w, yo + 0, 0).texture(u2, v1).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + 0, yo + 0, 0).texture(u1, v1).color(cr, cg, cb, a);
                }
                RenderUtil.endBuilding(bb);
            }

            glyphpagekey.clear();
        }
        stack.pop();
        GlStateManager._disableBlend();
        GlStateManager._enableCull();
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
        float r = ((color >> 16) & 0xff) / 255f;
        float g = ((color >> 8) & 0xff) / 255f;
        float b = ((color) & 0xff) / 255f;
        float a = ((color >> 24) & 0xff) / 255f;
        drawString(stack, s, (float) (x - getStringWidth(s) / 2f), (float) y, r, g, b, a);
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, Color color) {
        drawString(stack, s, (float) (x - getStringWidth(s) / 2f), (float) y, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public void drawCenteredString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        drawString(stack, s, x - getStringWidth(s) / 2f, y, r, g, b, a);
    }

    public float getStringWidth(String text) {
        char[] c = stripControlCodes(text).toCharArray();
        float currentLine = 0;
        float maxPreviousLines = 0;
        for (char c1 : c) {
            if (c1 == '\n') {
                maxPreviousLines = Math.max(currentLine, maxPreviousLines);
                currentLine = 0;
                continue;
            }
            Glyph glyph = locateGlyph1(c1);
            currentLine += glyph == null ? 0 : (glyph.width() / (float) this.scalemul);
        }
        return Math.max(currentLine, maxPreviousLines);
    }

    public float getStringHeight(String text) {
        char[] c = stripControlCodes(text).toCharArray();
        if (c.length == 0) {
            c = new char[]{' '};
        }
        float currentLine = 0;
        float previous = 0;
        for (char c1 : c) {
            if (c1 == '\n') {
                if (currentLine == 0) {
                    currentLine = (locateGlyph1(' ') == null ? 0 : (Objects.requireNonNull(locateGlyph1(' ')).height() / (float) this.scalemul));
                }
                previous += currentLine;
                currentLine = 0;
                continue;
            }
            Glyph glyph = locateGlyph1(c1);
            currentLine = Math.max(

                    glyph == null ? 0 : (glyph.height() / (float) this.scalemul)

                    , currentLine);
        }
        return currentLine + previous;
    }


    @Override
    public void close() {
        try {
            if (prebakeglyphsfuture != null && !prebakeglyphsfuture.isDone() && !prebakeglyphsfuture.isCancelled()) {
                prebakeglyphsfuture.cancel(true);
                prebakeglyphsfuture.get();
                prebakeglyphsfuture = null;
            }
            for (GlyphMap map : maps) {
                map.destroy();
            }
            maps.clear();
            allglyphs.clear();
            initialized = false;
        } catch (Exception ignored) {
        }
    }
    record Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {
    }

    class GlyphMap {
        final char fromIncl, toExcl;
        final Font font;
        final Identifier bindToTexture;
        final int pixelPadding;
        private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap<>();
        int width, height;

        boolean generated = false;

        public GlyphMap(char from, char to, Font font, Identifier identifier, int padding) {
            this.fromIncl = from;
            this.toExcl = to;
            this.font = font;
            this.bindToTexture = identifier;
            this.pixelPadding = padding;
        }

        public Glyph getGlyph(char c) {
            if (!generated) {
                generate();
            }
            return glyphs.get(c);
        }

        public void destroy() {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(this.bindToTexture);
            this.glyphs.clear();
            this.width = -1;
            this.height = -1;
            generated = false;
        }

        public boolean contains(char c) {
            return c >= fromIncl && c < toExcl;
        }

        private Font getFontForGlyph(char c) {
            if (font.canDisplay(c)) {
                return font;
            }
            return this.font;
        }

        public void generate() {
            if (generated) {
                return;
            }
            int range = toExcl - fromIncl - 1;
            int charsVert = (int) (Math.ceil(Math.sqrt(range)) * 1.5);
            glyphs.clear();
            int generatedChars = 0;
            int charNX = 0;
            int maxX = 0, maxY = 0;
            int currentX = 0, currentY = 0;
            int currentRowMaxY = 0;
            List<Glyph> glyphs1 = new ArrayList<>();
            AffineTransform af = new AffineTransform();
            FontRenderContext frc = new FontRenderContext(af, true, false);
            while (generatedChars <= range) {
                char currentChar = (char) (fromIncl + generatedChars);
                Font font = getFontForGlyph(currentChar);
                Rectangle2D stringBounds = font.getStringBounds(String.valueOf(currentChar), frc);

                int width = (int) Math.ceil(stringBounds.getWidth());
                int height = (int) Math.ceil(stringBounds.getHeight());
                generatedChars++;
                maxX = Math.max(maxX, currentX + width);
                maxY = Math.max(maxY, currentY + height);
                if (charNX >= charsVert) {
                    currentX = 0;
                    currentY += currentRowMaxY + pixelPadding;
                    charNX = 0;
                    currentRowMaxY = 0;
                }
                currentRowMaxY = Math.max(currentRowMaxY, height);
                glyphs1.add(new Glyph(currentX, currentY, width, height, currentChar, this));
                currentX += width + pixelPadding;
                charNX++;
            }
            BufferedImage bi = new BufferedImage(Math.max(maxX + pixelPadding, 1), Math.max(maxY + pixelPadding, 1),
                    BufferedImage.TYPE_INT_ARGB);
            width = bi.getWidth();
            height = bi.getHeight();
            Graphics2D g2d = bi.createGraphics();
            g2d.setColor(new Color(255, 255, 255, 0));
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.WHITE);

            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            for (Glyph glyph : glyphs1) {
                g2d.setFont(getFontForGlyph(glyph.value()));
                FontMetrics fontMetrics = g2d.getFontMetrics();
                g2d.drawString(String.valueOf(glyph.value()), glyph.u(), glyph.v() + fontMetrics.getAscent());
                glyphs.put(glyph.value(), glyph);
            }
            registerBufferedImageTexture(bindToTexture, bi);
            generated = true;
        }

        public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
            try {
                int ow = bi.getWidth();
                int oh = bi.getHeight();
                NativeImage image = new NativeImage(NativeImage.Format.RGBA, ow, oh, false);
                @SuppressWarnings("DataFlowIssue") long ptr = ((INativeImage) (Object) image).getPointer();
                IntBuffer backingBuffer = MemoryUtil.memIntBuffer(ptr, image.getWidth() * image.getHeight());
                int off = 0;
                Object _d;
                WritableRaster _ra = bi.getRaster();
                ColorModel _cm = bi.getColorModel();
                int nbands = _ra.getNumBands();
                int dataType = _ra.getDataBuffer().getDataType();
                _d = switch (dataType) {
                    case DataBuffer.TYPE_BYTE -> new byte[nbands];
                    case DataBuffer.TYPE_USHORT -> new short[nbands];
                    case DataBuffer.TYPE_INT -> new int[nbands];
                    case DataBuffer.TYPE_FLOAT -> new float[nbands];
                    case DataBuffer.TYPE_DOUBLE -> new double[nbands];
                    default -> throw new IllegalArgumentException("Unknown data buffer type: " +
                            dataType);
                };

                for (int y = 0; y < oh; y++) {
                    for (int x = 0; x < ow; x++) {
                        _ra.getDataElements(x, y, _d);
                        int a = _cm.getAlpha(_d);
                        int r = _cm.getRed(_d);
                        int g = _cm.getGreen(_d);
                        int b = _cm.getBlue(_d);
                        int abgr = a << 24 | b << 16 | g << 8 | r;
                        backingBuffer.put(abgr);
                    }
                }
                NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
                tex.upload();
                if (RenderSystem.isOnRenderThread()) {
                    MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex);
                } else {
                    RenderSystem.recordRenderCall(() -> MinecraftClient.getInstance().getTextureManager().registerTexture(i, tex));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    public class Texture {
        final Identifier id;

        public Texture(String path) {
            id = Identifier.of("luxury", validatePath(path));
        }

        public Texture(Identifier i) {
            id = Identifier.of(i.getNamespace(), i.getPath());
        }

        String validatePath(String path) {
            if (Identifier.isPathValid(path)) {
                return path;
            }
            StringBuilder ret = new StringBuilder();
            for (char c : path.toLowerCase().toCharArray()) {
                if (Identifier.isPathCharacterValid(c)) {
                    ret.append(c);
                }
            }
            return ret.toString();
        }

        public Identifier getId() {
            return id;
        }
    }
    @Contract(value = "-> new", pure = true)
    public static @NotNull Identifier randomIdentifier() {
        return Identifier.of("luxury", "temp/" + randomString());
    }

    private static String randomString() {
        return IntStream.range(0, 32).mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1))).collect(Collectors.joining());
    }

    @Contract(value = "_ -> new", pure = true)
    public static int @NotNull [] RGBIntToRGB(int in) {
        int red = in >> 8 * 2 & 0xFF;
        int green = in >> 8 & 0xFF;
        int blue = in & 0xFF;
        return new int[]{red, green, blue};
    }

    public float getFontHeight(String str) {
        return getStringHeight(str);
    }





    record DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {
    }
}
