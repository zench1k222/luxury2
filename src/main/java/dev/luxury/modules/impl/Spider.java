package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "Spider",
        desc = "",
        category = Category.Movement,
        key = GLFW.GLFW_KEY_Z
)
public class Spider extends Module {
    MinecraftClient mc = MinecraftClient.getInstance();
    @EventTarget
    public void onTick(EventTick e) {
        int bucketSlot = findBucketInInventory();
        if (bucketSlot != -1) {
            if (!mc.player.getMainHandStack().isOf(Items.WATER_BUCKET)) {
                mc.player.getInventory().selectedSlot = bucketSlot;
            }

            if (mc.player.horizontalCollision) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
            }
        }
    }

        private int findBucketInInventory() {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.isOf(Items.WATER_BUCKET)) {
                    return i;
                }
            }
            return -1;
        }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}

