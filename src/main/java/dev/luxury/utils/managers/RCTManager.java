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
        for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives().toArray(new ScoreboardObjective[0])) {
            if (ServerUtil.isConnected("funtime")) {
                String an = team.getDisplayName().getString();
                if (an.contains("Анархия-")) {
                    anarchy = an.split("Анархия-")[1];
                    mc.player.networkHandler.sendChatCommand("hub");
                    break;
                }
            }
            mc.player.networkHandler.sendChatCommand("an" + anarchy);
            String finalAnarchy = anarchy;
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mc.player.networkHandler.sendChatCommand("an" + finalAnarchy);
            }).start();
        }

        if (ServerUtil.isConnected("reallyworld") && ServerUtil.isConnected("playrw")) {
            for (ScoreboardObjective team : mc.world.getScoreboard().getObjectives().toArray(new ScoreboardObjective[0])) {
                String grief = team.getDisplayName().getString();
                if (grief.contains("ГРИФ #")) {
                    grief2 = grief.split("ГРИФ #")[1];
                    mc.player.networkHandler.sendChatCommand("hub");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);

                            mc.execute(() -> mc.player.networkHandler.sendChatCommand("menu"));

                            Thread.sleep(500);

                            mc.execute(() -> clickContainerSlot(21));

                            Thread.sleep(500);

                            try {
                                int slotNumber = Integer.parseInt(grief2.trim());
                                int slotIndex = slotNumber - 1;
                                mc.execute(() -> clickContainerSlot(slotIndex));
                            } catch (NumberFormatException e) {
                                System.err.println("Ошибка парсинга номера: " + grief2);
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    break;
                }
            }
        }
    }

    @Native
    private void clickContainerSlot(int slotIndex) {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            assert mc.player != null;
            int syncId = mc.player.currentScreenHandler.syncId;

            mc.interactionManager.clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP, mc.player);
        } else {
            System.err.println("Контейнер не открыт!");
        }
    }
}
