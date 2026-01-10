package dev.luxury.modules.impl.player;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.KeySetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickPearl",
        category = Category.Player
)
public class ClickPearl extends Module {
    private final KeySetting keySetting = new KeySetting("Бинд", GLFW.GLFW_MOUSE_BUTTON_3);
    private final BooleanSetting legitMode = new BooleanSetting("Легитный режим", true);
    private final BooleanSetting randomDelay = new BooleanSetting("Случайная задержка", true);

    private boolean shouldThrow = false;
    private boolean isThrowing = false;
    private int throwTicks = 0;
    private long throwStartTime = 0;
    private long randomDelayTime = 0;

    private boolean wasForwardPressed = false;
    private boolean wasBackwardPressed = false;
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private boolean wasJumpPressed = false;
    private boolean wasSneakPressed = false;
    private boolean wasSprintPressed = false;

    private int originalHotbarSlot = -1;
    private int originalSelectedSlot = -1;
    private boolean pearlWasInOffhand = false;
    private boolean pearlWasInMainHand = false;

    private Item originalItem = null;

    public ClickPearl() {
        addSettings(keySetting, legitMode, randomDelay);
        keySetting.setMouse(true);
    }

    @EventTarget
    public void onMouse(EventMouseInput e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (keySetting.isMouse() && e.getButton() == keySetting.getValue() && e.getAction() == 1) {
            shouldThrow = true;
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (shouldThrow && !isThrowing) {
            startPearlThrow();
            shouldThrow = false;
        }

        if (isThrowing) {
            handleThrowProcess();
        }
    }

    private void startPearlThrow() {
        isThrowing = true;
        throwTicks = 0;
        throwStartTime = System.currentTimeMillis();

        if (randomDelay.get()) {
            randomDelayTime = (long) (Math.random() * 100) + 100;
        } else {
            randomDelayTime = 100;
        }

        originalHotbarSlot = -1;
        originalSelectedSlot = -1;
        pearlWasInOffhand = false;
        pearlWasInMainHand = false;

        if (legitMode.get()) {
            saveMovementState();
            stopMovement();
        }

        if (!prepareAndThrowPearl()) {
            cancelThrow();
        }
    }

    private void saveMovementState() {
        wasForwardPressed = mc.player.input.playerInput.forward();
        wasBackwardPressed = mc.player.input.playerInput.backward();
        wasLeftPressed = mc.player.input.playerInput.left();
        wasRightPressed = mc.player.input.playerInput.right();
        wasJumpPressed = mc.player.input.playerInput.jump();
        wasSneakPressed = mc.player.input.playerInput.sneak();
        wasSprintPressed = mc.player.input.playerInput.sprint();
    }

    private void stopMovement() {
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;

        mc.player.input.playerInput = new net.minecraft.util.PlayerInput(
                false,
                false,
                false,
                false,
                false,
                wasSneakPressed,
                false
        );

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);

        if (mc.player.input != null) {
            mc.player.input.tick();
        }
    }

    private void restoreMovementState() {
        mc.player.input.movementForward = wasForwardPressed ? 1.0f : (wasBackwardPressed ? -1.0f : 0);
        mc.player.input.movementSideways = wasLeftPressed ? -1.0f : (wasRightPressed ? 1.0f : 0);

        mc.player.input.playerInput = new net.minecraft.util.PlayerInput(
                wasForwardPressed,
                wasBackwardPressed,
                wasLeftPressed,
                wasRightPressed,
                wasJumpPressed,
                wasSneakPressed,
                wasSprintPressed
        );

        mc.options.forwardKey.setPressed(wasForwardPressed);
        mc.options.backKey.setPressed(wasBackwardPressed);
        mc.options.leftKey.setPressed(wasLeftPressed);
        mc.options.rightKey.setPressed(wasRightPressed);
        mc.options.jumpKey.setPressed(wasJumpPressed);
        mc.options.sprintKey.setPressed(wasSprintPressed);

        if (mc.player.input != null) {
            mc.player.input.tick();
        }
    }

    private void handleThrowProcess() {
        throwTicks++;

        if (System.currentTimeMillis() - throwStartTime < randomDelayTime) {
            return;
        }

        if (legitMode.get()) {
            if (throwTicks == 8) {
                performPearlThrow();

                if (throwTicks >= 10) {
                    restoreHotbarItem();

                    restoreMovementState();
                    isThrowing = false;
                }
            }
        } else {
            if (throwTicks >= 1) {
                isThrowing = false;
            }
        }

        if (System.currentTimeMillis() - throwStartTime > 1000) {
            if (legitMode.get()) {
                restoreMovementState();
            }
            isThrowing = false;
        }
    }

    private boolean prepareAndThrowPearl() {
        originalSelectedSlot = mc.player.getInventory().selectedSlot;

        if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
            pearlWasInOffhand = true;
            return true;
        }

        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            pearlWasInMainHand = true;
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                originalHotbarSlot = i;
                mc.player.getInventory().selectedSlot = i;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                return true;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                int hotbarSlot = findHotbarSwapSlot();
                if (hotbarSlot != -1) {
                    originalHotbarSlot = hotbarSlot;

                    if (legitMode.get()) {
                        legitSwapItems(i, hotbarSlot);
                    } else {
                        fastSwapItems(i, hotbarSlot);
                    }

                    mc.player.getInventory().selectedSlot = hotbarSlot;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private void performPearlThrow() {
        if (pearlWasInOffhand) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else if (pearlWasInMainHand) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } else {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }

        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void restoreHotbarItem() {
        if (originalHotbarSlot != -1 && originalSelectedSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSelectedSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSelectedSlot));
        }

        // Возвращаем оригинальный предмет в исходный слот
        if (originalHotbarSlot != -1) {
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                    if (i < 9) {
                        mc.player.getInventory().selectedSlot = i;
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                    } else {
                        legitSwapItems(i, originalHotbarSlot);
                    }
                    break;
                }
            }
        }
    }

    private int findHotbarSwapSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return mc.player.getInventory().selectedSlot;
    }

    private void legitSwapItems(int fromSlot, int toSlot) {
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                fromSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                toSlot < 9 ? toSlot + 36 : toSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    fromSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );
        }
    }

    private void fastSwapItems(int fromSlot, int toSlot) {
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                fromSlot,
                toSlot < 9 ? toSlot + 36 : toSlot,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private void cancelThrow() {
        if (legitMode.get()) {
            restoreMovementState();
        }
        isThrowing = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldThrow = false;
        isThrowing = false;
        throwTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldThrow = false;
        if (isThrowing && legitMode.get()) {
            restoreMovementState();
        }
        isThrowing = false;
    }
}