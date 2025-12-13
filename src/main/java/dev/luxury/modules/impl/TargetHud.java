package dev.luxury.modules.impl;


import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.elytraaura.ElytraAura;
import dev.luxury.utils.animations.Easing;
import dev.luxury.utils.animations.infinity.InfinityAnimation;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.render.ScissorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHud extends Module {
    MinecraftClient mc = MinecraftClient.getInstance();
    public static TargetHud instance;
    private long lasthittime = 0L;
    private final InfinityAnimation healthAnim = new InfinityAnimation(Easing.LINEAR);
    private final InfinityAnimation absortitionAnim = new InfinityAnimation(Easing.LINEAR);
    private final java.util.Map<Item, InfinityAnimation> cooldownAnimations = new java.util.HashMap<>();
    public TargetHud() {
        instance = this;
    }
    private LivingEntity currentTarget;
    public static TargetHud getInstance() {
        return instance;
    }
    private float displayedHealth = 0f;
    private LivingEntity updateTarget() {
        KillAura aura = ModuleManager.getModule(KillAura.class);
        ElytraAura elytraAura = ModuleManager.getModule(ElytraAura.class);

        LivingEntity newTarget = null;

        if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
            newTarget = aura.getTarget();
        } else if (elytraAura != null && elytraAura.isEnabled() && elytraAura.getTarget() != null) {
            newTarget = elytraAura.getTarget();
        }

        if (mc.currentScreen instanceof ChatScreen) {
            newTarget = mc.player;
        }

        if (newTarget != currentTarget) {
            currentTarget = newTarget;
        }

        return currentTarget;
    }


    private static final long hitduration = 500L;

    public void onHit(LivingEntity target) {
        if (target != null) {
            lasthittime = System.currentTimeMillis();
        }
    }


    public void render(EventRender2D e) {
        LivingEntity target = updateTarget();
        if (target == null) return;
        FontDraw sfpro1 = FontHelper.sfprobold[18];
        FontDraw sfpro2  = FontHelper.sfprobold[15];
        int colorstandart = new Color(29,29,29,242).getRGB();

        String name = target.getName().getString();
        float health = target.getHealth() + target.getAbsorptionAmount();
        float maxHealth = target.getMaxHealth();
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String hp = "hp " + df.format(health);
        float targetHealth = target.getHealth() + target.getAbsorptionAmount();
        displayedHealth += (targetHealth - displayedHealth) * 0.2f;
        float screenWidth = e.getDrawContext().getScaledWindowWidth();
        float screenHeight = e.getDrawContext().getScaledWindowHeight();
        float startX = screenWidth - 400;
        float startY = screenHeight - 150;
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), startX, startY,117.5f, 41.5f, new Vector4f(8, 8, 8, 8), colorstandart);
        float barX = startX + 43.5f;
        float barY = startY + 30f;
        float barWidth = 67.5f;
        float barHeight = 4.5f;
        float healthPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        float animatedHealth = healthAnim.animate(healthPercent,100);
        float currentBarWidth = barWidth * animatedHealth;
        int colorfonts1= new Color(255,255,255,255).getRGB();
        int colorfonts2= new Color(153,153,153,255).getRGB();
        if (currentBarWidth > 0) {

            RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), barX, barY, barWidth, barHeight, new Vector4f(1f, 1f, 1f, 1f), new Color(77,77,77,48).getRGB());
            RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(), startX + 40, startY, 0.9f, 42, new Vector4f(0f, 0f, 0f, 0f), new Color(254, 254, 254, 150).getRGB());
            RenderUtil.drawRoundedRectGradient(e.getDrawContext().getMatrices(), barX, barY, currentBarWidth, barHeight, new Vector4f(1f, 1f, 1, 1f),new Color(255,195,0,255).getRGB(), new Color(153,117,0,255).getRGB());
            float absorption = target.getAbsorptionAmount();
            if (absorption > 0) {

                float absorptionPercent = MathHelper.clamp(absorption / maxHealth, 0.0f, 1.0f);
                float animatedAbsortition = absortitionAnim.animate(absorptionPercent,100);
                float absorptionBarWidth = barWidth  * animatedAbsortition;

                float absorptionY = barY ;

                int[] gradient2 = ColorUtil.getAnimatedGradient(0xFFE3D381, 0xFFffd700, 8, 0);

                RenderUtil.drawRoundedRectGradient(e.getDrawContext().getMatrices(), barX, absorptionY, absorptionBarWidth, barHeight, new Vector4f(1f, 1f, 1f, 1f), gradient2[0], gradient2[1]);
            }
        }
        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - lasthittime;
        float hitProgress = 1.0f - MathHelper.clamp(timeSinceHit / (float) hitduration, 0.0f, 1.0f);

        if (target instanceof PlayerEntity) {
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(e.getDrawContext().getMatrices(), ((AbstractClientPlayerEntity) target).getSkinTextures().texture(), startX + 6f, startY + 6f, 28, 28, 0.125f, 0.125f, 0.25f, 0.25f, new Vector4f(4.5f, 4.5f, 4.5f, 4.5f), tintColor
            );
        } else {
            Identifier image = Identifier.of("luxury:images/target.png");
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(e.getDrawContext().getMatrices(), image, startX +6f, startY + 5.5f, 25, 25, 0f, 0f, 1f, 1f, new Vector4f(2f, 2f, 2f, 2f), tintColor);
        }

        float nameY = startY + 5f;
        float nameHeight = sfpro1.getHeight();
        float nameWidth = sfpro1.getWidth(name);
        float hpWidth = sfpro2.getWidth(hp);
        float maxNameWidth = 39.5f;
        float spacing = 1f;

        float hpX;
        boolean needsClipping = false;
        float actualNameWidth = nameWidth;

        if (nameWidth <= maxNameWidth) {
            hpX = barX + nameWidth + spacing;
        } else {
            hpX = barX + 28.5f + maxNameWidth - hpWidth;
            actualNameWidth = Math.max(0, hpX - barX - spacing);
            needsClipping = true;
        }
        if (needsClipping) {
            ScissorUtil.push();
            ScissorUtil.setFromComponentCoordinates(barX, nameY, actualNameWidth, nameHeight);
        }
        sfpro1.drawGradientText(e.getDrawContext().getMatrices(), name, barX, nameY, colorfonts1, colorfonts2);
        if (needsClipping) {
            ScissorUtil.pop();
        }
        sfpro2.drawFontLeft(e.getDrawContext().getMatrices(), hp, hpX, startY + 6f, getHealthColor(animatedHealth));

        if (target instanceof PlayerEntity) {
            renderEquipmentSlots(e, barX, startY + 20f, (PlayerEntity) target);
        }
    }

    private void renderEquipmentSlots(EventRender2D e, float startX, float startY, PlayerEntity player) {
        float slotSize = 10.5f;
        float spacing = 1f;
        float currentX = startX;

        ItemStack[] items = new ItemStack[6];
        items[0] = player.getInventory().getArmorStack(3);
        items[1] = player.getInventory().getArmorStack(2);
        items[2] = player.getInventory().getArmorStack(1);
        items[3] = player.getInventory().getArmorStack(0);
        items[4] = player.getMainHandStack();
        items[5] = player.getOffHandStack();

        DrawContext context = e.getDrawContext();
        MatrixStack matrices = e.getDrawContext().getMatrices();

        for (int i = 0; i < 6; i++) {
            RenderUtil.drawRoundedRect(matrices, currentX, startY - 3.5f, slotSize, slotSize, new Vector4f(2, 2, 2, 2), new Color(53, 53, 53, 255).getRGB());

            RenderUtil.drawBorder(matrices, currentX, startY - 3.5f, slotSize, slotSize, new Vector4f(2, 2, 2, 2), new Color(78, 78, 78, 255).getRGB(), -0.8f, 1, 1, false);

            if (items[i] != null && !items[i].isEmpty()) {
                matrices.push();
                matrices.translate(currentX + 1f, startY - 2.5f, 0);
                matrices.scale(0.5f, 0.5f, 1.0f);
                context.drawItem(items[i], 0, 0);
                matrices.pop();

                if (items[i].isDamageable()) {
                    int maxDamage = items[i].getMaxDamage();
                    int damage = items[i].getDamage();

                    if (damage > 0) {
                        float durabilityPercent = 1.0f - ((float) damage / (float) maxDamage);

                        float durabilityBarWidth = (slotSize - 2f) * durabilityPercent;
                        float durabilityBarHeight = 1.5f;
                        float durabilityX = currentX + 1f;
                        float durabilityY = startY + slotSize - durabilityBarHeight - 4.5f;

                        RenderUtil.drawRoundedRect(matrices, durabilityX, durabilityY, slotSize - 2f, durabilityBarHeight, new Vector4f(0.1f, 0.1f, 0.1f, 0.1f), new Color(40, 40, 40, 200).getRGB());

                        int durabilityColor = getDurabilityColor(durabilityPercent);
                        RenderUtil.drawRoundedRect(matrices, durabilityX, durabilityY, durabilityBarWidth, durabilityBarHeight, new Vector4f(0.1f, 0.1f, 0.1f, 0.1f), durabilityColor);
                    }
                }

                int count = items[i].getCount();
                if (count > 1) {
                    String countText = String.valueOf(count);
                    FontDraw countFont = FontHelper.sfprobold[10];
                    float countX = currentX + slotSize - countFont.getWidth(countText) - 1f;
                    float countY = startY - 3.5f + slotSize - countFont.getHeight() - 0.5f;
                    countFont.drawFontLeft(matrices, countText, countX, countY, Color.white.getRGB());
                }
            }

            currentX += slotSize + spacing;
        }
    }

    private int getDurabilityColor(float percent) {
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