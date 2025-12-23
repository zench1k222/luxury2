package dev.luxury.modules.impl.hud.impl;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.hud.api.DraggableHudElement;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EffectsList extends DraggableHudElement {
    private static final int PADDING = 5;
    private static final int ITEM_HEIGHT = 12; // Изменено с 14 на 12 как в KeyBinds
    private static final int ICON_SIZE = 10;
    private static final long FADE_DURATION = 1000; // 1 секунда для исчезновения

    private final Map<String, EffectInfo> activeEffects = new ConcurrentHashMap<>();
    private final Map<String, Long> effectFadeOut = new ConcurrentHashMap<>();
    private final String[] icons = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R"};
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 500; // Обновление каждые 0.5 секунды

    // Настройки прозрачности
    private float backgroundAlpha = 0.85f; // Прозрачность фона (0.0 - полностью прозрачно, 1.0 - непрозрачно)
    private float elementsAlpha = 0.95f;   // Прозрачность элементов

    public EffectsList(String name, float x, float y) {
        super(name, x, y);
    }

    @Override
    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.getMatrices();

        updateEffects();
        cleanupOldEffects();

        FontDraw titleFont = FontHelper.sfprobold[18];
        FontDraw effectNameFont = FontHelper.sfprobold[16]; // Изменено с 14 на 16
        FontDraw effectDurationFont = FontHelper.sfprobold[13]; // Изменено с 12 на 13
        FontDraw iconsFont = FontHelper.icons[20];

        int textColor1 = Color.WHITE.getRGB();
        int textColor2 = new Color(150, 150, 160).getRGB();
        int accentColor = new Color(45, 125, 255).getRGB();

        List<EffectInfo> sortedEffects = new ArrayList<>(activeEffects.values());
        sortedEffects.sort(Comparator.comparingInt(EffectInfo::getAmplifier).reversed());

        // Рассчитываем размеры как в KeyBinds
        float maxWidth = 120f;
        float maxNameWidth = 0;
        float maxDurationWidth = 0;

        for (EffectInfo effect : sortedEffects) {
            float nameWidth = effectNameFont.getWidth(effect.getName());
            float durationWidth = effectDurationFont.getWidth(effect.getDurationText());
            float totalWidth = PADDING * 2 + ICON_SIZE + 5 + nameWidth + 5 + durationWidth;

            if (totalWidth > maxWidth) {
                maxWidth = totalWidth;
            }
            maxNameWidth = Math.max(maxNameWidth, nameWidth);
            maxDurationWidth = Math.max(maxDurationWidth, durationWidth);
        }

        float width = Math.max(maxWidth, 97.5f); // Минимальная ширина как в KeyBinds
        float startX = this.x;
        float startY = this.y;
        int titleHeight = 14;

        int totalHeight = (PADDING * 2 + titleHeight + sortedEffects.size() * ITEM_HEIGHT - 3);

        this.width = width;
        this.height = totalHeight;

        // ФОН КАК В KEYBINDS С ПРОЗРАЧНОСТЬЮ
        int backgroundColor = getColorWithAlpha(new Color(115, 115, 120), backgroundAlpha);
        RenderUtil.drawBlur(matrices, startX, startY, width, totalHeight,
                new Vector4f(8, 8, 8, 8), 18f, backgroundColor);

        // Заголовок с прозрачностью
        titleFont.drawGradientText(matrices, "Effects",
                startX + PADDING + 13, startY + PADDING - 3,
                textColor1, textColor2);
        iconsFont.drawFontLeft(matrices, icons[10], // Буква "K" для Effects
                startX + 5, startY + 3, accentColor);

        // Разделитель под заголовком с прозрачностью
        RenderUtil.drawBlur(matrices, startX, startY + 14, width, 0.9f,
                new Vector4f(0f, 0f, 0f, 0f), 18f,
                getColorWithAlpha(new Color(60, 60, 70), elementsAlpha));

        // Список эффектов
        int currentY = (int) (startY + PADDING + titleHeight);

        for (EffectInfo effect : sortedEffects) {
            renderEffectEntry(matrices, startX, currentY, width, effect,
                    effectNameFont, effectDurationFont, textColor1, textColor2, accentColor);
            currentY += ITEM_HEIGHT;
        }
    }

    private void renderEffectEntry(MatrixStack matrices, float startX, float currentY,
                                   float width, EffectInfo effect,
                                   FontDraw nameFont, FontDraw durationFont,
                                   int color1, int color2, int accentColor) {

        // Фон для эффекта с прозрачностью и анимацией исчезновения
        float fadeAlpha = getEffectAlpha(effect.getId());
        float finalAlpha = fadeAlpha * elementsAlpha;

        // Только если эффект достаточно видим
        if (finalAlpha < 0.1f) return;

        int bgAlpha = (int)(40 * finalAlpha); // Более прозрачный фон
        RenderUtil.drawBlur(matrices, startX, currentY, width, ITEM_HEIGHT,
                new Vector4f(0, 0, 0, 0), 5f,
                getColorWithAlpha(new Color(40, 40, 45), bgAlpha / 255f));

        // Иконка категории эффекта (используем метод из KeyBinds)
        float iconX = startX + PADDING + 1;
        String effectIcon = getEffectIcon(effect.getId());

        FontDraw effectIconFont = FontHelper.icons[14];
        effectIconFont.drawFontLeft(matrices, effectIcon, iconX, currentY,
                getColorWithAlpha(new Color(45, 125, 255), finalAlpha));

        // Точка после иконки (как в KeyBinds)
        float dotX = iconX + 10;
        float dotY = currentY + 1.5f;
        RenderUtil.drawBlur(matrices, dotX, dotY, 4, 4,
                new Vector4f(1, 1, 1, 1), 18f,
                getColorWithAlpha(effect.getColor(), finalAlpha));

        // Название эффекта с прозрачностью
        float nameX = startX + PADDING + 18;
        float nameY = currentY - 1.5f;

        int nameColor1 = getColorWithAlpha(Color.WHITE, finalAlpha);
        int nameColor2 = getColorWithAlpha(new Color(150, 150, 160), finalAlpha);

        nameFont.drawGradientText(matrices, effect.getName(), nameX, nameY, nameColor1, nameColor2);

        // Длительность эффекта
        String durationText = effect.getDurationText();
        float durationWidth = durationFont.getWidth(durationText);
        float durationX = startX + width - durationWidth - PADDING;
        float durationY = currentY - 1;

        int durationColor = effect.isPermanent() ?
                getColorWithAlpha(new Color(100, 255, 100), finalAlpha) :
                getColorWithAlpha(new Color(255, 200, 100), finalAlpha);

        durationFont.drawFontLeft(matrices, durationText, durationX, durationY, durationColor);

        // Уровень эффекта (если есть)
        if (effect.getAmplifier() > 0) {
            String levelText = getRomanNumeral(effect.getAmplifier() + 1);
            float levelWidth = durationFont.getWidth(levelText);
            float levelX = startX + width - durationWidth - levelWidth - 10;

            int levelColor = getColorWithAlpha(new Color(180, 220, 255), finalAlpha);
            durationFont.drawFontLeft(matrices, levelText, levelX, durationY, levelColor);
        }

        // Прогресс-бар длительности (только для временных эффектов)
        if (!effect.isPermanent()) {
            float progressX = nameX;
            float progressY = currentY + ITEM_HEIGHT - 3;
            float progressWidth = width - PADDING * 2 - 15;
            float progressHeight = 1.5f;

            // Фон прогресс-бара
            RenderUtil.drawBlur(matrices, progressX, progressY, progressWidth, progressHeight,
                    new Vector4f(0.5f, 0.5f, 0.5f, 0.5f), 2f,
                    getColorWithAlpha(new Color(50, 50, 60), finalAlpha * 0.5f));

            // Заполненная часть
            float progress = effect.getProgress();
            float filledWidth = progressWidth * progress;

            if (filledWidth > 0) {
                int progressColor = getProgressColor(progress, finalAlpha);
                RenderUtil.drawBlur(matrices, progressX, progressY, filledWidth, progressHeight,
                        new Vector4f(0.5f, 0.5f, 0.5f, 0.5f), 2f, progressColor);
            }
        }
    }

    // Методы для работы с прозрачностью
    private int getColorWithAlpha(Color color, float alpha) {
        return getColorWithAlpha(color.getRGB(), alpha);
    }

    private int getColorWithAlpha(int rgb, float alpha) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = (int)(alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private String getEffectIcon(String effectId) {
        if (effectId.contains("speed")) return "⚡";
        else if (effectId.contains("strength")) return "💪";
        else if (effectId.contains("jump_boost")) return "👟";
        else if (effectId.contains("regeneration")) return "❤️";
        else if (effectId.contains("resistance")) return "🛡️";
        else if (effectId.contains("fire_resistance")) return "🔥";
        else if (effectId.contains("water_breathing")) return "💧";
        else if (effectId.contains("invisibility")) return "👻";
        else if (effectId.contains("night_vision")) return "👁️";
        else if (effectId.contains("haste")) return "⛏️";
        else if (effectId.contains("mining_fatigue")) return "😴";
        else if (effectId.contains("slowness")) return "🐌";
        else if (effectId.contains("weakness")) return "🤕";
        else if (effectId.contains("poison")) return "☠️";
        else if (effectId.contains("wither")) return "💀";
        else if (effectId.contains("levitation")) return "🔼";
        else if (effectId.contains("glowing")) return "🌟";
        else if (effectId.contains("luck")) return "🍀";
        else if (effectId.contains("unluck")) return "☹️";
        else if (effectId.contains("slow_falling")) return "🍃";
        else if (effectId.contains("conduit_power")) return "🌀";
        else if (effectId.contains("dolphins_grace")) return "🐬";
        else if (effectId.contains("bad_omen")) return "👑";
        else if (effectId.contains("hero_of_the_village")) return "🎖️";
        return "✨";
    }

    private String getRomanNumeral(int number) {
        if (number < 1 || number > 10) return String.valueOf(number);

        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return numerals[number - 1];
    }

    private void updateEffects() {
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) return;
        lastUpdate = currentTime;

        // Получаем текущие эффекты
        Map<String, EffectInfo> newEffects = new HashMap<>();

        mc.player.getStatusEffects().forEach((statusEffectInstance) -> {
            String effectId = statusEffectInstance.getEffectType().value().getTranslationKey();
            String displayName = getEffectDisplayName(effectId);

            int amplifier = statusEffectInstance.getAmplifier();

            int duration = statusEffectInstance.getDuration();
            if (statusEffectInstance.isInfinite()) {
                duration = 32767;
            }

            int color = statusEffectInstance.getEffectType().value().getColor();

            EffectInfo effect = new EffectInfo(
                    effectId,
                    displayName,
                    amplifier,
                    duration,
                    color,
                    currentTime
            );

            // Используем ID эффекта как ключ
            String effectKey = effectId + "_" + amplifier;
            newEffects.put(effectKey, effect);

            // Если эффект уже был активен, обновляем его
            if (activeEffects.containsKey(effectKey)) {
                // Обновляем только длительность и время
                EffectInfo existing = activeEffects.get(effectKey);
                EffectInfo updated = new EffectInfo(
                        effectId,
                        displayName,
                        amplifier,
                        duration,
                        color,
                        existing.getStartTime()
                );
                activeEffects.put(effectKey, updated);
                effectFadeOut.remove(effectKey);
            } else {
                // Новый эффект
                activeEffects.put(effectKey, effect);
            }
        });

        // Помечаем исчезнувшие эффекты для fade-out
        for (String effectKey : activeEffects.keySet()) {
            if (!newEffects.containsKey(effectKey) && !effectFadeOut.containsKey(effectKey)) {
                effectFadeOut.put(effectKey, currentTime);
            }
        }

        // Очищаем fade-out эффекты которые уже полностью исчезли
        effectFadeOut.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > FADE_DURATION
        );
    }

    private void cleanupOldEffects() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = effectFadeOut.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String effectId = entry.getKey();
            long fadeStartTime = entry.getValue();

            if (currentTime - fadeStartTime > FADE_DURATION) {
                activeEffects.remove(effectId);
                iterator.remove();
            }
        }
    }

    private float getEffectAlpha(String effectId) {
        if (!effectFadeOut.containsKey(effectId)) {
            return 1.0f;
        }

        long fadeStartTime = effectFadeOut.get(effectId);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - fadeStartTime;

        if (elapsed >= FADE_DURATION) {
            return 0.0f;
        }

        return 1.0f - (elapsed / (float)FADE_DURATION);
    }

    private String getEffectDisplayName(String effectId) {
        String[] parts = effectId.split("\\.");
        if (parts.length > 0) {
            String name = parts[parts.length - 1];
            name = name.replace("_", " ");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            return name;
        }
        return "Unknown";
    }

    private int getProgressColor(float progress, float alpha) {
        if (progress > 0.5f) {
            // От зелёного к жёлтому
            int green = 255;
            int red = (int)((1.0f - progress) * 2 * 255);
            return getColorWithAlpha(new Color(red, green, 50), alpha);
        } else {
            // От жёлтого к красному
            int red = 255;
            int green = (int)(progress * 2 * 255);
            return getColorWithAlpha(new Color(red, green, 50), alpha);
        }
    }

    // Методы для настройки прозрачности (можно добавить в настройки модуля)
    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = MathHelper.clamp(alpha, 0.0f, 1.0f);
    }

    public void setElementsAlpha(float alpha) {
        this.elementsAlpha = MathHelper.clamp(alpha, 0.0f, 1.0f);
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public float getElementsAlpha() {
        return elementsAlpha;
    }

    // Класс для хранения информации об эффекте
    private static class EffectInfo {
        private final String id;
        private final String name;
        private final int amplifier;
        private final int duration;
        private final int color;
        private final long startTime;

        public EffectInfo(String id, String name, int amplifier, int duration, int color, long startTime) {
            this.id = id;
            this.name = name;
            this.amplifier = amplifier;
            this.duration = duration;
            this.color = color;
            this.startTime = startTime;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getAmplifier() { return amplifier; }
        public int getDuration() { return duration; }
        public int getColor() { return color; }
        public long getStartTime() { return startTime; }

        public boolean isPermanent() {
            return duration >= 32767;
        }

        public float getProgress() {
            if (isPermanent()) return 1.0f;
            if (duration <= 0) return 0.0f;

            long currentTime = System.currentTimeMillis();
            long elapsed = (currentTime - startTime) / 50;

            if (elapsed >= duration) return 0.0f;

            float progress = 1.0f - (elapsed / (float)duration);
            return MathHelper.clamp(progress, 0.0f, 1.0f);
        }

        public String getDurationText() {
            if (isPermanent()) return "∞";

            long currentTime = System.currentTimeMillis();
            long elapsed = (currentTime - startTime) / 50;
            long remaining = Math.max(0, duration - elapsed);

            long seconds = remaining / 20;
            long minutes = seconds / 60;
            seconds = seconds % 60;

            if (minutes > 0) {
                return String.format("%d:%02d", minutes, seconds);
            } else {
                return String.format("%ds", seconds);
            }
        }
    }
}