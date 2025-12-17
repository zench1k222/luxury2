package dev.luxury.utils.notifications;

import dev.luxury.utils.animations.Animation;
import dev.luxury.utils.animations.Easing;
import dev.luxury.utils.render.ColorUtil;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;

public class Notification {
    private final Text text;
    private final Animation animation;
    private final long startTime;
    private final long duration;
    private final SoundEvent sound;
    private boolean playedSound;

    public Notification(Text text, long duration, SoundEvent sound) {
        this.text = text;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.sound = sound;
        this.playedSound = false;

        this.animation = new Animation(400, 1.0, true, Easing.EASE_OUT_BACK);
    }

    public Text getText() {
        return text;
    }

    public String getDisplayText() {
        String textStr = text.getString();
        // Удаляем цветовые коды §, но сохраняем эмодзи и текст
        return textStr.replaceAll("§[0-9a-fklmnor]", "");
    }

    public String getCleanText() {
        String textStr = text.getString();
        return textStr.replaceAll("§[0-9a-fklmnor]", "");
    }

    public long getStartTime() {
        return startTime;
    }

    public float getAnimation() {
        return animation.getValue();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > startTime + duration;
    }

    public boolean shouldRemove() {
        return isExpired() && animation.finished(false);
    }

    public void update() {
        if (isExpired()) {
            animation.update(false);
        } else {
            animation.update(true);
        }

        if (!playedSound && sound != null && MinecraftClient.getInstance().getSoundManager() != null) {
            try {
                MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(sound, 1.0f)
                );
                playedSound = true;
            } catch (Exception e) {
            }
        }
    }

    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return 1.0f - Math.min(1.0f, (float) elapsed / duration);
    }

    public int getColor(float alpha) {
        String cleanText = getCleanText();

        if (cleanText.contains("✓") || cleanText.contains("Успех") || cleanText.contains("добавлена") || cleanText.contains("сломан")) {
            return ColorUtil.getColorWithAlpha(0x00FF00, alpha);
        } else if (cleanText.contains("✗") || cleanText.contains("Ошибка")) {
            return ColorUtil.getColorWithAlpha(0xFF0000, alpha);
        } else if (cleanText.contains("⚠") || cleanText.contains("Предупреждение")) {
            return ColorUtil.getColorWithAlpha(0xFFFF00, alpha);
        } else if (cleanText.contains("ℹ") || cleanText.contains("Информация")) {
            return ColorUtil.getColorWithAlpha(0x00FFFF, alpha);
        } else {
            return ColorUtil.getTextColor();
        }
    }

    public int getAccentColor(float alpha) {
        String cleanText = getCleanText();

        if (cleanText.contains("✓") || cleanText.contains("Успех") || cleanText.contains("сломан")) {
            return ColorUtil.getColorWithAlpha(0x00FF00, alpha);
        } else if (cleanText.contains("✗") || cleanText.contains("Ошибка")) {
            return ColorUtil.getColorWithAlpha(0xFF0000, alpha);
        } else if (cleanText.contains("⚠") || cleanText.contains("Предупреждение")) {
            return ColorUtil.getColorWithAlpha(0xFFFF00, alpha);
        } else if (cleanText.contains("ℹ") || cleanText.contains("Информация")) {
            return ColorUtil.getColorWithAlpha(0x00FFFF, alpha);
        } else {
            return ColorUtil.getAccentColor();
        }
    }

    public String getIcon() {
        String cleanText = getCleanText();
        if (cleanText.contains("✓") || cleanText.contains("Успех") || cleanText.contains("сломан")) return "✓";
        if (cleanText.contains("✗") || cleanText.contains("Ошибка")) return "✗";
        if (cleanText.contains("⚠") || cleanText.contains("Предупреждение")) return "⚠";
        if (cleanText.contains("ℹ") || cleanText.contains("Информация")) return "ℹ";
        return "";
    }
}