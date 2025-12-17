package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.player.InventoryUtil;
import dev.luxury.utils.client.Network;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ServerJoiner",
        desc = "Авто заход на сервера",
        category = Category.Misc
)
public class ServerJoiner extends Module {
    ModeSetting serverSelection = new ModeSetting("Сервер", "SpookyTime Duels", new String[]{"ReallyWorld", "SpookyTime Duels"});
    SliderSetting griefSelection = new SliderSetting("Номер грифа", 1, 1, 54, 1);

    @NonFinal long lastActionTime;
    @NonFinal boolean isToggling;
    @NonFinal boolean retryDuels;
    @NonFinal int clickDelay;
    @NonFinal boolean waitingForGui;

    public ServerJoiner() {
        addSettings(serverSelection, griefSelection);
    }

    @EventTarget
    public void onTick(EventTick event) {
        griefSelection.setVisible(serverSelection.is("ReallyWorld"));

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            onDisable();
            return;
        }

        // Если ждем открытия GUI
        if (waitingForGui) {
            clickDelay++;
            if (clickDelay > 10) { // Ждем 10 тиков (0.5 секунд) для открытия GUI
                waitingForGui = false;
                clickDelay = 0;
            }
            return;
        }

        // Если нет GUI и игрок молодой (только зашел)
        if (mc.currentScreen == null && mc.player != null && mc.player.age < 100) {
            InventoryUtil.selectCompass();
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    mc.player.getInventory().selectedSlot,
                    mc.player.getYaw(),
                    mc.player.getPitch()
            ));
            waitingForGui = true;
            clickDelay = 0;
        }
        // Если открыт GUI контейнера
        else if (mc.currentScreen instanceof GenericContainerScreen container) {
            processContainer(container);
        }
    }

    private void processContainer(GenericContainerScreen container) {
        for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
            String itemName = container.getScreenHandler().slots.get(i).getStack().getName().getString().toLowerCase();

            if (serverSelection.is("ReallyWorld") && Network.isReallyWorld()) {
                processReallyWorld(itemName, i);
            } else if (serverSelection.is("SpookyTime Duels") && Network.isSpookyTime()) {
                processSpookyTime(itemName, i);
            }
        }
    }

    private void processReallyWorld(String itemName, int slot) {
        if (System.currentTimeMillis() - lastActionTime < 50) return;

        if (itemName.contains("гриферское выживание")) {
            InventoryUtil.clickSlotLegit(slot, 0, SlotActionType.PICKUP, false);
            lastActionTime = System.currentTimeMillis();
        } else {
            int numberGrief = (int) griefSelection.getValue();
            if (itemName.contains("гриф #" + numberGrief)) {
                InventoryUtil.clickSlotLegit(slot, 0, SlotActionType.PICKUP, false);
                lastActionTime = System.currentTimeMillis();
            }
        }
    }

    private void processSpookyTime(String itemName, int slot) {
        if (System.currentTimeMillis() - lastActionTime < 70) return;

        if (itemName.contains("» дуэли")) {
            mc.player.getInventory().selectedSlot = 0;
            InventoryUtil.clickSlotLegit(slot, 0, SlotActionType.PICKUP, false);
            lastActionTime = System.currentTimeMillis();
            retryDuels = true;
        } else if (itemName.contains("липкий поршень")) {
            InventoryUtil.clickSlotLegit(slot, 0, SlotActionType.PICKUP, false);
            lastActionTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) return;

        if (event.getPacket() instanceof DisconnectS2CPacket packet) {
            String message = packet.reason().getString().toLowerCase();
            if (message.contains("к сожалению сервер переполнен") ||
                    message.contains("подождите 20 секунд!") ||
                    message.contains("вы уже подключены на этот сервер!") ||
                    message.contains("подождите несколько секунд перед повторным подключением!") ||
                    message.contains("вы были кикнуты с сервера 1duels:") ||
                    message.contains("вы были кикнуты") ||
                    message.contains("большой поток игроков") ||
                    message.contains("сервер заполнен!")) {
                InventoryUtil.selectCompass();
                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND,
                        mc.player.getInventory().selectedSlot,
                        mc.player.getYaw(),
                        mc.player.getPitch()
                ));
                waitingForGui = true;
                clickDelay = 0;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        waitingForGui = false;
        clickDelay = 0;
        lastActionTime = 0;
        isToggling = false;
        retryDuels = false;

        // Даем небольшую задержку перед началом работы
        new Thread(() -> {
            try {
                Thread.sleep(500); // 0.5 секунды задержки
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (mc.player != null && mc.player.networkHandler != null) {
                InventoryUtil.selectCompass();
                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                        Hand.MAIN_HAND,
                        mc.player.getInventory().selectedSlot,
                        mc.player.getYaw(),
                        mc.player.getPitch()
                ));
                waitingForGui = true;
            }
        }).start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        waitingForGui = false;
        clickDelay = 0;
        lastActionTime = 0;
        isToggling = false;
        retryDuels = false;
    }
}