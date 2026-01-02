package dev.luxury.modules.impl.other.hud.impl;

import dev.luxury.modules.impl.misc.DiscordRPC;
import dev.luxury.modules.impl.other.hud.api.DraggableHudElement;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class WaterMark extends DraggableHudElement {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private float animation1 = 0f;
    private static final DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static double currentTPS = 20.0;

    public WaterMark(String name, float x, float y) {
        super(name, x, y);
    }

    @Override
    public void render(DrawContext matrices) {
        FontDraw sfpro1 = FontHelper.sfprobold[17];
        FontDraw sfpro2 = FontHelper.sfprobold[10];
        FontDraw sfpro3 = FontHelper.sfprobold[15];
        FontDraw icons1 = FontHelper.icons[16];
        FontDraw icons2 = FontHelper.icons[18];

        String tpsText = getTPSString() + "tps";
        int colorstandart = new Color(115, 115, 120, 255).getRGB();
        int colorfonts1 = Color.WHITE.getRGB();
        int colorfonts2 = new Color(150, 150, 160).getRGB();
        int iconscolor = new Color(45, 125, 255).getRGB();

        String fps = mc.getCurrentFps() + "fps";
        String user = DiscordRPC.getInstance().getUserRole();
        String ping = getPing();
        String[] icons = {"A", "B", "C", "D", "F", "G", "H", "K", "L", "M", "N"};
        String xCoord = getX2();
        String yCoord = getY2();
        String zCoord = getZ2();
        String bps = getBps(mc.player) + "bps";

        float width = sfpro1.getWidth(user);
        float width2 = sfpro1.getWidth("Luxury");
        float width3 = sfpro1.getWidth(fps);
        float width4 = icons1.getWidth(icons[8]);
        float width5 = icons2.getWidth(icons[3]);
        float width6 = sfpro1.getWidth(getPing());
        float width7 = sfpro1.getWidth(tpsText);
        float totalwidth2 = width + width2 + width3 + width4 + width5 + width6 + width7;
        float totalwidth3 = width + width2 + width3 + width4 + width5;
        float totalwidth4 = width + width2 + width3 + width4 + width5 + width6;

        float width8 = icons1.getWidth(icons[9]);
        float width9 = sfpro1.getWidth(xCoord);
        float width10 = sfpro1.getWidth(yCoord);
        float width11 = sfpro1.getWidth(zCoord);
        float width12 = sfpro1.getWidth(bps);
        float totalwidth5 = width8 + width9 + width10 + width11 + width12;
        float totalwidth6 = width9;
        float totalwidth7 = width9 + width10;
        float totalwidth8 = width9 + width10 + width11;

        float startX = this.x;
        float startY = this.y;

        this.width = totalwidth2 + 82;
        this.height = 41;

        RenderUtil.drawBlur(matrices.getMatrices(), startX, startY, totalwidth2 + 82, 20, new Vector4f(8, 8, 8, 8),18, colorstandart);
        RenderUtil.drawBlur(matrices.getMatrices(), startX, startY + 21, totalwidth5 + 60, 20, new Vector4f(8, 8, 8, 8),18, colorstandart);
        RenderUtil.drawRoundedRectGradient(matrices.getMatrices(), startX + 36, startY + 6, 20.5f, 6.5f, new Vector4f(2.5f, 2.5f, 2.5f, 2.5f), new Color(45, 125, 255).getRGB(), new Color(80, 160, 255).getRGB());

        RenderUtil.drawBorder(matrices.getMatrices(), startX + 35.5f, startY + 5.5f, 21.5f, 7.5f, new Vector4f(2.5f, 2.5f, 2.5f, 2.5f), new Color(45, 125, 255).getRGB(), 0.1f, 1, 1, false);
        RenderUtil.drawBlur(matrices.getMatrices(), startX + 60, startY + 8, 4, 4, new Vector4f(1, 1, 1, 1),18, new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices.getMatrices(), startX + width + 83, startY + 8, 4, 4, new Vector4f(1, 1, 1, 1),18, new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices.getMatrices(), startX + totalwidth3 + 59, startY + 8, 4, 4, new Vector4f(1, 1, 1, 1),18, new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices.getMatrices(), startX + totalwidth4 + 69, startY + 8, 4, 4, new Vector4f(1, 1, 1, 1),18, new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices.getMatrices(), startX + totalwidth8 + 47, startY + 28.5f, 4, 4, new Vector4f(1, 1, 1, 1),18, new Color(45, 125, 255).getRGB());

        sfpro1.drawGradientText(matrices, "Luxury", startX + 5, startY + 4, colorfonts1, colorfonts2);
        sfpro1.drawGradientText(matrices, user, startX + 79, startY + 4, colorfonts1, colorfonts2);
        sfpro1.drawGradientText(matrices, fps, startX + width + 104, startY + 4, colorfonts1, colorfonts2);
        sfpro1.drawGradientText(matrices, ping, startX + totalwidth3 + 67, startY + 4, colorfonts1, colorfonts2);
        sfpro1.drawGradientText(matrices, tpsText, startX + totalwidth4 + 78, startY + 4, colorfonts1, colorfonts2);
        sfpro2.drawFontLeft(matrices, "1.21.4", startX + 38.5f, startY + 6, new Color(255, 255, 255, 255).getRGB());
        sfpro1.drawFontLeft(matrices, "x", startX + 17, startY + 25, new Color(153, 153, 153, 255).getRGB());
        sfpro1.drawGradientText(matrices, xCoord, startX + 22, startY + 25, colorfonts1, colorfonts2);
        sfpro3.drawFontLeft(matrices, "y", startX + totalwidth6 + 27, startY + 25.5f, new Color(153, 153, 153, 255).getRGB());
        sfpro1.drawGradientText(matrices, yCoord, startX + totalwidth6 + 32, startY + 25, colorfonts1, colorfonts2);
        sfpro1.drawFontLeft(matrices, "z", startX + totalwidth7 + 40, startY + 25, new Color(153, 153, 153, 255).getRGB());
        sfpro1.drawGradientText(matrices, zCoord, startX + totalwidth7 + 45, startY + 25, colorfonts1, colorfonts2);
        sfpro1.drawGradientText(matrices, bps, startX + totalwidth8 + 65, startY + 25, colorfonts1, colorfonts2);
        icons1.drawFontLeft(matrices, icons[8], startX + 67, startY + 5.5f, iconscolor);
        icons2.drawFontLeft(matrices, icons[3], startX + width + 91, startY + 5, iconscolor);
        icons2.drawFontLeft(matrices, icons[9], startX + 5, startY + 26.5f, iconscolor);
        icons2.drawFontLeft(matrices, icons[10], startX + totalwidth8 + 55, startY + 26, iconscolor);
    }

    private String getTPSString() {
        updateTPS();
        return String.format(Locale.US, "%.1f", currentTPS);
    }

    private void updateTPS() {
        MinecraftServer server = mc.getServer();
        if (server == null) {
            currentTPS = 20.0;
            return;
        }

        try {
            double mspt = server.getTickManager().getMillisPerTick();
            if (mspt > 0) {
                currentTPS = Math.min(20.0, 1000.0 / mspt);
            } else {
                currentTPS = 20.0;
            }
        } catch (Exception e) {
            currentTPS = 20.0;
        }
    }

    private String getX2() {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            return String.valueOf((int) Math.floor(player.getBlockX()));
        }
        return "0";
    }

    private String getY2() {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            return String.valueOf((int) Math.floor(player.getBlockY()));
        }
        return "0";
    }

    private String getZ2() {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            return String.valueOf((int) Math.floor(player.getBlockZ()));
        }
        return "0";
    }

    private String getBps(Entity entity) {
        if (mc.player == null) return "0.00";
        double dx = entity.getX() - entity.prevX;
        double dz = entity.getZ() - entity.prevZ;
        return String.format(Locale.US, "%.2f", Math.hypot(dx, dz) * 20.0D);
    }

    private String getPing() {
        try {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
                animation1 += (ping - animation1) * 0.2f;
                return df.format(animation1) + "ms ";
            }
        } catch (Exception ignored) {
        }
        return "0ms";
    }
}