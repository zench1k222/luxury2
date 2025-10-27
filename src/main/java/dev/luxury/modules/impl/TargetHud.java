package dev.luxury.modules.impl;

import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHud extends Module {
    MinecraftClient mc = MinecraftClient.getInstance();
    public static TargetHud instance;
    private long lasthittime = 0L;

    public TargetHud() {
        instance = this;
    }

    public static TargetHud getInstance() {
        return instance;
    }

    private static final long hitduration = 500L;

    public void onHit(LivingEntity target) {
        if (target != null) {
            lasthittime = System.currentTimeMillis();
        }
    }

    public void render(EventRender2D e) {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        String name = target.getName().getString();
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String hp = "HP: " + df.format(health);

        float screenWidth = e.getDrawContext().getScaledWindowWidth();
        float screenHeight = e.getDrawContext().getScaledWindowHeight();
        float startX = screenWidth - 400;
        float startY = screenHeight - 150;

        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), startX, startY, 100, 37.5f, new Vector4f(4, 4, 4, 4), 0xA10a0a0a);

        float barX = startX + 35.5f;
        float barY = startY + 23f;
        float barWidth = 61.5f;
        float barHeight = 5.5f;
        float healthPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        float currentBarWidth = barWidth * healthPercent;
        int barColor = getHealthColor(healthPercent);

        if (currentBarWidth > 0) {
            int[] gradient = ColorUtil.getAnimatedGradient(0xFF5555FF, 0xFFFF5555, 8, 0);
            RenderUtil.drawRoundedRectGradient(e.getDrawContext().getMatrices(), barX, barY, currentBarWidth, barHeight, new Vector4f(1.5f, 1.5f, 1.5f, 1.5f), gradient[0], gradient[1]);
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - lasthittime;
        float hitProgress = 1.0f - MathHelper.clamp(timeSinceHit / (float) hitduration, 0.0f, 1.0f);

        if (target instanceof PlayerEntity) {
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(e.getDrawContext().getMatrices(), ((AbstractClientPlayerEntity) target).getSkinTextures().texture(), startX + 3.5f, startY + 3.5f, 30, 30, 0.125f, 0.125f, 0.25f, 0.25f, new Vector4f(2f, 2f, 2f, 2f), tintColor
            );
        }

        float maxTextWidth = 100 - 37;

        String displayName = name;
        while (FontDraw.Montserrat_Medium.getStringWidth(displayName) > maxTextWidth && displayName.length() > 0) {
            displayName = displayName.substring(0, displayName.length() - 1);
        }
        FontDraw.Montserrat_Medium.drawGradientString(e.getDrawContext().getMatrices(), displayName, startX + 37, startY + 5, 0xFFFFFF00, 0xFF808080, true, 3000, 0
        );

        String displayHp = hp;
        while (FontDraw.Montserrat_Medium.getStringWidth(displayHp) > maxTextWidth && displayHp.length() > 0) {
            displayHp = displayHp.substring(0, displayHp.length() - 1);
        }
        FontDraw.Montserrat_Medium.drawGradientString(e.getDrawContext().getMatrices(), displayHp, startX + 37, startY + 15, 0xFFFFFF00, 0xFF808080, true, 3000, 0
        );
}
    private LivingEntity getTarget() {
        LivingEntity target =ModuleManager.getModule(KillAura.class).getTarget();

        if (target == null
                && mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }

        return target;
    }

    private int getHealthColor(float percent) {
        if (percent > 0.5f) {
            int green = 255;
            int red = (int) ((1.0f - percent) * 2 * 255);
            return 0xFF000000 | (red << 16) | (green << 8);
        } else {
            int red = 255;
            int green = (int) (percent * 2 * 255);
            return 0xFF000000 | (red << 16) | (green << 8);
        }
    }
    private int applyDamageTint(int baseColor,float strenght){
        int r = 255;
        int g =(int)(255*(1.0f - strenght*0.7f));
        int b = (int)(255*(1.0f - strenght *0.7f));
        return (baseColor & 0xFF000000) | (r << 16) | (g<<8) | b;
    }
}