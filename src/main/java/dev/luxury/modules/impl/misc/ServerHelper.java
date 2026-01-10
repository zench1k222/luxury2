package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.*;
import dev.luxury.utils.player.InventoryUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(
        name = "ServerHelper",
        desc = "Помощник для серверов",
        category = Category.Misc
)
public class ServerHelper extends Module {
    private final ModeSetting serverMode = new ModeSetting("Сервер", "HolyWorld",
            new String[]{"ReallyWorld", "HolyWorld", "FunTime"});
    private final BooleanSetting consumablesTimer = new BooleanSetting("Таймер расходников", true);
    private final BooleanSetting legitMode = new BooleanSetting("Легитный режим", true);

    private final Map<String, KeySetting> keyBindings = new HashMap<>();
    private int state = 0;
    private long lastActionTime = 0;

    private boolean wasForwardPressed = false;
    private boolean wasBackwardPressed = false;
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private boolean wasJumpPressed = false;
    private boolean wasSneakPressed = false;
    private boolean wasSprintPressed = false;

    private int originalSlot = -1;
    private Item originalItem = null;

    public ServerHelper() {
        addSettings(serverMode, consumablesTimer, legitMode);
        initKeyBindings();
    }

    private void initKeyBindings() {
        keyBindings.put("antiflight", new KeySetting("Анти полет", GLFW.GLFW_KEY_R));
        keyBindings.put("expscroll", new KeySetting("Свиток опыта", GLFW.GLFW_KEY_T));
        keyBindings.put("dtrap", new KeySetting("Взрывная трапка", GLFW.GLFW_KEY_Y));
        keyBindings.put("trap_holy", new KeySetting("Обычная трапка", GLFW.GLFW_KEY_U));
        keyBindings.put("stan", new KeySetting("Стан", GLFW.GLFW_KEY_I));
        keyBindings.put("ditem", new KeySetting("Взрывная штучка", GLFW.GLFW_KEY_O));
        keyBindings.put("snow", new KeySetting("Снежок заморозка", GLFW.GLFW_KEY_P));
        keyBindings.put("bojaura", new KeySetting("Божья аура", GLFW.GLFW_KEY_L));
        keyBindings.put("trap", new KeySetting("Трапка", GLFW.GLFW_KEY_K));
        keyBindings.put("plast", new KeySetting("Пласт", GLFW.GLFW_KEY_J));
        keyBindings.put("sugar", new KeySetting("Явная пыль", GLFW.GLFW_KEY_H));
        keyBindings.put("fireSwirl", new KeySetting("Огненный смерч", GLFW.GLFW_KEY_G));
        keyBindings.put("disorientation", new KeySetting("Дезориентация", GLFW.GLFW_KEY_F));
        keyBindings.put("tikva", new KeySetting("Светильник Джека", GLFW.GLFW_KEY_V));
        keyBindings.put("exp", new KeySetting("Пузырь опыта", GLFW.GLFW_KEY_B));

        for (KeySetting setting : keyBindings.values()) {
            addSetting(setting);
        }
    }

    public void addSetting(Setting setting) {
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            this.toggle();
            return;
        }

        handleKeyPresses();
    }

    private void handleKeyPresses() {
        keyBindings.forEach((key, setting) -> {
            if (GLFW.glfwGetKey(mc.getWindow().getHandle(), setting.getValue()) == GLFW.GLFW_PRESS) {
                useItem(key);
            }
        });
    }

    private void useItem(String itemKey) {
        Item item = getItemByKey(itemKey);
        if (item == null) return;

        int slot = findItemSlot(item);
        if (slot == -1) return;

        originalSlot = mc.player.getInventory().selectedSlot;
        originalItem = mc.player.getInventory().getStack(originalSlot).getItem();

        if (legitMode.get()) {
            saveMovementState();
            stopMovement();
        }

        performSwap(item, slot);

        if (legitMode.get()) {
            restoreMovementState();
        }

        if (originalSlot != -1 && originalItem != null) {
            restoreOriginalItem(originalSlot, originalItem);
        }
    }

    private void restoreOriginalItem(int slot, Item item) {
        if (slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        } else {
            InventoryUtil.swapSlots(slot, 0);
        }

        // Если предмет в инвентаре, возвращаем его
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                if (i < 9) {
                    mc.player.getInventory().selectedSlot = i;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                } else {
                    InventoryUtil.swapSlots(i, 0);
                }
                return;
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

    private void performSwap(Item item, int slot) {
        int originalSlot = mc.player.getInventory().selectedSlot;

        if (slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        } else {
            InventoryUtil.swapSlots(slot, 0);
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (slot < 9) {
            mc.player.getInventory().selectedSlot = originalSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        } else {
            InventoryUtil.swapSlots(0, originalSlot);
        }
    }

    private Item getItemByKey(String key) {
        return switch (key) {
            case "antiflight" -> Items.FIREWORK_STAR;
            case "expscroll" -> Items.FLOWER_BANNER_PATTERN;
            case "dtrap" -> Items.PRISMARINE_SHARD;
            case "trap_holy" -> Items.POPPED_CHORUS_FRUIT;
            case "stan" -> Items.NETHER_STAR;
            case "ditem" -> Items.FIRE_CHARGE;
            case "snow" -> Items.SNOWBALL;
            case "bojaura" -> Items.PHANTOM_MEMBRANE;
            case "trap" -> Items.NETHERITE_SCRAP;
            case "plast" -> Items.DRIED_KELP;
            case "sugar" -> Items.SUGAR;
            case "fireSwirl" -> Items.FIRE_CHARGE;
            case "disorientation" -> Items.ENDER_EYE;
            case "tikva" -> Items.JACK_O_LANTERN;
            case "exp" -> Items.EXPERIENCE_BOTTLE;
            default -> null;
        };
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;

        if (event.getPacket() instanceof DisconnectS2CPacket packet) {
            String message = packet.reason().getString().toLowerCase();

            if (message.contains("сервер переполнен") ||
                    message.contains("подождите несколько секунд") ||
                    message.contains("вы уже подключены")) {
                this.toggle();
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = 0;
        lastActionTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        state = 0;
        lastActionTime = 0;
        if (legitMode.get()) {
            restoreMovementState();
        }
    }
}