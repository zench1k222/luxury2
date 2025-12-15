package dev.luxury.utils.player;

import net.minecraft.scoreboard.*;
import org.apache.commons.lang3.StringUtils;

import static dev.luxury.utils.player.InventoryUtil.mc;

public class ServerUtil {
    public static String server = "Vanilla";
    public static String selectedServerMode = "reallyworld";

    public ServerUtil(){
        server = getServer();
    }

    private int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        switch (server) {
            case "FunTime" -> {
                if (objective != null) {
                    String[] string = objective.getDisplayName().getString().split("-");
                    if (string.length > 1) return Integer.parseInt(string[1]);
                }
            }
            case "HolyWorld" -> {
                for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (!text.isEmpty()) {
                        String string = StringUtils.substringBetween(text, "#", " -◆-");
                        if (string != null && !string.isEmpty()) return Integer.parseInt(string.replace(" (1.20)", ""));
                    }
                }
            }
        }
        return -1;
    }

    public String getServer() {
        if (PlayerIntersectionUtil.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        else if (serverIp.contains("aresmine") || serverIp.contains("craftyou")) return "AresMine";
        return "Vanilla";
    }
    public boolean isCopyTime() {return selectedServerMode.equals("funtime");}
    public boolean isFunTime() {return selectedServerMode.equals("funtime");}
    public boolean isReallyWorld() {return selectedServerMode.equals("reallyworld");}
    public boolean isHolyWorld() {return selectedServerMode.equals("holyworld");}
    public boolean isVanilla() {return selectedServerMode.equals("vanilla");}
    public boolean isAresMine() {return selectedServerMode.equals("aresmine");}
}
