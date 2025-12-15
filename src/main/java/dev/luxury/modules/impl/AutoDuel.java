package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.player.InventoryUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ModuleAnnotation(
        name = "AutoDuel",
        desc = "Автоматическая отправка дуэлей и выбор набора",
        category = Category.Misc
)
public class AutoDuel extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Шары",
            new String[]{"Шары", "Щит", "Шипы 3", "Незеритка", "Читерский рай", "Лук", "Классик", "Тотемы", "Нодебафф"});

    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final List<String> sent = new ArrayList<>();
    private long lastSend = 0;
    private long lastClear = 0;
    private long lastChoice = 0;
    private long lastConfirm = 0;

    public AutoDuel() {
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        sent.clear();
        lastSend = lastClear = lastChoice = lastConfirm = 0;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        List<String> players = getOnlinePlayers();

        if (now - lastClear >= Math.max(800L * Math.max(1, players.size()), 2000L)) {
            sent.clear();
            lastClear = now;
        }

        for (String p : players) {
            if (!p.equals(mc.player.getName().getString())
                    && !sent.contains(p)
                    && now - lastSend >= 1000) {

                sendDuel(p);
                sent.add(p);
                lastSend = now;
            }
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        String title = screen.getTitle().getString();

        if (title.contains("Выбор набора") && now - lastChoice >= 150) {
            int slotId = resolveModeSlot();
            if (slotId >= 0) {
                InventoryUtil.clickSlot(slotId, 0, SlotActionType.PICKUP);
            }
            lastChoice = now;
            return;
        }

        if (title.contains("Настройка поединка") && now - lastConfirm >= 150) {
            InventoryUtil.clickSlot(0, 0, SlotActionType.PICKUP);
            lastConfirm = now;
        }
    }


    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isReceive() && event.getPacket() instanceof GameMessageS2CPacket msg) {
            String text = msg.content().getString().toLowerCase();
            if ((text.contains("начало") && text.contains("через") && text.contains("секунд")) ||
                    text.contains("во время поединка запрещено использовать команды")) {
                this.disable();
            }
        }
    }

    private void sendDuel(String player) {
        if (mc.player == null) return;

        String cmd = "duel " + player;
        try {
            mc.player.networkHandler.sendPacket(new CommandExecutionC2SPacket(cmd));
        } catch (Throwable ignored) {
            try {
                mc.player.networkHandler.sendChatCommand(cmd);
            } catch (Throwable ignored2) {}
        }
    }

    private int resolveModeSlot() {
        if (mode.is("Щит")) return 0;
        if (mode.is("Шипы 3")) return 1;
        if (mode.is("Лук")) return 2;
        if (mode.is("Тотемы")) return 3;
        if (mode.is("Нодебафф")) return 4;
        if (mode.is("Шары")) return 5;
        if (mode.is("Классик")) return 6;
        if (mode.is("Читерский рай")) return 7;
        if (mode.is("Незеритка")) return 8;
        return -1;
    }

    private List<String> getOnlinePlayers() {
        try {
            return mc.getNetworkHandler().getPlayerList().stream()
                    .map(PlayerListEntry::getProfile)
                    .map(p -> p.getName())
                    .filter(name -> name != null && namePattern.matcher(name).matches())
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            return List.of();
        }
    }
}
