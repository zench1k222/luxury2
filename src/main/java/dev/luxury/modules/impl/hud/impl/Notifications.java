package dev.luxury.modules.impl.hud.impl;

import dev.luxury.modules.impl.hud.api.DraggableHudElement;
import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Notifications extends DraggableHudElement {

    private final List<PreviewNotification> previewNotifications = new ArrayList<>();
    private FontDraw fontDraw;
    private boolean initialized = false;

    public Notifications(String name, float x, float y) {
        super(name, x, y);
        this.width = 220;
        this.height = 150;

        createPreviewNotifications();
        initializeFont();
    }

    private void initializeFont() {
        if (!initialized && FontHelper.sfprobold != null && FontHelper.sfprobold[12] != null) {
            fontDraw = FontHelper.sfprobold[12];
            initialized = true;
        }
    }

    private void createPreviewNotifications() {
        previewNotifications.clear();
        previewNotifications.add(new PreviewNotification("§a✓ Метка 'home' добавлена", 0.8f, 0x00FF00));
        previewNotifications.add(new PreviewNotification("§c✗ Метка не найдена", 0.6f, 0xFF0000));
        previewNotifications.add(new PreviewNotification("§e⚠ Низкое здоровье", 0.4f, 0xFFFF00));
        previewNotifications.add(new PreviewNotification("§bℹ Новое обновление", 0.2f, 0x00FFFF));
    }

    @Override
    public void render(DrawContext context) {
        MatrixStack matrices = context.getMatrices();

        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            renderPreview(matrices);
        } else {
            renderRealNotifications(matrices);
        }
    }

    private void renderPreview(MatrixStack matrices) {
        matrices.push();
        matrices.translate(x, y, 0);

        int colorstandart = new Color(115, 115, 120, 255).getRGB();
        RenderUtil.drawBlur(matrices,
                0, 0, width, height,
                new Vector4f(8, 8, 8, 8),
                18f, colorstandart);

        String title = "Уведомления (Предпросмотр)";
        if (fontDraw != null) {
            float titleWidth = fontDraw.getWidth(title);
            fontDraw.drawCenteredString(matrices, title, width / 2, 8, 0x80FFFFFF);
        }

        float notificationY = 30;
        float notificationHeight = 25;
        float spacing = 5;

        for (PreviewNotification preview : previewNotifications) {
            renderPreviewNotification(matrices, preview, notificationY, notificationHeight);
            notificationY += notificationHeight + spacing;
        }

        matrices.pop();
    }

    private void renderPreviewNotification(MatrixStack matrices, PreviewNotification preview, float y, float height) {
        float notificationWidth = width - 10;
        float x = 5;

        int notificationBgColor = 0x80000000;
        RenderUtil.drawBlur(matrices,
                x, y, notificationWidth, height,
                new Vector4f(4, 4, 4, 4),
                8f, notificationBgColor);

        RenderUtil.drawBlur(matrices,
                x, y, 3, height,
                new Vector4f(1, 0, 0, 1),
                8f, preview.getColorWithAlpha(0xFF));

        String text = preview.getText();
        if (fontDraw != null) {
            float textHeight = fontDraw.getHeight();
            fontDraw.drawString(matrices, text, x + 10, y + (height - textHeight) / 2, 0xFFFFFFFF);
        }

        float progressWidth = (notificationWidth - 6) * preview.getProgress();
        if (progressWidth > 0) {
            RenderUtil.drawBlur(matrices,
                    x + 3, y + height - 3,
                    progressWidth, 2,
                    new Vector4f(1, 1, 1, 1),
                    4f, 0x80AAAAAA);
        }
    }

    private void renderRealNotifications(MatrixStack matrices) {
        matrices.push();
        matrices.translate(x, y, 0);

        int colorstandart = new Color(115, 115, 120, 100).getRGB();
        RenderUtil.drawBlur(matrices,
                0, 0, width, height,
                new Vector4f(8, 8, 8, 8),
                18f, colorstandart);

        matrices.pop();
    }

    private static class PreviewNotification {
        private final String text;
        private final float progress;
        private final int color;

        public PreviewNotification(String text, float progress, int color) {
            this.text = text;
            this.progress = progress;
            this.color = color;
        }

        public String getText() {
            return text;
        }

        public float getProgress() {
            return progress;
        }

        public int getColorWithAlpha(int alpha) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return (alpha << 24) | (r << 16) | (g << 8) | b;
        }
    }
}