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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(
        name = "AutoAccept",
        desc = "Залупенск",
        category = Category.Misc
)
public class AutoAccept extends Module {

    private final TimerUtils pvpWatch = new TimerUtils();

    private final String[] teleportMessages = new String[]{
            "has requested teleport",
            "просит телепортироваться",
            "запрос на телепортацию от",
            "хочет телепортироваться к вам",
            "просит к вам телепортироваться"
    };
    private boolean canAccept;

    private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", true);

    public AutoAccept() {
        addSettings(friendSetting);
    }

    @EventTarget
    public void onPacket(PacketEvent e, PlayerEntity player) {
        if (e.getPacket() instanceof GameMessageS2CPacket m) {
            String message = m.content().getString();
            boolean validPlayer = !friendSetting.isValue() || FriendManager.getInstance().isFriend(player.getName().getString());
            if (isTeleportMessage(message)) {
                canAccept = validPlayer;
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (!isPvp() && canAccept) {
            mc.player.networkHandler.sendChatCommand("tpaccept");
            canAccept = false;
        }
    }

    public boolean isPvp() {
        return !pvpWatch.finished(500);
    }

    private boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
    }

    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}