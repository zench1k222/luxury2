package dev.luxury.modules.impl.elytraaura;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static dev.luxury.modules.api.Module.mc;

public class ElytraAuraUtility {

    public static int findFireworkOnHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                return i;
            }
        }
        return -1;
    }

    public static void useItem(int slot) {
        int prev = mc.player.getInventory().selectedSlot;
        if (prev != slot) {
            mc.player.getInventory().selectedSlot = slot;
        }
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (prev != slot) {
            mc.player.getInventory().selectedSlot = prev;
        }
    }
}

