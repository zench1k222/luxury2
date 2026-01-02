package dev.luxury.modules.impl.other;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.KeySetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ElytraHelper",
        desc = "Быстрое взаимодействие с элитрой",
        category = Category.Player
)
public class ElytraHelper extends Module {

    private final KeySetting elytraKey = new KeySetting("Кнопка элитры", GLFW.GLFW_MOUSE_BUTTON_5);
    private final KeySetting fireworkKey = new KeySetting("Кнопка фейерверка", GLFW.GLFW_MOUSE_BUTTON_4);
    private final BooleanSetting autoTakeoff = new BooleanSetting("Авто-взлёт", true);

    private int takeoffTicks = 0;
    private boolean waitingToGlide = false;
    private boolean shouldSwapElytra = false;
    private boolean shouldUseFirework = false;

    private boolean movementBlocked = false;
    private int movementBlockTicks = 0;
    private boolean wasForward = false;
    private boolean wasBack = false;
    private boolean wasLeft = false;
    private boolean wasRight = false;
    private boolean wasJump = false;
    private boolean wasSprint = false;
    private boolean wasSneak = false;

    public ElytraHelper() {
        addSettings(elytraKey, fireworkKey, autoTakeoff);
        elytraKey.setMouse(true);
        fireworkKey.setMouse(true);
    }

    @EventTarget
    public void onMouse(EventMouseInput e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (elytraKey.isMouse() && e.getButton() == elytraKey.getValue() && e.getAction() == 1) {
            shouldSwapElytra = true;
        }

        if (fireworkKey.isMouse() && e.getButton() == fireworkKey.getValue() && e.getAction() == 1) {
            shouldUseFirework = true;
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (movementBlocked) {
            blockMovement();
            movementBlockTicks++;

            if (movementBlockTicks >= 4) {
                restoreMovement();
                movementBlocked = false;
                movementBlockTicks = 0;
            }
        }

        if (shouldSwapElytra) {
            shouldSwapElytra = false;
            saveMovementState();
            blockMovement();
            movementBlocked = true;
            movementBlockTicks = 0;
            handleElytraSwap();
        }

        if (shouldUseFirework) {
            shouldUseFirework = false;
            handleFirework();
        }

        if (autoTakeoff.get()) {
            handleAutoTakeoff();
        }
    }

    private void saveMovementState() {
        wasForward = mc.options.forwardKey.isPressed();
        wasBack = mc.options.backKey.isPressed();
        wasLeft = mc.options.leftKey.isPressed();
        wasRight = mc.options.rightKey.isPressed();
        wasJump = mc.options.jumpKey.isPressed();
        wasSprint = mc.options.sprintKey.isPressed();
        wasSneak = mc.options.sneakKey.isPressed();
    }

    private void blockMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    private void restoreMovement() {
        mc.options.forwardKey.setPressed(wasForward);
        mc.options.backKey.setPressed(wasBack);
        mc.options.leftKey.setPressed(wasLeft);
        mc.options.rightKey.setPressed(wasRight);
        mc.options.jumpKey.setPressed(wasJump);
        mc.options.sprintKey.setPressed(wasSprint);
        mc.options.sneakKey.setPressed(wasSneak);
    }

    private void handleElytraSwap() {
        ItemStack equipped = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (equipped.getItem() == Items.ELYTRA) {
            int chestPlateSlot = findChestplate();
            int elytraSlot = findItemSlot(Items.ELYTRA);

            if (chestPlateSlot != -1) {
                swapSlots(chestPlateSlot, 6);
            } else {
                swapSlots(elytraSlot == -1 ? 6 : elytraSlot, 6);
            }
        } else {
            int elytraSlot = findItemSlot(Items.ELYTRA);
            if (elytraSlot != -1) {
                swapSlots(elytraSlot, 6);
            }
        }
    }

    private void handleFirework() {
        ItemStack equipped = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (equipped.getItem() != Items.ELYTRA) return;

        useFirework();
    }

    private void handleAutoTakeoff() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }

        if (mc.player.isGliding()) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }

        if (mc.player.isOnGround() && !waitingToGlide) {
            mc.player.jump();
            waitingToGlide = true;
            takeoffTicks = 0;
            return;
        }

        if (waitingToGlide) {
            takeoffTicks++;

            if (takeoffTicks >= 2 && mc.player.getVelocity().y < -0.08 && !mc.player.isGliding()) {
                startFlying();
                waitingToGlide = false;
                takeoffTicks = 0;
            }

            if (takeoffTicks > 10) {
                waitingToGlide = false;
                takeoffTicks = 0;
            }
        }
    }

    private void useFirework() {
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return;
        }

        if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return;
        }

        int fireworkSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {fireworkSlot = i;break;}
        }

        if (fireworkSlot != -1) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = fireworkSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = currentSlot;
            return;
        }

        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                int currentSlot = mc.player.getInventory().selectedSlot;

                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, currentSlot, SlotActionType.SWAP, mc.player);

                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, currentSlot, SlotActionType.SWAP, mc.player);
                return;
            }
        }
    }

    private void startFlying() {mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private int findChestplate() {
        Item[] chestplates = {
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
        };
        for (Item item : chestplates) {
            int slot = findItemSlot(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private void swapSlots(int from, int armorSlot) {
        int slot = from < 9 ? from + 36 : from;
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, armorSlot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldSwapElytra = false;
        shouldUseFirework = false;
        waitingToGlide = false;
        takeoffTicks = 0;
        movementBlocked = false;
        movementBlockTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldSwapElytra = false;
        shouldUseFirework = false;
        waitingToGlide = false;
        takeoffTicks = 0;

        if (movementBlocked) {
            restoreMovement();
            movementBlocked = false;
            movementBlockTicks = 0;
        }
    }
}