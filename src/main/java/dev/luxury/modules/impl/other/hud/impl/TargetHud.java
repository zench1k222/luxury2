package dev.luxury.modules.impl.other.hud.impl;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.combat.KillAura;
import dev.luxury.modules.impl.render.NameProtect;
import dev.luxury.modules.impl.other.elytraaura.ElytraAura;
import dev.luxury.modules.impl.other.hud.api.DraggableHudElement;
import dev.luxury.utils.animations.Easing;
import dev.luxury.utils.animations.infinity.InfinityAnimation;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.player.ServerUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.render.ScissorUtil;
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

    public static TargetHud instance;

    private long lastHitTime = 0L;
    private final InfinityAnimation healthAnim = new InfinityAnimation(Easing.LINEAR);
    private final InfinityAnimation absorptionAnim = new InfinityAnimation(Easing.LINEAR);

    private LivingEntity currentTarget;
    private float displayedHealth = 0f;

    private static final long HIT_DURATION = 500L;

    public TargetHud(String name, float x, float y) {
        super(name, x, y);
        instance = this;
        this.width = 117.5f;
        this.height = 41.5f;
    }

    public static TargetHud getInstance() {
        return instance;
    }

    public void onHit(LivingEntity target) {
        if (target != null) {
            lastHitTime = System.currentTimeMillis();
        }
    }

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

    @Override
    public void render(DrawContext context) {
        LivingEntity target = updateTarget();
        if (target == null) return;

        MatrixStack matrices = context.getMatrices();

        FontDraw sfpro1 = FontHelper.sfprobold[18];
        FontDraw sfpro2 = FontHelper.sfprobold[15];
        int colorStandard = new Color(115, 115, 120, 255).getRGB();

        String originalName = target.getName().getString();
        String name = originalName;

        if (NameProtect.instance != null && NameProtect.instance.isEnabled()) {
            String cleanName = originalName.replaceAll("§[0-9a-fk-or]", "");
            String protectedName = NameProtect.instance.getProtectedName(cleanName);

            if (!protectedName.equals(cleanName)) {
                name = originalName.replace(cleanName, protectedName);
            }
        }
        float health = ServerUtil.getHealth(target);
        float maxHealth = target.getMaxHealth();
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        float absorption = target.getAbsorptionAmount();
        float totalHealth = health + absorption;
        String hp = "hp " + df.format(totalHealth);
        float targetHealth = health + target.getAbsorptionAmount();
        displayedHealth += (targetHealth - displayedHealth) * 0.2f;

        float startX = this.x;
        float startY = this.y;

        this.width = 117.5f;
        this.height = 41.5f;

        RenderUtil.drawBlur(matrices, startX, startY, 117.5f, 41.5f, new Vector4f(8, 8, 8, 8),18f, colorStandard);


        float barX = startX + 43.5f;
        float barY = startY + 30f;
        float barWidth = 67.5f;
        float barHeight = 4.5f;
        float healthPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        float animatedHealth = healthAnim.animate(healthPercent, 100);
        float currentBarWidth = barWidth * animatedHealth;
        int colorFonts1 = Color.WHITE.getRGB();
        int colorFonts2 = new Color(150, 150, 160).getRGB();

        if (currentBarWidth > 0) {
            RenderUtil.drawBlur(matrices, barX, barY, barWidth, barHeight, new Vector4f(1f, 1f, 1f, 1f),18f, new Color(45, 45, 50, 120).getRGB());
            RenderUtil.drawBlur(matrices, startX + 40, startY, 0.9f, 42, new Vector4f(0f, 0f, 0f, 0f),18f, new Color(60, 60, 70).getRGB());
            RenderUtil.drawRoundedRectGradient(matrices, barX, barY, currentBarWidth, barHeight, new Vector4f(1f, 1f, 1, 1f), new Color(45, 125, 255).getRGB(), new Color(80, 160, 255).getRGB());

            if (absorption > 0) {
                float absorptionPercent = MathHelper.clamp(absorption / maxHealth, 0.0f, 1.0f);
                float animatedAbsorption = absorptionAnim.animate(absorptionPercent, 100);
                float absorptionBarWidth = barWidth * animatedAbsorption;

                int[] gradient2 = ColorUtil.getAnimatedGradient(0xFF2D7DFF, 0xFF50A0FF, 8, 0);
                RenderUtil.drawRoundedRectGradient(matrices, barX, barY, absorptionBarWidth, barHeight, new Vector4f(1f, 1f, 1f, 1f), gradient2[0], gradient2[1]);
            }
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceHit = currentTime - lastHitTime;
        float hitProgress = 1.0f - MathHelper.clamp(timeSinceHit / (float) HIT_DURATION, 0.0f, 1.0f);

        if (target instanceof PlayerEntity) {
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(matrices, ((AbstractClientPlayerEntity) target).getSkinTextures().texture(), startX + 6f, startY + 6f, 28, 28, 0.125f, 0.125f, 0.25f, 0.25f, new Vector4f(4.5f, 4.5f, 4.5f, 4.5f), tintColor);
        } else {
            Identifier image = Identifier.of("luxury:images/target.png");
            int tintColor = applyDamageTint(0xFFFFFFFF, hitProgress);
            RenderUtil.drawRoundedImage(matrices, image, startX + 6f, startY + 5.5f, 25, 25, 0f, 0f, 1f, 1f, new Vector4f(2f, 2f, 2f, 2f), tintColor);
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
        sfpro1.drawGradientText(matrices, name, barX, nameY, colorFonts1, colorFonts2);
        if (needsClipping) {
            ScissorUtil.pop();
        }

        sfpro2.drawFontLeft(matrices, hp, hpX, startY + 6f, getHealthColor(animatedHealth));

        if (target instanceof PlayerEntity) {
            renderEquipmentSlots(context, barX, startY + 20f, (PlayerEntity) target);
        }
    }

    private void renderEquipmentSlots(DrawContext context, float startX, float startY, PlayerEntity player) {
        MatrixStack matrices = context.getMatrices();

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

        for (int i = 0; i < 6; i++) {
            RenderUtil.drawRoundedRect(matrices, currentX, startY - 3.5f, slotSize, slotSize, new Vector4f(2, 2, 2, 2), new Color(35, 35, 40, 255).getRGB());

            RenderUtil.drawBorder(matrices, currentX, startY - 3.5f, slotSize, slotSize, new Vector4f(2, 2, 2, 2), new Color(60, 60, 70, 255).getRGB(), -0.8f, 1, 1, false);

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

                        RenderUtil.drawBlur(matrices, durabilityX, durabilityY, slotSize - 2f, durabilityBarHeight, new Vector4f(0.1f, 0.1f, 0.1f, 0.1f),18f, new Color(40, 40, 40, 200).getRGB());

                        int durabilityColor = getDurabilityColor(durabilityPercent);
                        RenderUtil.drawBlur(matrices, durabilityX, durabilityY, durabilityBarWidth, durabilityBarHeight, new Vector4f(0.1f, 0.1f, 0.1f, 0.1f),18f, durabilityColor);
                    }
                }

                int count = items[i].getCount();
                if (count > 1) {
                    String countText = String.valueOf(count);
                    FontDraw countFont = FontHelper.sfprobold[10];
                    float countX = currentX + slotSize - countFont.getWidth(countText) - 1f;
                    float countY = startY - 3.5f + slotSize - countFont.getHeight() - 0.5f;
                    countFont.drawFontLeft(matrices, countText, countX, countY, Color.WHITE.getRGB());
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

    private int applyDamageTint(int baseColor, float strength) {
        int r = 255;
        int g = (int) (255 * (1.0f - strength * 0.7f));
        int b = (int) (255 * (1.0f - strength * 0.7f));
        return (baseColor & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public boolean hasTarget() {
        return updateTarget() != null;
    }
}