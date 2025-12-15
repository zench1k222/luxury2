package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Игнорирует античит-ботов",
        category = Category.Combat
)
public final class AntiBot extends Module {

    public static final AntiBot INSTANCE = new AntiBot();

    private final Set<UUID> bots = new HashSet<>();
    private final Map<UUID, Vec3d> lastPos = new HashMap<>();
    private final Map<UUID, Long> lastMove = new HashMap<>();

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            UUID uuid = player.getUuid();

            if (checkBot(player)) {
                bots.add(uuid);
            }

            checkSpeed(player);
        }

        // очистка мёртвых
        bots.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        lastPos.keySet().removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        lastMove.keySet().removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
    }

    private boolean checkBot(PlayerEntity p) {
        // мёртвый
        if (!p.isAlive() || p.getHealth() <= 0) return true;

        // слишком рано появился
        if (p.age < 20) return true;

        // странный UUID
        if (!p.getUuid().equals(getOfflineUUID(p.getName().getString()))) {
            return true;
        }

        // фулл броня сразу
        int armor = 0;
        for (ItemStack stack : p.getArmorItems()) {
            if (!stack.isEmpty()) armor++;
        }
        return armor == 4;
    }

    //ууид так делаем, пидоры метод удалили
    private UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }


    private void checkSpeed(PlayerEntity p) {
        UUID uuid = p.getUuid();
        Vec3d current = p.getPos();
        long now = System.currentTimeMillis();

        if (lastPos.containsKey(uuid)) {
            Vec3d prev = lastPos.get(uuid);
            long time = now - lastMove.getOrDefault(uuid, now);

            if (time > 0) {
                double speed = current.distanceTo(prev) / (time / 1000.0);
                if (speed > 20.0) {
                    bots.add(uuid);
                }
            }
        }

        lastPos.put(uuid, current);
        lastMove.put(uuid, now);
    }

    public boolean isBot(PlayerEntity player) {
        return bots.contains(player.getUuid());
    }

    @Override
    public void onEnable() {
        bots.clear();
        lastPos.clear();
        lastMove.clear();
    }

    @Override
    public void onDisable() {
        bots.clear();
        lastPos.clear();
        lastMove.clear();
    }
}
