package dev.luxury.utils.managers;

import dev.luxury.utils.player.ServerUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;

public class RCTManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private String anarchy = "";
    private String grief2 = "";
    private int funtimeDelayTicks = 0;
    private String funtimeCommand = null;
    private boolean isFuntimeProcessing = false;
    private boolean tickHandlerRegistered = false;

    private void ensureTickHandler() {
        if (!tickHandlerRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
            tickHandlerRegistered = true;
        }
    }

    @Native
    public void run() throws Exception {
        ensureTickHandler();

        if (ServerUtil.isConnected("funtime")) {
            runFuntimeLogic();
        } else if (ServerUtil.isConnected("reallyworld") || ServerUtil.isConnected("playrw")) {
            runReallyWorldLogic();
        } else {
            System.err.println("Неизвестный сервер или не подключен к целевому серверу");
        }
    }

    @Native
    private void runFuntimeLogic() {
        if (mc.player == null || mc.world == null) {
            System.err.println("Игрок или мир null в runFuntimeLogic");
            return;
        }

        if (isFuntimeProcessing) {
            System.err.println("Funtime уже обрабатывается");
            return;
        }

        isFuntimeProcessing = true;

        try {
            for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives()) {
                String an = team.getDisplayName().getString();
                if (an.contains("Анархия-")) {
                    anarchy = an.split("Анархия-")[1].trim();
                    System.out.println("Найдена анархия: " + anarchy);

                    if (mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendChatMessage("/hub");
                        System.out.println("Отправлена команда: /hub");
                    }

                    funtimeCommand = "an" + anarchy;
                    funtimeDelayTicks = 20;

                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка в runFuntimeLogic: " + e.getMessage());
            e.printStackTrace();
            isFuntimeProcessing = false;
        }
    }

    private void onClientTick(MinecraftClient client) {
        if (funtimeDelayTicks > 0) {
            funtimeDelayTicks--;
            if (funtimeDelayTicks == 0 && funtimeCommand != null) {
                if (mc.player != null && mc.player.networkHandler != null) {
                    try {
                        mc.player.networkHandler.sendChatMessage("/" + funtimeCommand);
                        System.out.println("Отправлена команда: /" + funtimeCommand);
                    } catch (Exception e) {
                        System.err.println("Ошибка отправки команды: " + e.getMessage());
                    }
                }
                funtimeCommand = null;
                isFuntimeProcessing = false;
            }
        }
    }

    @Native
    private void runReallyWorldLogic() {
        for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives().toArray(new ScoreboardObjective[0])) {
            String grief = team.getDisplayName().getString();
            if (grief.contains("ГРИФ #")) {
                grief2 = grief.split("ГРИФ #")[1].trim();

                startGriefJoinSequence(grief2);
                break;
            } else if (grief.contains("ГРИФ #MEGA")) {

                startMegaGriefJoinSequence();
                break;
            }
        }
    }

    @Native
    private void startGriefJoinSequence(String griefNumber) {
        if (mc.player == null || mc.player.networkHandler == null) {
            System.err.println("Игрок или networkHandler null в startGriefJoinSequence");
            return;
        }

        mc.player.networkHandler.sendChatCommand("hub");
        System.out.println("Отправлена команда /hub");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                mc.player.networkHandler.sendChatCommand("hub");

                mc.execute(() -> {
                    if (mc.player != null && mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendChatMessage("/menu");
                    }
                });

                Thread.sleep(1000);

                int maxWaits = 20;
                boolean menuOpened = false;
                for (int i = 0; i < maxWaits; i++) {
                    if (mc.currentScreen instanceof GenericContainerScreen) {
                        menuOpened = true;
                        break;
                    }
                    Thread.sleep(50);
                }

                if (!menuOpened) {
                    return;
                }

                mc.execute(() -> {
                    clickContainerSlot(21);
                });

                Thread.sleep(800);

                try {
                    int slotNumber = Integer.parseInt(griefNumber);
                    int slotIndex = slotNumber - 1;

                    mc.execute(() -> {
                        clickContainerSlot(slotIndex);
                    });

                } catch (NumberFormatException e) {
                    System.err.println("Ошибка парсинга номера грифа: " + griefNumber);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Native
    private void startMegaGriefJoinSequence() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        mc.player.networkHandler.sendChatCommand("hub");
        System.out.println("Отправлена команда /hub");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                mc.player.networkHandler.sendChatCommand("hub");

                mc.execute(() -> {
                    if (mc.player != null && mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendChatMessage("/menu");
                    }
                });

                Thread.sleep(1000);

                int maxWaits = 20;
                boolean menuOpened = false;
                for (int i = 0; i < maxWaits; i++) {
                    if (mc.currentScreen instanceof GenericContainerScreen) {
                        menuOpened = true;
                        break;
                    }
                    Thread.sleep(50);
                }

                if (!menuOpened) {
                    return;
                }

                mc.execute(() -> {
                    clickContainerSlot(23);
                });

                Thread.sleep(800);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Native
    private void clickContainerSlot(int slotIndex) {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            if (mc.player != null && mc.interactionManager != null) {
                try {
                    int syncId = mc.player.currentScreenHandler.syncId;
                    mc.interactionManager.clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP, mc.player);
                    System.out.println("Клик по слоту: " + slotIndex);
                } catch (Exception e) {
                    System.err.println("Ошибка клика по слоту: " + e.getMessage());
                }
            }
        }
    }
}