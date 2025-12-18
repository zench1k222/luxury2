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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
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

        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        Item firstType = getItemByType(firstItem.get());
        Item secondType = getItemByType(secondItem.get());

        ItemStack offhandItem = mc.player.getOffHandStack();

        Item itemToPut = null;

        if (firstType != Items.AIR && hasItem(firstType) && offhandItem.getItem() != firstType) {
            itemToPut = firstType;
        } else if (secondType != Items.AIR && hasItem(secondType) && offhandItem.getItem() != secondType) {
            itemToPut = secondType;
        }

        if (itemToPut != null) {
            swapToOffhand(itemToPut);
        } else {
            if (offhandItem.getItem() != Items.AIR) {
                removeFromOffhand();
            }
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
        int itemSlot = -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                itemSlot = i;
                break;
            }
        }

        if (itemSlot == -1) return;

        int guiSlot = itemSlot;
        if (itemSlot < 9) {
            guiSlot = itemSlot + 36;
        }

        mc.interactionManager.clickSlot(0, guiSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, guiSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void removeFromOffhand() {
        int emptySlot = -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) return;

        int guiSlot = emptySlot;
        if (emptySlot < 9) {
            guiSlot = emptySlot + 36;
        }

        mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, guiSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void swapToOffhandSimple(Item item) {
        int itemSlot = findItemSlot(item);
        if (itemSlot == -1) return;

        swapSlots(itemSlot, 45);
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private void swapSlots(int from, int to) {
        int slot = from < 9 ? from + 36 : from;
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, to, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
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