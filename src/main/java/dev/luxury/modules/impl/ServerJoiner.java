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
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
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
    ModeSetting serverSelection = new ModeSetting("Сервер", "SpookyTime Duels",
            new String[]{"ReallyWorld", "SpookyTime Duels"});
    SliderSetting griefSelection = new SliderSetting("Номер грифа", 1, 1, 54, 1);

    enum State { SELECT_SLOT, OPEN_MENU, CLICK_JOIN, WAIT_DIAMOND, DONE }
    private State spookyState = State.SELECT_SLOT;
    private long lastActionTime = 0;

    private boolean waitingForGui = false;
    private int reallyWorldState = 0;
    private long lastCompassClick = 0;
    private int tickCounter = 0;

    public ServerJoiner() {
        addSettings(serverSelection, griefSelection);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        griefSelection.setVisible(serverSelection.get().equals("ReallyWorld"));

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            onDisable();
            return;
        }

        if (tickCounter % 20 == 0) {
            Network.tick();
        }

        long now = System.currentTimeMillis();

        if (serverSelection.get().equals("SpookyTime Duels")) {
            handleSpookyTimeDuels(now);
        } else if (serverSelection.get().equals("ReallyWorld")) {
            handleReallyWorld(now);
        }
    }

    private void handleSpookyTimeDuels(long now) {
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            spookyState = State.DONE;
            this.toggle();
            return;
        }

        switch (spookyState) {
            case SELECT_SLOT:
                int targetSlot = 4;
                ensureSelectedSlot(targetSlot);
                spookyState = State.OPEN_MENU;
                lastActionTime = now;
                break;

            case OPEN_MENU:
                ensureSelectedSlot(4);
                if (isSixRowContainerOpen()) {
                    spookyState = State.CLICK_JOIN;
                    lastActionTime = now;
                    break;
                }
                if (now - lastActionTime >= 150) {
                    interactItem(Hand.MAIN_HAND);
                    lastActionTime = now;
                }
                break;

            case CLICK_JOIN:
                ensureSelectedSlot(4);
                if (!isSixRowContainerOpen()) {
                    spookyState = State.OPEN_MENU;
                    lastActionTime = now;
                    break;
                }
                if (now - lastActionTime >= 150) {
                    clickMenuSlot(14);
                    lastActionTime = now;
                }
                spookyState = State.WAIT_DIAMOND;
                break;

            case WAIT_DIAMOND:
                ensureSelectedSlot(4);
                if (mc.player.getInventory().getStack(0).getItem() == Items.DIAMOND_SWORD) {
                    if (mc.player.currentScreenHandler != null && mc.currentScreen != null) {
                        mc.player.closeHandledScreen();
                    }
                    spookyState = State.DONE;
                    this.toggle();
                    break;
                }

                if (isSixRowContainerOpen() && now - lastActionTime >= 200) {
                    clickMenuSlot(14);
                    lastActionTime = now;
                }

                if (!isSixRowContainerOpen()) {
                    spookyState = State.OPEN_MENU;
                    lastActionTime = now;
                }
                break;

            case DONE:
                this.toggle();
                break;
        }
    }

    private void handleReallyWorld(long now) {
        if (mc.currentScreen instanceof GenericContainerScreen container) {
            handleContainerGUI(container, now);
        }
        else if (reallyWorldState == 0 && now - lastCompassClick >= 2000 && mc.player.age > 20) {
            clickCompass();
            reallyWorldState = 1;
            lastCompassClick = now;
        }
        else if (reallyWorldState == 1 && now - lastCompassClick >= 500) {
            reallyWorldState = 2;
            lastActionTime = now - 1000;
        }
    }

    private boolean isSixRowContainerOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return false;
        GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
        return screen.getScreenHandler().slots.size() >= 54;
    }

    private void clickMenuSlot(int slotIndex) {
        if (!(mc.currentScreen instanceof GenericContainerScreen container)) return;
        var handler = container.getScreenHandler();
        if (handler == null) return;
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) return;

        mc.interactionManager.clickSlot(handler.syncId, slotIndex, 0, SlotActionType.PICKUP, mc.player);
    }

    private void ensureSelectedSlot(int idx) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getInventory().selectedSlot != idx) {
            mc.player.getInventory().selectedSlot = idx;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(idx));
        }
    }

    private void interactItem(Hand hand) {
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, hand);
        }
    }

    private void clickCompass() {
        int compassSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COMPASS) {
                compassSlot = i;
                break;
            }
        }

        if (compassSlot == -1) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.COMPASS) {
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

            if (serverSelection.get().equals("ReallyWorld") && Network.isReallyWorld()) {
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
            else if (serverSelection.get().equals("SpookyTime Duels") && Network.isSpookyTime()) {
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
            reallyWorldState = 0;
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
                        if (serverSelection.get().equals("SpookyTime Duels")) {
                            // Сброс состояния для SpookyTime
                            spookyState = State.SELECT_SLOT;
                            lastActionTime = System.currentTimeMillis();
                        } else {
                            clickCompass();
                            reallyWorldState = 1;
                            lastCompassClick = System.currentTimeMillis();
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (serverSelection.get().equals("SpookyTime Duels")) {
            spookyState = State.SELECT_SLOT;
        } else {
            reallyWorldState = 0;
        }

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

            if (serverSelection.get().equals("ReallyWorld")) {
                reallyWorldState = 0;
                lastCompassClick = System.currentTimeMillis() - 3000;
            }
        }).start();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (serverSelection.get().equals("SpookyTime Duels")) {
            spookyState = State.SELECT_SLOT;
        } else {
            reallyWorldState = 0;
        }

        lastActionTime = 0;
        lastCompassClick = 0;
        waitingForGui = false;
    }
}