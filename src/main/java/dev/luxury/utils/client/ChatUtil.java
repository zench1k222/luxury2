package dev.luxury.utils.client;

import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ChatUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static FontDraw chatFont = null;

    static {
        if (FontHelper.sfprobold != null && FontHelper.sfprobold.length > 17) {
            chatFont = FontHelper.sfprobold[17];
        } else if (FontHelper.monsterrat != null && FontHelper.monsterrat.length > 14) {
            chatFont = FontHelper.monsterrat[14];
        }
    }

    public static void sendChat(String message) {
        if (mc.player == null) return;

        String prefix = "§1[§lLuxury Free§r§1] §r» ";
        mc.player.sendMessage(Text.literal(prefix + message), false);
    }

    public static void sendGradientChat(String message, int color1, int color2) {
        if (mc.player == null || mc.inGameHud == null) return;

        mc.inGameHud.getChatHud().addMessage(Text.literal("[Luxury Free] » "));
    }

    public static void drawGradientChatMessage(DrawContext context, String message, float x, float y,
                                               int color1, int color2) {
        if (chatFont != null) {
            chatFont.drawGradientText(context, message, x, y, color1, color2);
        } else {
            // Fallback: рисуем обычный текст если шрифт не загружен
            context.drawText(mc.textRenderer, message, (int)x, (int)y, color1, false);
        }
    }

    public static void drawAnimatedGradientChat(DrawContext context, String message, float x, float y,
                                                int color1, int color2, float time) {
        if (chatFont != null) {
            chatFont.drawAnimatedGradientText(context, message, x, y, color1, color2, time);
        } else {
            context.drawText(mc.textRenderer, message, (int)x, (int)y, color1, false);
        }
    }

    public static void sendError(String message) {
        sendChat("§c" + message);
    }

    public static void sendSuccess(String message) {
        sendChat("§a" + message);
    }

    public static void sendWarning(String message) {
        sendChat("§e" + message);
    }

    public static void sendInfo(String message) {
        sendChat("§9" + message);
    }

    public static void sendLuxury(String message) {
        int color1 = ColorUtil.getColor(0, 200, 255, 255);
        int color2 = ColorUtil.getColor(138, 43, 226, 255);
        sendChat("§1[§lLuxury Free§r§1] §r» §b" + message);
    }
}

