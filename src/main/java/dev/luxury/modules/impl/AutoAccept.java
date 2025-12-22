package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.math.TimerUtils;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleAnnotation(
        name = "AutoAccept",
        desc = "Автоматически принимает запросы на телепортацию",
        category = Category.Misc
)
public class AutoAccept extends Module {

    private final TimerUtils pvpWatch = new TimerUtils();

    private final String[] teleportMessages = new String[]{
            "has requested teleport",
            "просит телепортироваться",
            "запрос на телепортацию от",
            "хочет телепортироваться к вам",
            "просит к вам телепортироваться",
            "teleport request from",
            "requested to teleport to you",
            "tpa к вам",
            "запросил телепорт"
    };

    private final Pattern playerNamePattern = Pattern.compile(
            "от игрока ([a-zA-Zа-яА-Я0-9_]{3,16})|" +
                    "игрок ([a-zA-Zа-яА-Я0-9_]{3,16}) просит|" +
                    "([a-zA-Zа-яА-Я0-9_]{3,16}) хочет|" +
                    "([a-zA-Zа-яА-Я0-9_]{3,16}) просит|" +
                    "([a-zA-Zа-яА-Я0-9_]{3,16}) has|" +
                    "teleport request from ([a-zA-Zа-яА-Я0-9_]{3,16})|" +
                    "tpa к вам от ([a-zA-Zа-яА-Я0-9_]{3,16})|" +
                    "запросил телепорт ([a-zA-Zа-яА-Я0-9_]{3,16})"
    );

    private boolean canAccept = false;
    private String pendingPlayer = null;
    private long lastAcceptTime = 0;
    private final long acceptCooldown = 1000;

    private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", true);

    public AutoAccept() {
        addSettings(friendSetting);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            String lowerMessage = message.toLowerCase();

            if (isTeleportMessage(lowerMessage)) {
                String playerName = extractPlayerName(message);

                if (playerName != null && !playerName.equalsIgnoreCase(mc.player.getName().getString())) {
                    FriendManager friendManager = FriendManager.getInstance();
                    boolean isFriend = friendManager != null && friendManager.isFriend(playerName);

                    if (!friendSetting.get() || isFriend) {
                        pendingPlayer = playerName;
                        canAccept = true;
                        System.out.println("[AutoAccept] Запрос от " + playerName +
                                " (Друг: " + isFriend + ")");
                    } else {
                        System.out.println("[AutoAccept] Игнорируем запрос от " + playerName +
                                " (не друг)");
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null || !canAccept) return;

        long currentTime = System.currentTimeMillis();

        if (!isInPvp() && (currentTime - lastAcceptTime) >= acceptCooldown) {
            try {
                mc.player.networkHandler.sendChatCommand("tpaccept");
                System.out.println("[AutoAccept] Принят запрос от " + pendingPlayer);

            } catch (Exception ex) {
                try {
                    // Альтернативный метод
                    mc.player.networkHandler.sendChatMessage("/tpaccept");
                } catch (Exception ex2) {
                    System.err.println("[AutoAccept] Ошибка отправки команды: " + ex2.getMessage());
                }
            }

            lastAcceptTime = currentTime;
            canAccept = false;
            pendingPlayer = null;
        }
    }

    private boolean isInPvp() {
        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            boolean hasPvp = mc.inGameHud.getBossBarHud().bossBars.values().stream()
                    .map(bar -> bar.getName().getString().toLowerCase())
                    .anyMatch(text -> text.contains("pvp") || text.contains("пвп") || text.contains("дуэль"));

            if (hasPvp) {
                pvpWatch.reset();
                return true;
            }
        }

        return !pvpWatch.finished(500);
    }

    private boolean isTeleportMessage(String message) {
        for (String pattern : teleportMessages) {
            if (message.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String extractPlayerName(String message) {
        Matcher matcher = playerNamePattern.matcher(message);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.isEmpty()) {
                    return group;
                }
            }
        }

        String[] words = message.split("[ :\\[\\]]");
        for (String word : words) {
            if (word.matches("[a-zA-Zа-яА-Я0-9_]{3,16}")) {
                String lowerWord = word.toLowerCase();
                if (!isIgnoredWord(lowerWord)) {
                    return word;
                }
            }
        }

        return null;
    }

    private boolean isIgnoredWord(String word) {
        String[] ignoredWords = {
                "запрос", "телепорта", "просит", "хочет", "игрок", "от",
                "has", "request", "teleport", "to", "you", "tpa",
                "вам", "к", "в", "на", "из", "и", "не", "no"
        };

        for (String ignored : ignoredWords) {
            if (word.contains(ignored)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        canAccept = false;
        pendingPlayer = null;
        lastAcceptTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        canAccept = false;
        pendingPlayer = null;
    }
}