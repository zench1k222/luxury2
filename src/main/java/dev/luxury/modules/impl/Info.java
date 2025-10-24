package dev.luxury.modules.impl;

import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.joml.Vector4f;

import java.awt.*;

public class Info {
    MinecraftClient mc = MinecraftClient.getInstance();

    public void render(EventRender2D e) {
        float screenWidth = e.getDrawContext().getScaledWindowWidth();
        float screenHeight = e.getDrawContext().getScaledWindowHeight();
        float padding = 4f;

        String[] icons = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        String bpsText = " " + getBPS();
        String coordsText = "X: " + getX() + " Y: " + getY() + " Z: " + getZ();

        float iconWidthBPS = FontDraw.icons.getStringWidth(icons[6]);
        float textWidthBPS = FontDraw.Montserrat_Medium.getStringWidth(bpsText);
        float totalWidthBPS = iconWidthBPS + textWidthBPS;

        float iconWidthCoords = FontDraw.icons.getStringWidth(icons[5]);
        float textWidthCoords = FontDraw.Montserrat_Medium.getStringWidth(coordsText);
        float totalWidthCoords = iconWidthCoords + textWidthCoords;

        float infoHeight = FontDraw.Montserrat_Medium.getFontHeight("A");

        float startX = 2.5f;
        float startY = screenHeight - infoHeight - 33;
        float coordsY = startY + (infoHeight + padding + 10);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), startX, coordsY, totalWidthCoords + padding * 2, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10A0A0A);
        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[5], startX + padding-1, coordsY + padding+2.5f, Color.YELLOW.getRGB());
        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), coordsText, startX + padding + iconWidthCoords, coordsY + padding+1.5f, Color.YELLOW.getRGB());
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), startX, startY+7, totalWidthBPS + padding * 2, infoHeight + padding + 2, new Vector4f(4, 4, 4, 4), 0xA10A0A0A);
        FontDraw.icons.drawString(e.getDrawContext().getMatrices(), icons[6], startX + padding, startY + padding+9.5f, Color.YELLOW.getRGB());
        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(), bpsText, startX + padding + iconWidthBPS, startY + padding+8.5f, Color.YELLOW.getRGB());


    }


    private String getX() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            int x = (int) Math.floor(player.getBlockX());
            return String.valueOf(x);
        }
        return "0";
    }

    private String getY() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            int y = (int) Math.floor(player.getBlockY());
            return String.valueOf(y);
        }
        return "0";
    }

    private String getZ() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            int z = (int) Math.floor(player.getBlockZ());
            return String.valueOf(z);
        }
        return "0";
    }

    private double[] bpsHistory = new double[10];
    private int historyIndex = 0;
    private double smoothedBPS = 0;

    private String getBPS() {
        if (mc.player == null) return "0.0 BPS";

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        double horizontalSpeed = Math.sqrt(velX * velX + velZ * velZ);
        double instantBPS = horizontalSpeed * 36.5;

        bpsHistory[historyIndex] = instantBPS;
        historyIndex = (historyIndex + 1) % bpsHistory.length;

        double sum = 0;
        for (double bps : bpsHistory) {
            sum += bps;
        }
        smoothedBPS = sum / bpsHistory.length;

        return String.format("%.1f BPS", smoothedBPS);
    }
}
