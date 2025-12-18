package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.KeySetting;
import dev.luxury.modules.api.settings.ModeSetting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "AutoSwap",
        desc = "Автоматически свапает предметы в оффхенд",
        category = Category.Combat
)
public class AutoSwap extends Module {
    private final KeySetting bind = new KeySetting("Кнопка", GLFW.GLFW_MOUSE_BUTTON_5);
    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Totem of Undying",
            new String[]{"Totem of Undying", "Player Head", "Golden Apple", "Shield", "Crystal"});
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Shield",
            new String[]{"Totem of Undying", "Player Head", "Golden Apple", "Shield", "Crystal"});

    private boolean shouldSwap = false;

    public AutoSwap() {
        addSettings(bind, firstItem, secondItem);
        bind.setMouse(true);
    }

    @EventTarget
    public void onMouse(EventMouseInput e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (bind.isMouse() && e.getButton() == bind.getValue() && e.getAction() == 1) {
            shouldSwap = true;
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (!shouldSwap) return;
        shouldSwap = false;

        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        Item firstType = getItemByType(firstItem.get());
        Item secondType = getItemByType(secondItem.get());

        Item targetItem = null;

        if (firstType != Items.AIR && hasItem(firstType) && mc.player.getOffHandStack().getItem() != firstType) {
            targetItem = firstType;
        } else if (secondType != Items.AIR && hasItem(secondType) && mc.player.getOffHandStack().getItem() != secondType) {
            targetItem = secondType;
        }

        if (targetItem != null) {
            swapToOffhand(targetItem);
        }
    }

    private boolean hasItem(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private void swapToOffhand(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                int guiSlot = (i < 9) ? i + 36 : i;

                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        guiSlot,
                        40,
                        net.minecraft.screen.slot.SlotActionType.SWAP,
                        mc.player
                );
                return;
            }
        }
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Totem of Undying" -> Items.TOTEM_OF_UNDYING;
            case "Player Head" -> Items.PLAYER_HEAD;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            case "Crystal" -> Items.END_CRYSTAL;
            default -> Items.AIR;
        };
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldSwap = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldSwap = false;
    }
}