package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.stream.StreamSupport;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Античит анти-бот",
        category = Category.Combat
)
public final class AntiBot extends Module {

    public static final AntiBot INSTANCE = new AntiBot();

    private final Set<UUID> suspectSet = new HashSet<>();
    private final Set<UUID> botSet = new HashSet<>();

    private final ModeSetting mode = new ModeSetting(
            "Mode",
            "Matrix",
            new String[]{"Matrix", "ReallyWorld"}
    );

    public AntiBot() {
        addSettings(mode);
    }

    @EventTarget
    public void onTick(EventTick e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        for (UUID uuid : new HashSet<>(suspectSet)) {
            PlayerEntity p = mc.world.getPlayerByUuid(uuid);
            if (p != null) {
                evaluateSuspect(p);
            }
            suspectSet.remove(uuid);
        }

        if (mode.is("Matrix")) {
            matrixMode();
        } else {
            reallyWorldMode();
        }

        botSet.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
    }


    private void matrixMode() {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;

            String name = p.getName().getString();

            boolean nameBot =
                    name.startsWith("CIT-") &&
                            !name.contains("NPC") &&
                            !name.startsWith("[ZNPC]");

            boolean fakeUUID = !p.getUuid().equals(getOfflineUUID(name));

            int armor = 0;
            for (var stack : p.getArmorItems()) {
                if (!stack.isEmpty()) armor++;
            }

            if (armor == 4 || nameBot || fakeUUID) {
                botSet.add(p.getUuid());
            }
        }
    }

    private void reallyWorldMode() {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;

            if (mc.getNetworkHandler().getPlayerListEntry(p.getUuid()) == null) {
                botSet.add(p.getUuid());
                continue;
            }

            int diamond = 0;
            int netherite = 0;
            int totalArmor = 0;

            for (var stack : p.getArmorItems()) {
                if (stack.isEmpty()) continue;

                totalArmor++;

                String id = stack.getItem().toString();

                if (id.contains("diamond")) diamond++;
                if (id.contains("netherite")) netherite++;
            }

            boolean fullDiamond = diamond == 4;
            boolean fullNetherite = netherite == 4;

            if (totalArmor > 0 && !fullDiamond && !fullNetherite) {
                botSet.add(p.getUuid());
            }
        }
    }


    private void evaluateSuspect(PlayerEntity p) {
        int armor = 0;
        for (var stack : p.getArmorItems()) {
            if (!stack.isEmpty()) armor++;
        }

        if (armor == 4) {
            botSet.add(p.getUuid());
        }
    }

    private UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }


    public boolean isBot(PlayerEntity p) {
        if (p == null) return false;

        String name = p.getName().getString();

        boolean nameBot =
                name.startsWith("CIT-") &&
                        !name.contains("NPC") &&
                        !name.startsWith("[ZNPC]");

        return nameBot || botSet.contains(p.getUuid());
    }

    @Override
    public void onDisable() {
        botSet.clear();
        suspectSet.clear();
        super.onDisable();
    }
}
