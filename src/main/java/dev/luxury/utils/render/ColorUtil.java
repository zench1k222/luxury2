package dev.luxury.utils.render;


import com.mojang.blaze3d.systems.RenderSystem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;

import java.awt.Color;
import java.util.concurrent.*;

public class ColorUtil {
    private static final long CACHE_EXPIRATION_TIME = 60 * 1000;
    private static final ConcurrentHashMap<ColorKey, CacheEntry> colorCache = new ConcurrentHashMap<>();
    private static final DelayQueue<CacheEntry> cleanupQueue = new DelayQueue<>();
    public static int red(int c) {return (c >> 16) & 0xFF;}

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int alpha(int c) {
        return (c >> 24) & 0xFF;
    }

    public float redf(int c) {
        return red(c) / 255.0f;
    }

    public float greenf(int c) {
        return green(c) / 255.0f;
    }

    public float bluef(int c) {
        return blue(c) / 255.0f;
    }

    public float alphaf(int c) {
        return alpha(c) / 255.0f;
    }
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public int[] getRGBA(int c) {
        return new int[]{red(c), green(c), blue(c), alpha(c)};
    }

    public int[] getRGB(int c) {
        return new int[]{red(c), green(c), blue(c)};
    }

    public float[] getRGBAf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
    }

    public float[] getRGBf(int c) {
        return new float[]{redf(c), greenf(c), bluef(c)};
    }

    public int getColor(float red, float green, float blue, float alpha) {
        return getColor(Math.round(red * 255), Math.round(green * 255), Math.round(blue * 255), Math.round(alpha * 255));
    }

    public int getColor(int red, int green, int blue, float alpha) {
        return getColor(red, green, blue, Math.round(alpha * 255));
    }
    public static int swapAlpha(int color, int newAlpha) {
        return (color & 0x00FFFFFF) | ((newAlpha & 0xFF) << 24);
    }
    public static int fadeColor(int color1, int color2, float progress, float alpha) {
        progress = Math.max(0, Math.min(1, progress));

        int r1 = red(color1);
        int g1 = green(color1);
        int b1 = blue(color1);
        int a1 = alpha(color1);

        int r2 = red(color2);
        int g2 = green(color2);
        int b2 = blue(color2);
        int a2 = alpha(color2);

        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        int a = (int) ((a1 + (a2 - a1) * progress) * alpha);

        return getColor(r, g, b, a);
    }

    /**
     * Радужный цвет для GUI элементов
     * @param index Индекс элемента (для смещения цвета)
     * @param offset Временное смещение (обычно System.currentTimeMillis())
     * @return Радужный цвет
     */
    public static int rainbowGui(int index, long offset) {
        float hue = ((System.currentTimeMillis() + offset + index * 100) % 3600) / 3600.0f;
        return Color.HSBtoRGB(hue, 0.7f, 1.0f);
    }
    public int getColor(float red, float green, float blue) {
        return getColor(red, green, blue, 1.0F);
    }

    public int getColor(int brightness, int alpha) {
        return getColor(brightness, brightness, brightness, alpha);
    }

    public int getColor(int brightness, float alpha) {
        return getColor(brightness, Math.round(alpha * 255));
    }

    public int getColor(int brightness) {
        return getColor(brightness, brightness, brightness);
    }

    public int replAlpha(int color, int alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public int replAlpha(int color, float alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public static int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }

    public int multColor(int colorStart, int colorEnd, float progress) {
        return getColor(Math.round(red(colorStart) * (redf(colorEnd) * progress)), Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
                Math.round(blue(colorStart) * (bluef(colorEnd) * progress)), Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress)));
    }

    public int multRed(int colorStart, int colorEnd, float progress) {
        return getColor(Math.round(red(colorStart) * (redf(colorEnd) * progress)), Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
                Math.round(blue(colorStart) * (bluef(colorEnd) * progress)), Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress)));
    }
    public static int getCircularGradient(int baseColor, int movingColor, float radius, float x, float y, float centerX, float centerY, int speed) {
        // Угол анимации (по кругу по часовой стрелке)
        float angle = (System.currentTimeMillis() % speed) / (float) speed * 360f;

        // Координаты вращающегося цвета
        double rad = Math.toRadians(angle);
        float movingX = (float) (centerX + Math.cos(rad) * radius);
        float movingY = (float) (centerY + Math.sin(rad) * radius);

        // Расстояние от текущей точки до "движущегося" цвета
        float dist = (float) Math.sqrt(Math.pow(x - movingX, 2) + Math.pow(y - movingY, 2));

        // Определяем плавное смешивание
        float mix = MathHelper.clamp(1.0f - dist / radius, 0.0f, 1.0f);

        return overCol(baseColor, movingColor, mix);
    }
    public static int[] getCircularGradient(int color1, int color2, int speed, int index) {
        // угол вращения (0–360°)
        float angle = (System.currentTimeMillis() % speed) / (float) speed * 360f + index * 10f;

        // конвертируем угол в синус/косинус
        float x = (float) Math.cos(Math.toRadians(angle)) * 0.5f + 0.5f;
        float y = (float) Math.sin(Math.toRadians(angle)) * 0.5f + 0.5f;

        // получаем цвета для "верхнего-левого" и "нижнего-правого" угла
        int leftColor = overCol(color1, color2, x);
        int rightColor = overCol(color1, color2, y);

        return new int[]{leftColor, rightColor};
    }
    public static int[] getCircularRotatingGradient4(int color1, int color2, int speed, int index) {
        float angle = (System.currentTimeMillis() % speed) / (float) speed * 360f + index * 10f;

        float cos = (float) Math.cos(Math.toRadians(angle));
        float sin = (float) Math.sin(Math.toRadians(angle));

        float tlMix = (cos + sin + 2f) / 4f;
        float trMix = (-cos + sin + 2f) / 4f;
        float blMix = (cos - sin + 2f) / 4f;
        float brMix = (-cos - sin + 2f) / 4f;

        int topLeft = overCol(color1, color2, tlMix);
        int topRight = overCol(color1, color2, trMix);
        int bottomLeft = overCol(color1, color2, blMix);
        int bottomRight = overCol(color1, color2, brMix);

        return new int[]{topLeft, topRight, bottomLeft, bottomRight};
    }

    public static float[] toRGBAf(int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new float[]{r, g, b, a};
    }

    public int multDark(int color, float percent01) {
        return getColor(
                Math.round(red(color) * percent01),
                Math.round(green(color) * percent01),
                Math.round(blue(color) * percent01),
                alpha(color)
        );
    }
    public static int changeAlpha(int color, int newAlpha) {
        newAlpha = MathHelper.clamp(newAlpha, 0, 255);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
    public static int overCol(int color1, int color2, float percent01) {
        final float percent = MathHelper.clamp(percent01, 0F, 1F);
        return getColor(
                MathHelper.lerp(percent, red(color1), red(color2)),
                MathHelper.lerp(percent, green(color1), green(color2)),
                MathHelper.lerp(percent, blue(color1), blue(color2)),
                MathHelper.lerp(percent, alpha(color1), alpha(color2))
        );
    }
    public int multBright(int color, float percent01) {
        return getColor(
                Math.min(255, Math.round(red(color) / percent01)),
                Math.min(255, Math.round(green(color) / percent01)),
                Math.min(255, Math.round(blue(color) / percent01)),
                alpha(color)
        );
    }



    public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
        return new Vector4i(multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha));
    }

    public static int multRedAndAlpha(int color, float red, float alpha) {
        return getColor(red(color),Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
    }

    public static int multRed(int color, float percent01) {
        return getColor(red(color),Math.min(255, Math.round(green(color) / percent01)), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
    }
    public static int fade(int speed, int index, int first, int second) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = angle >= 180 ? 360 - angle : angle;
        return overCol(first, second, angle / 180f);
    }
    public static int fade(int index) {
        Color clientColor = new Color(getClientColor());
        return fade(8, index, clientColor.brighter().getRGB(), clientColor.darker().getRGB());
    }

    public int multGreen(int color, float percent01) {
        return getColor(Math.min(255, Math.round(green(color) / percent01)), green(color), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
    }

    public static int[] getAnimatedGradient(int color1, int color2, int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float progress = (float) Math.sin(Math.toRadians(angle)) * 0.5f + 0.5f; // 0.0 to 1.0

        int leftColor = overCol(color1, color2, progress);
        int rightColor = overCol(color2, color1, progress);

        return new int[]{leftColor, rightColor};
    }

    public static int[] getAnimatedGradient(int color1, int color2) {
        return getAnimatedGradient(color1, color2, 2000, 0);
    }

    public static int getWaveGradientAtPosition(int color1, int color2, int speed, int index, float position) {
        float timeOffset = ((System.currentTimeMillis() + index * 100) % speed) / (float) speed * 2.0f;

        float wavePosition = (position - timeOffset) % 2.0f;
        if (wavePosition < 0.0f) wavePosition += 2.0f;

        if (wavePosition <= 1.0f) {
            return overCol(color1, color2, wavePosition);
        } else {
            return overCol(color2, color1, wavePosition - 1.0f);
        }
    }


    public static int[] getWaveGradient(int color1, int color2, int speed, int index) {
        int leftColor = getWaveGradientAtPosition(color1, color2, speed, index, 0.0f);
        int rightColor = getWaveGradientAtPosition(color1, color2, speed, index, 1.0f);
        return new int[]{leftColor, rightColor};
    }

    public static int[] getWaveGradient(int color1, int color2) {
        return getWaveGradient(color1, color2, 2000, 0);
    }

    public static int[] getStaticGradient(int color1, int color2) {
        return new int[]{color1, color2};
    }




    public static int getColor(int red, int green, int blue, int alpha) {
        ColorKey key = new ColorKey(red, green, blue, alpha);
        CacheEntry cacheEntry = colorCache.computeIfAbsent(key, k -> {
            CacheEntry newEntry = new CacheEntry(k, computeColor(red, green, blue, alpha), CACHE_EXPIRATION_TIME);
            cleanupQueue.offer(newEntry);
            return newEntry;
        });
        return cacheEntry.getColor();
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    private static int computeColor(int red, int green, int blue, int alpha) {
        return ((MathHelper.clamp(alpha, 0, 255) << 24) |
                (MathHelper.clamp(red, 0, 255) << 16) |
                (MathHelper.clamp(green, 0, 255) << 8) |
                MathHelper.clamp(blue, 0, 255));
    }

    private String generateKey(int red, int green, int blue, int alpha) {
        return red + "," + green + "," + blue + "," + alpha;
    }

    public String formatting(int color) {
        return "⏏" + color + "⏏";
    }

    public static float[] rgba(final int color) {
        return new float[] {
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class ColorKey {
        final int red, green, blue, alpha;
    }

    @Getter
    private static class CacheEntry implements Delayed {
        private final ColorKey key;
        private final int color;
        private final long expirationTime;

        CacheEntry(ColorKey key, int color, long ttl) {
            this.key = key;
            this.color = color;
            this.expirationTime = System.currentTimeMillis() + ttl;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = expirationTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof CacheEntry) {
                return Long.compare(this.expirationTime, ((CacheEntry) other).expirationTime);
            }
            return 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

    }



    public int getText() {return new Color(0xE6E6E6).getRGB();}



    public int getFriendColor() {
        return new Color(0x55FF55).getRGB();
    }
    public static int getClientColor() {
        return 0xE6E6E6;
    }

    public int getOutline() {
        return new Color(0x373746).getRGB();
    }

    public static int getColorStyle(float index) {
        return getColorStyle((int) index);
    }

    public static int getColorStyle(float index, float alpha) {
        return getColorStyle((int) index, (int) alpha);
    }

    public static int getColorStyle(int index) {
        // Используем градиент между двумя цветами клиента
        int color1 = 0xFF6B9EFF; // Синий
        int color2 = 0xFFFF6B9E; // Розовый
        return gradient(5, index, color1, color2);
    }

    public static int getColorStyle(int index, int alpha) {
        int gradientColor = getColorStyle(index);
        int red = (gradientColor >> 16) & 0xFF;
        int green = (gradientColor >> 8) & 0xFF;
        int blue = gradientColor & 0xFF;
        return new Color(red, green, blue, alpha).getRGB();
    }

    // ДОБАВЬ ЭТИ МЕТОДЫ В КОНЕЦ КЛАССА

    public static int getColorWithAlpha(int color, float alpha) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alphaInt = (int) (alpha * 255);
        return getColor(red, green, blue, alphaInt);
    }

    public static int applyAlpha(int color, int alpha) {
        Color c = new Color(color, true);
        int newAlpha = (int) ((c.getAlpha() / 255.0) * alpha);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, newAlpha)).getRGB();
    }

    public static int applyAlpha(int color, float alpha) {
        Color c = new Color(color, true);
        int newAlpha = (int) ((c.getAlpha() / 255.0) * (alpha * 255));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, newAlpha)).getRGB();
    }

    // Метод для создания цвета из java.awt.Color
    public static int fromAwtColor(java.awt.Color color) {
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    // Быстрые методы для часто используемых цветов
    public static int getTextColor() {
        return getColor(230, 230, 230, 255);
    }

    public static int getSecondaryTextColor() {
        return getColor(150, 150, 150, 255);
    }

    public static int getButtonColor() {
        return getColor(50, 50, 50, 180);
    }

    public static int getButtonHoverColor() {
        return getColor(60, 60, 60, 200);
    }

    public static int getAccentColor() {
        return getColor(1, 235, 1, 200);
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360f * colors.length - colorIndex);
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = MathHelper.clamp(amount, 0, 1);

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = (int) MathHelper.lerp(amount, red1, red2);
        int interpolatedGreen = (int) MathHelper.lerp(amount, green1, green2);
        int interpolatedBlue = (int) MathHelper.lerp(amount, blue1, blue2);
        int interpolatedAlpha = (int) MathHelper.lerp(amount, alpha1, alpha2);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }
}