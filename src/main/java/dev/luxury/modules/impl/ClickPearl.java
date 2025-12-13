package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.KeySetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickPearl",
        category = Category.Player
)
public class ClickPearl extends Module {
    KeySetting keySetting = new KeySetting("Бинд", GLFW.GLFW_MOUSE_BUTTON_3);
    private boolean shouldThrow = false;

    public ClickPearl() {
        addSettings(keySetting);
        keySetting.setMouse(true);
    }

    @EventTarget
    public void onMouse(EventMouseInput e) {
        if (mc.player == null || mc.world == null) return;

        // Проверка на открытые меню/экраны
        if (mc.currentScreen != null) return;

        if (keySetting.isMouse() && e.getButton() == keySetting.getValue() && e.getAction() == 1) {
            shouldThrow = true;
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (!shouldThrow) return;
        shouldThrow = false;

        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.currentScreen != null) return;

        throwPearl();
    }

    private void throwPearl() {
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return;
        }

        if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return;
        }

        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot != -1) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = pearlSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = currentSlot;
            return;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                int currentSlot = mc.player.getInventory().selectedSlot;

                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        i,
                        currentSlot,
                        SlotActionType.SWAP,
                        mc.player
                );

                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        i,
                        currentSlot,
                        SlotActionType.SWAP,
                        mc.player
                );
                return;
            }
        }
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