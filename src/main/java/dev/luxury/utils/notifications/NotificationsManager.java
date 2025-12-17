package dev.luxury.utils.notifications;

import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.modules.impl.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NotificationsManager {
    private static NotificationsManager instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<Notification> pendingRemoval = new ArrayList<>();

    private FontDraw fontDraw;
    private boolean initialized = false;

    private int maxNotifications = 5;
    private int notificationWidth = 220;
    private int notificationHeight = 28;
    private int padding = 5;
    private int spacing = 5;

    private int posX = 5;
    private int posY = 30;

    private long gradientStartTime = System.currentTimeMillis();

    private NotificationsManager() {
        initialize();
    }

    public static NotificationsManager getInstance() {
        if (instance == null) {
            instance = new NotificationsManager();
        }
        return instance;
    }

    private void initialize() {
        if (!initialized && FontHelper.monsterrat != null && FontHelper.monsterrat[12] != null) {
            fontDraw = FontHelper.monsterrat[12];
            initialized = true;
        }
    }

    public void tick() {
        for (Notification notification : notifications) {
            notification.update();

            if (notification.shouldRemove()) {
                pendingRemoval.add(notification);
            }
        }
        notifications.removeAll(pendingRemoval);
        pendingRemoval.clear();

        notifications.sort(Comparator.comparingLong(n -> -n.getStartTime()));

        if (notifications.size() > maxNotifications) {
            notifications.subList(maxNotifications, notifications.size()).clear();
        }
    }

    public void render(DrawContext context) {
        if (notifications.isEmpty() || mc.player == null) return;

        if (!initialized) {
            initialize();
            if (!initialized) return;
        }

        MatrixStack matrices = context.getMatrices();

        float totalOffsetY = 0;

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            float anim = notification.getAnimation();
            if (anim <= 0.01f) continue;

            float x = posX;
            float y = posY + totalOffsetY;

            renderNotification(matrices, notification, x, y, anim, i);

            totalOffsetY += (notificationHeight + spacing) * anim;
        }
    }

    private void renderNotification(MatrixStack matrices, Notification notification, float x, float y, float alpha, int index) {
        int[] gradientColors = ColorUtil.getAnimatedGradient(
                ColorUtil.getColor(20, 20, 25, (int)(200 * alpha)),
                ColorUtil.getColor(30, 30, 35, (int)(200 * alpha)),
                3000,
                index
        );

        RenderUtil.drawRoundedRectGradient(matrices,
                x, y, notificationWidth, notificationHeight,
                new Vector4f(4, 4, 4, 4),
                gradientColors[0], gradientColors[1]);

        int accentColor = notification.getAccentColor(alpha);
        RenderUtil.drawRoundedRect(matrices,
                x, y, 4, notificationHeight,
                new Vector4f(1, 0, 0, 1),
                accentColor);

        RenderUtil.drawBorder(matrices,
                x, y, notificationWidth, notificationHeight,
                new Vector4f(4, 4, 4, 4),
                ColorUtil.getColorWithAlpha(0xFFFFFF, alpha * 0.1f),
                0.5f, 1.0f, 0.5f, false);

        String displayText = notification.getDisplayText();
        int textColor = notification.getColor(alpha);

        if (fontDraw != null) {
            float textWidth = fontDraw.getWidth(displayText);
            float textHeight = fontDraw.getHeight();
            float textX = x + 12;
            float textY = y + (notificationHeight - textHeight) / 2;

            if (textWidth > notificationWidth - 24) {
                displayText = truncateText(displayText, notificationWidth - 24);
            }

            int shadowColor = ColorUtil.applyAlpha(textColor, 0.3f);
            fontDraw.drawString(matrices, displayText, textX + 1, textY + 1, shadowColor);
            fontDraw.drawString(matrices, displayText, textX, textY, textColor);

        }

        float progress = notification.getProgress();
        float progressWidth = (notificationWidth - 8) * progress;

        if (progressWidth > 0) {
            int progressColor1 = ColorUtil.fade(2000, index,
                    ColorUtil.getColor(150, 150, 150, (int)(200 * alpha)),
                    ColorUtil.getColor(200, 200, 200, (int)(200 * alpha))
            );
            int progressColor2 = ColorUtil.fade(2000, index + 1,
                    ColorUtil.getColor(150, 150, 150, (int)(200 * alpha)),
                    ColorUtil.getColor(200, 200, 200, (int)(200 * alpha))
            );

            RenderUtil.drawRoundedRectGradient(matrices,
                    x + 4, y + notificationHeight - 3,
                    progressWidth, 2,
                    new Vector4f(1, 1, 1, 1),
                    progressColor1, progressColor2);
        }

        String icon = notification.getIcon();
        if (fontDraw != null && !icon.isEmpty()) {
            float iconX = x + 4;
            float iconY = y + (notificationHeight - fontDraw.getHeight()) / 2;
            fontDraw.drawString(matrices, icon, iconX, iconY, accentColor);
        }
    }

    private String truncateTextFallback(String text, int maxWidth) {
        while (mc.textRenderer.getWidth(text + "...") > maxWidth && text.length() > 3) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private String getNotificationIcon(String text) {
        if (text.contains("✓") || text.contains("Успех")) return "✓";
        if (text.contains("✗") || text.contains("Ошибка")) return "✗";
        if (text.contains("⚠") || text.contains("Предупреждение")) return "⚠";
        if (text.contains("ℹ") || text.contains("Информация")) return "ℹ";
        return "";
    }

    private String truncateText(String text, int maxWidth) {
        if (fontDraw != null) {
            while (fontDraw.getWidth(text + "...") > maxWidth && text.length() > 3) {
                text = text.substring(0, text.length() - 1);
            }
            return text + "...";
        } else {
            while (mc.textRenderer.getWidth(text + "...") > maxWidth && text.length() > 3) {
                text = text.substring(0, text.length() - 1);
            }
            return text + "...";
        }
    }

    public void addNotification(String text, long duration) {
        addNotification(Text.literal(text), duration, null);
    }

    public void addNotification(Text text, long duration) {
        addNotification(text, duration, null);
    }

    public void addNotification(String text, long duration, SoundEvent sound) {
        addNotification(Text.literal(text), duration, sound);
    }

    public void addNotification(Text text, long duration, SoundEvent sound) {
        Notification notification = new Notification(text, duration, sound);
        notifications.add(notification);
    }


    public void success(String message, long duration) {
        String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "");
        addNotification("§a✓ " + cleanMessage, duration, getSuccessSound());
    }

    public void error(String message, long duration) {
        String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "");
        addNotification("§c✗ " + cleanMessage, duration, getErrorSound());
    }

    public void warning(String message, long duration) {
        String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "");
        addNotification("§e⚠ " + cleanMessage, duration, getWarningSound());
    }

    public void info(String message, long duration) {
        String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "");
        addNotification("§bℹ " + cleanMessage, duration, getInfoSound());
    }

    public void successAnimated(String message, long duration) {
        String animatedMessage = "§a" + ColorUtil.gradient(2000, 0, 0x00FF00, 0x00CC00) + "✓ " + message;
        addNotification(Text.literal(animatedMessage), duration, getSuccessSound());
    }

    public void rainbow(String message, long duration) {
        int rainbowColor = ColorUtil.rainbowGui(0, 0);
        String rainbowText = String.format("§x%06X", rainbowColor) + "✨ " + message;
        addNotification(Text.literal(rainbowText), duration, null);
    }

    public void gradient(String message, long duration, int color1, int color2) {
        int[] gradient = ColorUtil.getAnimatedGradient(color1, color2);
        String gradientText = String.format("§x%06X", gradient[0]) + "➤ " +
                String.format("§x%06X", gradient[1]) + message;
        addNotification(Text.literal(gradientText), duration, null);
    }

    private SoundEvent getSuccessSound() {
        try {
            ClientSounds clientSounds = ClientSounds.getInstance();
            if (clientSounds != null && clientSounds.toggleSound.get()) {
                return getSoundEvent("luxury", "enable");
            }
        } catch (Exception e) {
        }
        return null;
    }

    private SoundEvent getErrorSound() {
        try {
            ClientSounds clientSounds = ClientSounds.getInstance();
            if (clientSounds != null && clientSounds.armorAlertSound.get()) {
                return getSoundEvent("luxury", "armor-alert");
            }
        } catch (Exception e) {
        }
        return null;
    }

    private SoundEvent getWarningSound() {
        try {
            ClientSounds clientSounds = ClientSounds.getInstance();
            if (clientSounds != null && clientSounds.toggleSound.get()) {
                return getSoundEvent("luxury", "disable");
            }
        } catch (Exception e) {
        }
        return null;
    }

    private SoundEvent getInfoSound() {
        try {
            ClientSounds clientSounds = ClientSounds.getInstance();
            if (clientSounds != null && clientSounds.clientStartSound.get()) {
                return getSoundEvent("luxury", "start");
            }
        } catch (Exception e) {
        }
        return null;
    }

    private SoundEvent getSoundEvent(String namespace, String path) {
        try {
            Identifier id = Identifier.of(namespace, path);
            return Registries.SOUND_EVENT.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public void setMaxNotifications(int max) {
        this.maxNotifications = Math.max(1, max);
    }

    public void clearAll() {
        notifications.clear();
        pendingRemoval.clear();
    }

    public int getNotificationCount() {
        return notifications.size();
    }

    public boolean hasNotifications() {
        return !notifications.isEmpty();
    }

    public void showTestNotifications() {
        success("Операция выполнена успешно", 3000);
        error("Произошла ошибка при выполнении", 3000);
        warning("Внимание: низкий заряд батареи", 3000);
        info("Новое обновление доступно", 3000);
        rainbow("Поздравляем с достижением!", 3000);
        gradient("Градиентное уведомление", 3000, 0xFF6B9E, 0xFFE66B);
    }
}