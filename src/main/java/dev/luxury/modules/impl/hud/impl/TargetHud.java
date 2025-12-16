package dev.luxury.modules.impl.hud.impl;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.modules.impl.hud.api.DraggableHudElement;
import dev.luxury.utils.animations.Easing;
import dev.luxury.utils.animations.infinity.InfinityAnimation;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.player.ServerUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHud extends DraggableHudElement {

    private final InfinityAnimation healthAnim = new InfinityAnimation(Easing.LINEAR);
    private final InfinityAnimation absortitionAnim = new InfinityAnimation(Easing.LINEAR);
    private LivingEntity currentTarget;
    private long lasthittime = 0L;
    private static final long hitduration = 500L;

    public TargetHud(String name, float x, float y) {
        super(name, x, y);
        this.width = 117.5f;
        this.height = 41.5f;
    }

    @Override
    public void render(MatrixStack matrices) {
        LivingEntity target = updateTarget();

        if (target == null) return;

        renderTargetHud(matrices, target);
    }

    private LivingEntity updateTarget() {
        KillAura aura = ModuleManager.getModule(KillAura.class);

        LivingEntity newTarget = null;

        if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
            newTarget = aura.getTarget();
        }

        if (mc.currentScreen instanceof ChatScreen) {
            newTarget = mc.player;
        }

        if (newTarget != currentTarget) {
            currentTarget = newTarget;
        }

        return currentTarget;
    }

    private void renderTargetHud(MatrixStack matrices, LivingEntity target) {
        FontDraw sfpro1 = FontHelper.sfprobold[18];
        FontDraw sfpro2 = FontHelper.sfprobold[15];

        String name = target.getName().getString();
        float health = ServerUtil.getHealth(target);
        float maxHealth = target.getMaxHealth();
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        float absorption = target.getAbsorptionAmount();
        float totalHealth = health + absorption;
        String hp = "hp " + df.format(totalHealth);

        float startX = this.x;
        float startY = this.y;

        int colorstandart = new Color(25, 25, 30, 255).getRGB();

        RenderUtil.drawRoundedRect(matrices, startX, startY, 117.5f, 41.5f,
                new Vector4f(8, 8, 8, 8), colorstandart);

        float barX = startX + 43.5f;
        float barY = startY + 30f;
        float barWidth = 67.5f;
        float barHeight = 4.5f;
        float healthPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        float animatedHealth = healthAnim.animate(healthPercent, 100);
        float currentBarWidth = barWidth * animatedHealth;

        if (currentBarWidth > 0) {
            RenderUtil.drawRoundedRect(matrices, barX, barY, barWidth, barHeight,
                    new Vector4f(1f, 1f, 1f, 1f), new Color(45, 45, 50, 120).getRGB());
            RenderUtil.drawRoundedRect(matrices, startX + 40, startY, 0.9f, 42,
                    new Vector4f(0f, 0f, 0f, 0f), new Color(60, 60, 70).getRGB());
            RenderUtil.drawRoundedRectGradient(matrices, barX, barY, currentBarWidth, barHeight,
                    new Vector4f(1f, 1f, 1, 1f), new Color(45, 125, 255).getRGB(), new Color(80, 160, 255).getRGB());

            if (absorption > 0) {
                float absorptionPercent = MathHelper.clamp(absorption / maxHealth, 0.0f, 1.0f);
                float animatedAbsortition = absortitionAnim.animate(absorptionPercent, 100);
                float absorptionBarWidth = barWidth * animatedAbsortition;

                int[] gradient2 = ColorUtil.getAnimatedGradient(0xFF2D7DFF, 0xFF50A0FF, 8, 0);
                RenderUtil.drawRoundedRectGradient(matrices, barX, barY, absorptionBarWidth, barHeight,
                        new Vector4f(1f, 1f, 1f, 1f), gradient2[0], gradient2[1]);
            }
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - lasthittime;
        float hitProgress = 1.0f - MathHelper.clamp(timeSinceHit / (float) hitduration, 0.0f, 1.0f);

        if (target instanceof PlayerEntity) {
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(matrices,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    startX + 6f, startY + 6f, 28, 28, 0.125f, 0.125f, 0.25f, 0.25f,
                    new Vector4f(4.5f, 4.5f, 4.5f, 4.5f), tintColor);
        } else {
            Identifier image = Identifier.of("luxury:images/target.png");
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(matrices, image, startX + 6f, startY + 5.5f, 25, 25,
                    0f, 0f, 1f, 1f, new Vector4f(2f, 2f, 2f, 2f), tintColor);
        }

        float nameY = startY + 5f;
        sfpro1.drawGradientText(matrices, name, barX, nameY, Color.WHITE.getRGB(), new Color(150, 150, 160).getRGB());
        sfpro2.drawFontLeft(matrices, hp, barX + sfpro1.getWidth(name) + 2f, startY + 6f, getHealthColor(animatedHealth));

        if (target instanceof PlayerEntity) {
            renderEquipmentSlots(matrices, barX, startY + 20f, (PlayerEntity) target);
        }
    }

    private void renderEquipmentSlots(MatrixStack matrices, float startX, float startY, PlayerEntity player) {
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

        DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());

        for (int i = 0; i < 6; i++) {
            RenderUtil.drawRoundedRect(matrices, currentX, startY - 3.5f, slotSize, slotSize,
                    new Vector4f(2, 2, 2, 2), new Color(35, 35, 40, 255).getRGB());

            RenderUtil.drawBorder(matrices, currentX, startY - 3.5f, slotSize, slotSize,
                    new Vector4f(2, 2, 2, 2), new Color(60, 60, 70, 255).getRGB(), -0.8f, 1, 1, false);

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

                        RenderUtil.drawRoundedRect(matrices, durabilityX, durabilityY, slotSize - 2f, durabilityBarHeight,
                                new Vector4f(0.1f, 0.1f, 0.1f, 0.1f), new Color(40, 40, 40, 200).getRGB());

                        int durabilityColor = getDurabilityColor(durabilityPercent);
                        RenderUtil.drawRoundedRect(matrices, durabilityX, durabilityY, durabilityBarWidth, durabilityBarHeight,
                                new Vector4f(0.1f, 0.1f, 0.1f, 0.1f), durabilityColor);
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
            return new Color((int)((1.0f - percent) * 2 * 255), 255, 0).getRGB();
        } else {
            return new Color(255, (int)(percent * 2 * 255), 0).getRGB();
        }
    }

    private int getHealthColor(float percent) {
        if (percent > 0.5f) {
            return new Color((int)((1.0f - percent) * 2 * 255), 255, 0).getRGB();
        } else {
            return new Color(255, (int)(percent * 2 * 255), 0).getRGB();
        }
    }

    private int applyDamageTint(int baseColor, float strength) {
        int r = 255;
        int g = (int) (255 * (1.0f - strength * 0.7f));
        int b = (int) (255 * (1.0f - strength * 0.7f));
        return (baseColor & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public void onHit(LivingEntity target) {
        if (target != null) {
            lasthittime = System.currentTimeMillis();
        }
    }
}