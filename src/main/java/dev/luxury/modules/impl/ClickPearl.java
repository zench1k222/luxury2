package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.KeySetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickPearl",
        category = Category.Player
)
public class ClickPearl extends Module {
    private final KeySetting keySetting = new KeySetting("Бинд", GLFW.GLFW_MOUSE_BUTTON_3);
    private final ModeSetting mode = new ModeSetting("Режим", "Legit", new String[]{"Legit", "Hotbar", "All"});
    private final BooleanSetting delaySetting = new BooleanSetting("Задержка", true);

    private boolean shouldThrow = false;
    private boolean isThrowing = false;
    private int throwCooldown = 0;
    private double lastMotionX = 0;
    private double lastMotionZ = 0;
    private boolean wasMoving = false;

    public ClickPearl() {
        addSettings(keySetting, mode, delaySetting);
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
        if (throwCooldown > 0) {
            throwCooldown--;
            return;
        }

        if (!shouldThrow) {
            if (isThrowing) {
                resumeMovement();
                isThrowing = false;
            }
            return;
        }

        shouldThrow = false;

        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (mode.is("Legit")) {
            throwPearlLegit();
        }
        if (mode.is("Hotbar")) {
            throwPearlHotbar();
        }
        if (mode.is("All")) {
            throwPearlAll();
        }
    }

    private void throwPearlLegit() {
        if (isThrowing) return;

        wasMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
        lastMotionX = mc.player.getVelocity().x;
        lastMotionZ = mc.player.getVelocity().z;

        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;

        isThrowing = true;
        throwCooldown = 2;

        if (throwPearlSimple()) {
            if (delaySetting.get()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}

                    mc.execute(() -> {
                        if (isEnabled() && mc.player != null) {
                            resumeMovement();
                            isThrowing = false;
                        }
                    });
                }).start();
            } else {
                resumeMovement();
                isThrowing = false;
            }
        } else {
            resumeMovement();
            isThrowing = false;
        }
    }

    private void throwPearlHotbar() {
        throwPearlHotbarOnly();
    }

    private void throwPearlAll() {
        throwPearlSimple();
    }

    private boolean throwPearlSimple() {
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return true;
        }

        if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return true;
        }

        return false;
    }

    private boolean throwPearlHotbarOnly() {
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return true;
        }

        if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return true;
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
            return true;
        }

        return false;
    }

    private void resumeMovement() {
        if (!wasMoving || mc.player == null) return;

        mc.player.setVelocity(lastMotionX, mc.player.getVelocity().y, lastMotionZ);
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
        shouldThrow = false;
        isThrowing = false;
        throwCooldown = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        shouldThrow = false;
        isThrowing = false;
    }
}