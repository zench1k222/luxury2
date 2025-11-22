package dev.luxury.modules.impl;

import dev.luxury.events.impl.render.EventRender2D;

import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class WaterMark {
    MinecraftClient mc = MinecraftClient.getInstance();
    ClientPlayerEntity player = mc.player;
    private float animation1 = 0f;
    DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public void render(EventRender2D e) {

    FontDraw sfpro1 = FontHelper.sfprobold[17];
    FontDraw sfpro2  = FontHelper.sfprobold[10];
    FontDraw sfpro3 = FontHelper.sfprobold[15];
    FontDraw icons1 = FontHelper.icons[16];
    FontDraw icons2 = FontHelper.icons[18];
        String tpsText = getTPSString() + "tps";
    int colorstandart = new Color(29,29,29,242).getRGB();
    int colorfonts1= new Color(255,255,255,255).getRGB();
    int colorfonts2= new Color(153,153,153,255).getRGB();
    int iconscolor = new Color(251,225,0,255).getRGB();
        String fps = mc.getCurrentFps() +"fps";
        String user = "Developer";
        String ping = getPing();
        String[] icons = {"A", "B","C","D","F","G","H","K","L","M","N"};
        String x = getX();
        String y = getY();
        String z = getZ();
        String bps = getBps(mc.player) + "bps";
        float width = sfpro1.getWidth(user);
        float width2 = sfpro1.getWidth("Luxury");
        float width3 = sfpro1.getWidth(fps);
        float width4 = icons1.getWidth(icons[8]);
        float width5 = icons2.getWidth(icons[3]);
        float width6 = sfpro1.getWidth(getPing() );
        float width7 = sfpro1.getWidth(tpsText);
        float totalwidth2 = width + width2 + width3 + width4 + width5 + width6 + width7;
        float totalwidth3 = width + width2 + width3 + width4 + width5;
        float totalwidth4 = width + width2 + width3 + width4 + width5 + width6 ;
        float width8 = icons1.getWidth(icons[9]);
        float width9 = sfpro1.getWidth(x);
        float width10 = sfpro1.getWidth(y);
        float width11 = sfpro1.getWidth(z);
        float width12 = sfpro1.getWidth(bps);
        float totalwidth5 = width8 + width9 + width10 + width11 + width12;
        float totalwidth6 = width9;
        float totalwidth7 = width9 + width10;
        float totalwidth8 = width9 + width10 + width11;
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),5,4.5f,totalwidth2 + 82,20,new Vector4f(8,8,8,8),colorstandart);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),5,25.5f,totalwidth5 + 60,20,new Vector4f(8,8,8,8),colorstandart);
        RenderUtil.drawRoundedRectGradient(e.getDrawContext().getMatrices(),41,10.5f,20.5f,6.5f,new Vector4f(2.5f,2.5f,2.5f,2.5f),new Color(195,189,141,255).getRGB(),new Color(221,195,0,255).getRGB());

       RenderUtil.drawBorder(e.getDrawContext().getMatrices(),40.5f,10f,21.5f,7.5f,new Vector4f(2.5f,2.5f,2.5f,2.5f),new Color(221,195,0,255).getRGB(),0.1f,1,1,false);
       RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),65,12.5f,4,4,new Vector4f(1,1,1,1),new Color(217,217,217,255).getRGB());
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),width + 88 ,12.5f,4,4,new Vector4f(1,1,1,1),new Color(217,217,217,255).getRGB());
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),totalwidth3 + 64   ,12.5f,4,4,new Vector4f(1,1,1,1),new Color(217,217,217,255).getRGB());
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),totalwidth4 + 74  ,12.5f,4,4,new Vector4f(1,1,1,1),new Color(217,217,217,255).getRGB());
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),totalwidth8+ 52  ,33f,4,4,new Vector4f(1,1,1,1),new Color(217,217,217,255).getRGB());


        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),"Luxury",10f,8.5f,colorfonts1,colorfonts2);
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),user,84f,8.5f,colorfonts1,colorfonts2);
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),fps,width + 109,8.5f,colorfonts1,colorfonts2);
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),ping,totalwidth3 +72 ,8.5f,colorfonts1,colorfonts2);
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),tpsText,totalwidth4 +83 ,8.5f,colorfonts1,colorfonts2);
        sfpro2.drawFontLeft(e.getDrawContext().getMatrices(),"1.21.4",43.5f,10.5f,new Color(255,255,255,255).getRGB());
        sfpro1.drawFontLeft(e.getDrawContext().getMatrices(),"x",22f,29.5f,new Color(153,153,153,255).getRGB());
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),x,27 ,29.5f,colorfonts1,colorfonts2);
        sfpro3.drawFontLeft(e.getDrawContext().getMatrices(),"y",totalwidth6+ 32,30f,new Color(153,153,153,255).getRGB());
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),y,totalwidth6 +37 ,29.5f,colorfonts1,colorfonts2);
        sfpro1.drawFontLeft(e.getDrawContext().getMatrices(),"z",totalwidth7 + 45,29.5f,new Color(153,153,153,255).getRGB());
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),z,totalwidth7 +50 ,29.5f,colorfonts1,colorfonts2);
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(),bps,totalwidth8 +70 ,29.5f,colorfonts1,colorfonts2);
        icons1.drawFontLeft(e.getDrawContext().getMatrices(),icons[8],72,10, iconscolor);
        icons2.drawFontLeft(e.getDrawContext().getMatrices(),icons[3],width + 96,9.5f, iconscolor);
        icons2.drawFontLeft(e.getDrawContext().getMatrices(),icons[9],10,31f, iconscolor);
        icons2.drawFontLeft(e.getDrawContext().getMatrices(),icons[10],totalwidth8 + 60,30.5f, iconscolor);

    }
    private static double currentTPS = 20.0;

    public static double getTPS() {
        updateTPS();
        return currentTPS;
    }

    public static String getTPSString() {
        return String.format(java.util.Locale.US, "%.1f", getTPS());
    }

    private static void updateTPS() {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();

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

    public static String getBps(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return "0.00";
        double dx = entity.getX() - entity.prevX;
        double dz = entity.getZ() - entity.prevZ;
        return String.format(Locale.US, "%.2f", Math.hypot(dx, dz) * 20.0D);
    }



    private String getPing() {
        MinecraftClient mc = MinecraftClient.getInstance();
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


