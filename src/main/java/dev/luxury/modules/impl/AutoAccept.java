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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
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
    private final long acceptCooldown = 1000; // 1 секунда кулдаун

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
                    boolean isFriend = FriendManager.getInstance().isFriend(playerName);

                    if (!friendSetting.isValue() || isFriend) {
                        pendingPlayer = playerName;
                        canAccept = true;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();

        if (!isInPvp() && canAccept && (currentTime - lastAcceptTime) >= acceptCooldown) {
            try {
                mc.player.networkHandler.sendChatCommand("tpaccept");

            } catch (Exception ex) {
                try {
                    mc.player.networkHandler.sendChatMessage("/tpaccept");
                } catch (Exception ex2) {
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
                if (!lowerWord.contains("запрос") && !lowerWord.contains("телепорта") &&
                        !lowerWord.contains("просит") && !lowerWord.contains("хочет") &&
                        !lowerWord.contains("игрок") && !lowerWord.contains("от") &&
                        !lowerWord.contains("has") && !lowerWord.contains("request") &&
                        !lowerWord.contains("teleport") && !lowerWord.contains("to") &&
                        !lowerWord.contains("you") && !lowerWord.contains("tpa") &&
                        !lowerWord.contains("вам") && !lowerWord.contains("к")) {
                    return word;
                }
            }
        }

        return null;
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