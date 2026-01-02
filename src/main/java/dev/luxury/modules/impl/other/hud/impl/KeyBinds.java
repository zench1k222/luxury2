package dev.luxury.modules.impl.other.hud.impl;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.other.hud.api.DraggableHudElement;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeyBinds extends DraggableHudElement {
    private static final int PADDING = 5;
    private static final int ITEM_HEIGHT = 12;
    private final String[] icons = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R"};

    public KeyBinds(String name, float x, float y) {
        super(name, x, y);
    }

    @Override
    public void render(DrawContext matrices) {
        FontDraw sfpro = FontHelper.sfprobold[18];
        FontDraw sfpro1 = FontHelper.sfprobold[16];
        FontDraw sfpro2 = FontHelper.sfprobold[13];
        FontDraw iconsFont = FontHelper.icons[20];

        FontDraw iconsFontMovement = FontHelper.icons[16];
        FontDraw iconsFontCombat = FontHelper.icons[14];
        FontDraw iconsFontMisc = FontHelper.icons[14];
        FontDraw iconsFontRender = FontHelper.icons[14];
        FontDraw iconsFontPlayer = FontHelper.icons[14];

        int colorfonts1 = Color.WHITE.getRGB();
        int colorfonts2 = new Color(150, 150, 160).getRGB();
        int colorstandart = new Color(115, 115, 120, 255).getRGB();


        List<Module> boundModules = ModuleManager.getModules().stream().filter(Module::isEnabled).filter(m -> m.getKey() > 0).collect(Collectors.toList());
        float width = 97.5f;
        float startX = this.x;
        float startY = this.y;
        int titleHeight = 14;

        int totalHeight = (PADDING * 2 + titleHeight + boundModules.size() * ITEM_HEIGHT - 3);

        this.width = width;
        this.height = totalHeight;

        RenderUtil.drawBlur(matrices.getMatrices(), startX, startY, width, totalHeight, new Vector4f(8, 8, 8, 8),18f, colorstandart);
        sfpro.drawGradientText(matrices, "KeyBinds", startX + PADDING + 13, startY + PADDING - 3, colorfonts1, colorfonts2);
        iconsFont.drawFontLeft(matrices, icons[7], startX + 5, startY + 3, new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices.getMatrices(), startX, startY + 14, 97.5f, 0.9f, new Vector4f(0f, 0f, 0f, 0f),18f ,new Color(60, 60, 70).getRGB());

        int currentY = (int) (startY + PADDING + titleHeight);
        for (Module module : boundModules) {
            String keyName = getKeyName(module.getKey());
            String moduleName = module.getName();

            String categoryIcon = getCategoryIcon(module.getCategory());
            FontDraw categoryIconFont = getCategoryIconFont(module.getCategory(), iconsFontMovement, iconsFontCombat, iconsFontMisc, iconsFontRender, iconsFontPlayer);

            float iconX = startX + PADDING + 1;

            categoryIconFont.drawFontLeft(matrices, categoryIcon, iconX, currentY, new Color(45, 125, 255).getRGB());
            RenderUtil.drawBlur(matrices.getMatrices(), iconX + 10, currentY + 1.5f, 4, 4, new Vector4f(1, 1, 1, 1),18f, new Color(45, 125, 255).getRGB());

            sfpro1.drawGradientText(matrices, moduleName, startX + PADDING + 18, currentY - 1.5f, colorfonts1, colorfonts2);

            float keyWidth = sfpro.getWidth(keyName);
            sfpro2.drawFontLeft(matrices, keyName, startX + width - keyWidth - PADDING, currentY - 1, colorfonts2);

            currentY += ITEM_HEIGHT;
        }
    }

    private String getCategoryIcon(Category category) {
        if (category == Category.Movement) return icons[14];
        else if (category == Category.Combat) return icons[6];
        else if (category == Category.Misc) return icons[15];
        else if (category == Category.Render) return icons[16];
        else if (category == Category.Player) return icons[11];
        else return icons[0];
    }

    private FontDraw getCategoryIconFont(Category category, FontDraw movement, FontDraw combat, FontDraw misc, FontDraw render, FontDraw player) {
        if (category == Category.Movement) return movement;
        else if (category == Category.Combat) return combat;
        else if (category == Category.Misc) return misc;
        else if (category == Category.Render) return render;
        else if (category == Category.Player) return player;
        else return movement;
    }

    private String getKeyName(int keyCode) {
        try {
            String keyName = InputUtil.fromKeyCode(keyCode, 0).getTranslationKey();
            if (keyName.startsWith("key.keyboard.")) {
                keyName = keyName.substring(13);
            }
            return keyName.toUpperCase();
        } catch (Exception e) {
            return "?";
        }
    }
}