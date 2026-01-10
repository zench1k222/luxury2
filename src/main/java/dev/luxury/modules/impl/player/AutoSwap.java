package dev.luxury.modules.impl.player;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.KeySetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Overwrite;

@ModuleAnnotation(
        name = "AutoSwap",
        desc = "Автоматически свапает предметы в оффхенд",
        category = Category.Player
)
public class AutoSwap extends Module {
    private final KeySetting bind = new KeySetting("Кнопка", GLFW.GLFW_MOUSE_BUTTON_5);
    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Totem",
            new String[]{"Totem", "Head", "Gapple", "Shield", "Crystal", "Pearl"});
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Shield",
            new String[]{"Totem", "Head", "Gapple", "Shield", "Crystal", "Pearl"});
    private final BooleanSetting legitMode = new BooleanSetting("Легитный режим", true);

    private boolean shouldSwap = false;
    private boolean isSwapping = false;
    private int swapTick = 0;
    private long swapStartTime = 0;

    private boolean wasForwardPressed = false;
    private boolean wasBackwardPressed = false;
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private boolean wasJumpPressed = false;
    private boolean wasSneakPressed = false;
    private boolean wasSprintPressed = false;

    public AutoSwap() {
        addSettings(bind, firstItem, secondItem, legitMode);
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
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (shouldSwap && !isSwapping) {
            startSwap();
            shouldSwap = false;
        }

        if (isSwapping) {
            handleSwapProcess();
        }
    }

    private void startSwap() {
        isSwapping = true;
        swapTick = 0;
        swapStartTime = System.currentTimeMillis();

        if (legitMode.get()) {
            saveMovementState();

            stopMovement();
        }

        Item targetItem = getSwapTarget();
        if (targetItem != null) {
            performSwap(targetItem);
        } else {
            isSwapping = false;
            if (legitMode.get()) {
                restoreMovementState();
            }
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

    private void handleSwapProcess() {
        swapTick++;

        if (legitMode.get()) {
            if (swapTick >= 8) {
                restoreMovementState();
                isSwapping = false;
            }
        } else {
            if (swapTick >= 1) {
                isSwapping = false;
            }
        }

        if (System.currentTimeMillis() - swapStartTime > 500) {
            if (legitMode.get()) {
                restoreMovementState();
            }
            isSwapping = false;
        }
    }

    private Item getSwapTarget() {
        ItemStack offhandItem = mc.player.getOffHandStack();
        Item firstType = getItemByType(firstItem.get());
        Item secondType = getItemByType(secondItem.get());

        if (offhandItem.isEmpty()) {
            return firstType != Items.AIR && hasItem(firstType) ? firstType :
                    secondType != Items.AIR && hasItem(secondType) ? secondType : null;
        }

        if (offhandItem.getItem() == firstType && secondType != Items.AIR && hasItem(secondType)) {
            return secondType;
        }

        if (firstType != Items.AIR && hasItem(firstType) && offhandItem.getItem() != firstType) {
            return firstType;
        }

        if (secondType != Items.AIR && hasItem(secondType) && offhandItem.getItem() != secondType) {
            return secondType;
        }

        return null;
    }

    private void performSwap(Item item) {
        int itemSlot = findItemSlot(item);
        if (itemSlot == -1) {
            isSwapping = false;
            if (legitMode.get()) {
                restoreMovementState();
            }
            return;
        }

        int guiSlot = itemSlot;
        if (itemSlot < 9) {
            guiSlot = itemSlot + 36;
        } else if (itemSlot >= 36) {
            guiSlot = itemSlot;
        }

        if (legitMode.get()) {
            performLegitSwap(guiSlot);
        } else {
            performFastSwap(guiSlot);
        }
    }

    private void performLegitSwap(int guiSlot) {
        try {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    guiSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    45,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );

            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getStack(i).isEmpty()) {
                        int returnSlot = i < 9 ? i + 36 : i;
                        mc.interactionManager.clickSlot(
                                mc.player.currentScreenHandler.syncId,
                                returnSlot,
                                0,
                                SlotActionType.PICKUP,
                                mc.player
                        );
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (legitMode.get()) {
                restoreMovementState();
            }
            isSwapping = false;
        }
    }

    private void performFastSwap(int guiSlot) {
        try {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    guiSlot,
                    40,
                    SlotActionType.SWAP,
                    mc.player
            );
        } catch (Exception e) {
            e.printStackTrace();
            isSwapping = false;
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

    private int findItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private Item getItemByType(String itemType) {
        return switch (itemType.toLowerCase()) {
            case "totem" -> Items.TOTEM_OF_UNDYING;
            case "head" -> Items.PLAYER_HEAD;
            case "gapple" -> Items.GOLDEN_APPLE;
            case "shield" -> Items.SHIELD;
            case "crystal" -> Items.END_CRYSTAL;
            case "pearl" -> Items.ENDER_PEARL;
            default -> Items.AIR;
        };
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldSwap = false;
        isSwapping = false;
        swapTick = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldSwap = false;
        isSwapping = false;
        // Восстанавливаем движение на всякий случай
        if (legitMode.get()) {
            restoreMovementState();
        }
    }

    @Overwrite
    public String getSuffix() {
        if (isSwapping) return "Swapping...";
        return null;
    }
}