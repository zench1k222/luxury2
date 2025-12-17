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

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ModuleAnnotation(
        name = "ServerJoiner",
        desc = "Авто заход на сервера",
        category = Category.Misc
)
public class ServerJoiner extends Module {
    ModeSetting serverSelection = new ModeSetting("Сервер", "SpookyTime Duels", new String[]{"ReallyWorld", "SpookyTime Duels"});
    SliderSetting griefSelection = new SliderSetting("Номер грифа", 1, 1, 54, 1);

    @NonFinal long lastActionTime;
    @NonFinal long lastCompassClick;
    @NonFinal boolean waitingForGui;
    @NonFinal int state;
    @NonFinal int tickCounter;

    public ServerJoiner() {
        addSettings(serverSelection, griefSelection);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        griefSelection.setVisible(serverSelection.is("ReallyWorld"));

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            onDisable();
            return;
        }

        if (tickCounter % 20 == 0) {
            Network.tick();
        }

        long now = System.currentTimeMillis();

        if (mc.currentScreen instanceof GenericContainerScreen container) {
            handleContainerGUI(container, now);
        }
        else if (state == 0 && now - lastCompassClick >= 2000 && mc.player.age > 20) {
            clickCompass();
            state = 1;
            lastCompassClick = now;
        }
        else if (state == 1 && now - lastCompassClick >= 500) {
            state = 2;
            lastActionTime = now - 1000;
        }
    }

    private void clickCompass() {
        int compassSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.COMPASS) {
                compassSlot = i;
                break;
            }
        }

        if (compassSlot == -1) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.COMPASS) {
                    InventoryUtil.swapSlots(i, 0);
                    compassSlot = 0;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}
                    break;
                }
            }
        }

        if (compassSlot == -1) {
            return;
        }

        mc.player.getInventory().selectedSlot = compassSlot;

        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }

    private void handleContainerGUI(GenericContainerScreen container, long now) {
        String title = container.getTitle().getString();

        if (!title.toLowerCase().contains("сервер") && !title.toLowerCase().contains("server")) {
            return;
        }

        if (now - lastActionTime < 100) return;

        boolean clicked = false;

        for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
            var stack = container.getScreenHandler().getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            String itemName = stack.getName().getString();
            String lowerName = itemName.toLowerCase();


            if (serverSelection.is("ReallyWorld") && Network.isReallyWorld()) {
                if ((lowerName.contains("гриферское") && lowerName.contains("выживание")) ||
                        lowerName.contains("grief") || lowerName.contains("survival")) {
                    InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
                    clicked = true;
                    break;
                }

                int numberGrief = (int) griefSelection.getValue();
                if (lowerName.contains("гриф") && lowerName.contains("#" + numberGrief)) {
                    InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
                    clicked = true;
                    break;
                }
            }
            else if (serverSelection.is("SpookyTime Duels") && Network.isSpookyTime()) {
                if (lowerName.contains("дуэли") || lowerName.contains("duels") ||
                        lowerName.contains("» дуэли") || itemName.contains("» Дуэли")) {
                    mc.player.getInventory().selectedSlot = 0;
                    InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
                    clicked = true;
                    break;
                }

                if (lowerName.contains("липкий поршень") || lowerName.contains("sticky piston")) {
                    InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
                    clicked = true;
                    break;
                }
            }
        }

        if (clicked) {
            lastActionTime = now;
            state = 0;
            lastCompassClick = now;
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

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mc.player != null && mc.player.networkHandler != null) {
                        clickCompass();
                        state = 1;
                        lastCompassClick = System.currentTimeMillis();
                    }
                }).start();
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = 0;
        tickCounter = 0;
        lastActionTime = 0;
        lastCompassClick = 0;
        waitingForGui = false;

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            state = 0;
            lastCompassClick = System.currentTimeMillis() - 3000;
        }).start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        state = 0;
        lastActionTime = 0;
        lastCompassClick = 0;
        waitingForGui = false;
    }
}