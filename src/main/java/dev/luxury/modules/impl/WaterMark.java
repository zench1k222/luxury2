package dev.luxury.modules.impl;

import dev.luxury.User;
import dev.luxury.events.impl.render.EventRender2D;

import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.render.GifUtils;
import dev.luxury.utils.render.RenderUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.joml.Vector4f;

import java.awt.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WaterMark {
    MinecraftClient mc = MinecraftClient.getInstance();
    ClientPlayerEntity player = mc.player;

    public void render(EventRender2D e) {

        String timetext = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String userText = " " + User.user + " |  ";
        String serverIpText = " " + getServerIp();
        String pingText = " " + getPing();
        String fpsText = " " + mc.getCurrentFps() + " fps ";
        String info2 = getPing();

        String[] icons = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        float iconOffSet = 12f;
        float info2X = 50f;
        float info2Y = 20;
        float padding = 4f;
        float infoX = 42f;
        float infoY = 6.5f;
        float infoX2 =
                42f;
        float infoY2 = 23.5f;
        float infoX3 = 110;
        float infoY3 = 23.5f;
        float infoX4 = 155;
        float infoY4 = 27f;
        String clientName = "Luxury 1.21.4" + " | ";


        String info = clientName + icons[0] + userText + "       " + fpsText;
        float totalWidth = FontDraw.Montserrat_Medium.getStringWidth(clientName)
                + FontDraw.icons.getStringWidth(icons[0])
                + FontDraw.Montserrat_Medium.getStringWidth(userText)
                + FontDraw.icons.getStringWidth(icons[1])
                + FontDraw.Montserrat_Medium.getStringWidth(fpsText);
        float totalWidth2 = FontDraw.icons.getStringWidth(icons[5]) + FontDraw.Montserrat_Medium.getStringWidth(serverIpText);
        float totalWidth3 = FontDraw.icons.getStringWidth(icons[2]) + FontDraw.Montserrat_Medium.getStringWidth(pingText);

        float iconWidth = FontDraw.icons.getStringWidth(icons[6]);

        float iconWidth2 = FontDraw.icons.getStringWidth(icons[8]);
        float textWidth2 = FontDraw.Montserrat_Medium.getStringWidth(timetext);
        float iconWidth3 = FontDraw.Montserrat_Medium.getStringWidth(icons[4]);

        float totalWidth4 = iconWidth2 + textWidth2;


        float infoHeight2 = FontDraw.Montserrat_Medium.getFontHeight(clientName);
        float infoHeight = FontDraw.Montserrat_Medium.getFontHeight(clientName);
        float currentX = infoX;
        float currentX2 = infoX2;
        float currentX3 = infoX3;

        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), infoX - padding, infoY - padding, totalWidth + padding * 2, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10a0a0a);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), infoX2 - padding, infoY2 - padding, totalWidth2 + padding * 2, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10a0a0a);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), totalWidth2 + 49, infoY3 - padding, totalWidth3 + padding * 2, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10a0a0a);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), totalWidth2 + totalWidth3 + 59f, 23.5f - padding, totalWidth4 + padding * 2 + 3, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10a0a0a);

        GifUtils.renderGif(e.getDrawContext().getMatrices(), "images/gif/736f8af249d0499a829b4150935eb0fc5oazoi7rhm7dedmx-", (int) 2.5f, (int) 2.5f, (int)34.5, (int)34.5, new Vector4f(5, 5, 5, 5), 249);
        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), clientName + "", currentX, 8f, Color.yellow.getRGB());
        currentX += FontDraw.Montserrat_Medium.getStringWidth(clientName);

        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[0], currentX, 9.2f, Color.yellow.getRGB());
        currentX += FontDraw.icons.getStringWidth(icons[0]);

        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), userText, currentX, 8f, Color.yellow.getRGB());
        currentX += FontDraw.Montserrat_Medium.getStringWidth(userText);

        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[4], currentX, 9.5f, Color.yellow.getRGB());
        currentX += FontDraw.icons.getStringWidth(icons[4]);

        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), fpsText, currentX, 8f, Color.yellow.getRGB());
        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[1], currentX2, 26.5f, Color.yellow.getRGB());
        currentX2 += FontDraw.icons.getStringWidth(icons[1]);

        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), serverIpText, currentX2, 25f, Color.yellow.getRGB());
        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[2], totalWidth2 + 52f, 26f, Color.yellow.getRGB());
        currentX3 += FontDraw.icons.getStringWidth(icons[2]);

        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), pingText, totalWidth2 + 61f, 25f, Color.yellow.getRGB());
        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[8], totalWidth2 + totalWidth3 + 62f, 26, Color.yellow.getRGB());
        currentX3 += FontDraw.icons.getStringWidth(icons[8]);

        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), timetext, totalWidth2 + totalWidth3 + 74f, 25f, Color.yellow.getRGB());



    }


    private String getServerIp() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (mc.isIntegratedServerRunning() || mc.getNetworkHandler() == null) {
                return "localhost";
            }
            SocketAddress addr = mc.getNetworkHandler().getConnection().getAddress();
            if (addr instanceof InetSocketAddress) {
                String host = ((InetSocketAddress) addr).getHostString();
                if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
                return host != null && !host.isEmpty() ? host : "localhost";
            } else if (addr != null) {
                String s = addr.toString();
                if (s.startsWith("/")) s = s.substring(1);
                if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
                return s;
            } else return "localhost";
        } catch (Exception ex) {
            return "localhost";
        }
    }


    private String getPing() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                int ping = mc.getNetworkHandler()
                        .getPlayerListEntry(mc.player.getUuid())
                        .getLatency();
                return ping + " ms ";
            }
        } catch (Exception ignored) {
        }
        return "0ms";
    }
}


