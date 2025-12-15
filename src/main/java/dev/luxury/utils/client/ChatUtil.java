package dev.luxury.utils.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.awt.*;

public class ChatUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void sendChat(String message) {
        if (mc.player == null) return;

        String prefix = "§eLuxury Free";
        String arrow = " §r» ";

        String text = prefix + arrow + message;

        mc.player.sendMessage(Text.literal(text), false);
    }
    //Похуй, пусть так будет. Я не понимаю как ебаный градиент сделать из 1.16.5

//    public static MutableText gradient(String text, int startRGB, int endRGB) {
//        MutableText result = Text.literal("");
//
//        for (int i = 0; i < text.length(); i++) {
//            float ratio = i / (float) (text.length() - 1);
//
//            int startR = (startRGB >> 16) & 0xFF;
//            int startG = (startRGB >> 8) & 0xFF;
//            int startB = startRGB & 0xFF;
//
//            int endR = (endRGB >> 16) & 0xFF;
//            int endG = (endRGB >> 8) & 0xFF;
//            int endB = endRGB & 0xFF;
//
//            int red = (int) (startR + ratio * (endR - startR));
//            int green = (int) (startG + ratio * (endG - startG));
//            int blue = (int) (startB + ratio * (endB - startB));
//
//            String hex = String.format("%02x%02x%02x", red, green, blue);
//            result.append(Text.literal("§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" + hex.charAt(2) +
//                    "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5) + text.charAt(i)));
//        }
//
//        return result;
//    }
    //Я хотел тут сделать типо ргб от желтого к ярко желтому но у меня нихуя не получается, даже нейронка не может помочь.
}
