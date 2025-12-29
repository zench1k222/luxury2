package dev.luxury.utils.managers;

import dev.luxury.utils.player.ServerUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;

import static dev.luxury.modules.impl.targetesp.mode.Circle.mc;

public class RCTManager {

    String anarchy = "";
    String grief2 = "";

    @Native
    public void run() throws Exception {
        if (ServerUtil.isConnected("funtime")) {
            runFuntimeLogic();
        } else if (ServerUtil.isConnected("reallyworld") && ServerUtil.isConnected("playrw")) {
            runReallyWorldLogic();
        } else {
            System.err.println("Неизвестный сервер или не подключен к целевому серверу");
        }
    }

    @Native
    private void runFuntimeLogic() {
        for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives().toArray(new ScoreboardObjective[0])) {
            String an = team.getDisplayName().getString();
            if (an.contains("Анархия-")) {
                anarchy = an.split("Анархия-")[1];
                mc.player.networkHandler.sendChatCommand("hub");

                mc.player.networkHandler.sendChatCommand("an" + anarchy);

                String finalAnarchy = anarchy;
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        mc.player.networkHandler.sendChatCommand("an" + finalAnarchy);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
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
            }
        }
    }

    @Native
    private void startGriefJoinSequence(String griefNumber) {
        mc.player.networkHandler.sendChatCommand("hub");
        System.out.println("Отправлена команда /hub");

        new Thread(() -> {
            try {
                Thread.sleep(1500);

                mc.execute(() -> {
                    mc.player.networkHandler.sendChatCommand("menu");
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
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Native
    private void clickContainerSlot(int slotIndex) {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            if (mc.player != null && mc.interactionManager != null) {
                int syncId = mc.player.currentScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP, mc.player);
            }
        }
    }
}