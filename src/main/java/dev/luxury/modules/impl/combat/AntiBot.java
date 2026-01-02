package dev.luxury.modules.impl.combat;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.player.ServerUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.*;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Игнорирует античит-ботов",
        category = Category.Combat
)
public class AntiBot extends Module {

    public final List<PlayerEntity> bots = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();
    private final List<UUID> cubeCraftBots = new ArrayList<>();
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();
    private final ModeSetting mode = new ModeSetting("Режим", "RW",
            new String[]{"RW", "CubeCraft"});
    private final BooleanSetting removeBots = new BooleanSetting("Удалять ботов", true);

    public static AntiBot instance;

    public AntiBot(){
        instance = this;
        addSettings(mode, removeBots);
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.world == null || mc.player == null) return;

        if (mode.is("RW")) {
            handleRVMode();
        } else if (mode.is("CubeCraft")) {
            handleCubeCraftMode();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (!mode.is("CubeCraft") || !e.isReceive()) return;

        if (e.getPacket() instanceof PlayerListS2CPacket packet) {
            handlePlayerListPacket(packet);
        }

        if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
            handleEntityStatus(packet);
        }
    }

    private void handleRVMode() {
        if (timer.passed(10000) && !bots.isEmpty()) {
            bots.clear();
            timer.reset();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == mc.player) continue;

            // Байпас под RV
            if (armorCheck(player) && !bots.contains(player)) {
                bots.add(player);
            }
        }
    }

    private void handleCubeCraftMode() {
        long currentTime = System.currentTimeMillis();

        playerJoinTimes.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 10000);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;

            UUID playerId = player.getUuid();

            if (isCubeCraftBot(player)) {
                if (!cubeCraftBots.contains(playerId)) {
                    cubeCraftBots.add(playerId);

                    if (removeBots.get()) {
                        removeCubeCraftBot(player);
                    }
                }
            } else {
                cubeCraftBots.remove(playerId);
            }
        }

        if (removeBots.get() && !cubeCraftBots.isEmpty()) {
            removeExistingBots();
        }
    }

    private void handlePlayerListPacket(PlayerListS2CPacket packet) {
        long currentTime = System.currentTimeMillis();

        // В Fabric 1.21.4 нужно использовать getEntries() и проверять тип действия
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            UUID playerId = entry.profileId();

            // Проверяем тип действия через reflection или другие методы
            if (isAddAction(packet, entry)) {
                playerJoinTimes.put(playerId, currentTime);
            } else if (isRemoveAction(packet, entry)) {
                playerJoinTimes.remove(playerId);
                cubeCraftBots.remove(playerId);
            }
        }
    }

    private boolean isAddAction(PlayerListS2CPacket packet, PlayerListS2CPacket.Entry entry) {
        try {
            if (entry.gameMode() != null) {
                return true;
            }

            if (entry.displayName() != null) {
                return true;
            }

            if (entry.latency() >= 0) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRemoveAction(PlayerListS2CPacket packet, PlayerListS2CPacket.Entry entry) {
        try {
            return entry.gameMode() == null &&
                    entry.displayName() == null &&
                    entry.latency() == -1;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleEntityStatus(EntityStatusS2CPacket packet) {
        if (packet.getStatus() == 0 || packet.getStatus() == 2) {
        }
    }

    private boolean isCubeCraftBot(PlayerEntity player) {
        UUID playerId = player.getUuid();
        long currentTime = System.currentTimeMillis();

        Long joinTime = playerJoinTimes.get(playerId);
        if (joinTime == null) {
            playerJoinTimes.put(playerId, currentTime);
            joinTime = currentTime;
        }

        try {
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(playerId);
                if (entry != null && entry.getLatency() <= 0) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        if (player.getVelocity().length() == 0 &&
                currentTime - joinTime > 3000 &&
                player.age > 100) {
            return true;
        }

        // 4. Проверка имени (CubeCraft иногда добавляет ботов с определенными именами)
        String name = player.getName().getString();
        if (name.matches("^NPC-\\d+$") ||
                name.matches("^Bot-\\d+$") ||
                name.contains("[NPC]") ||
                name.contains("CID-") ||
                name.contains("CIT-") ||
                name.contains("[BOT]")) {
            return true;
        }

        return false;
    }

    private void removeCubeCraftBot(PlayerEntity bot) {
        if (mc.world == null || mc.player == null) return;

        // Удаляем из мира
        if (mc.world.getEntityById(bot.getId()) != null) {
            mc.world.removeEntity(bot.getId(),
                    net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        }

        // Удаляем из списка игроков
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getPlayerList().remove(
                    mc.getNetworkHandler().getPlayerListEntry(bot.getUuid()));
        }
    }

    private void removeExistingBots() {
        if (mc.world == null) return;

        List<UUID> toRemove = new ArrayList<>();

        for (UUID botId : cubeCraftBots) {
            PlayerEntity bot = mc.world.getPlayerByUuid(botId);
            if (bot != null) {
                removeCubeCraftBot(bot);
                toRemove.add(botId);
            }
        }

        cubeCraftBots.removeAll(toRemove);
    }

    private boolean armorCheck(PlayerEntity entity) {
        return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET &&
                isNotColored(entity, 3) &&
                !getArmor(entity, 3).hasEnchantments() ||
                getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE &&
                        isNotColored(entity, 2) &&
                        !getArmor(entity, 2).hasEnchantments() ||
                getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS &&
                        isNotColored(entity, 1) &&
                        !getArmor(entity, 1).hasEnchantments() ||
                getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS &&
                        isNotColored(entity, 0) &&
                        !getArmor(entity, 0).hasEnchantments() ||
                getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE &&
                        !getArmor(entity, 2).hasEnchantments() ||
                getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS &&
                        !getArmor(entity, 1).hasEnchantments());
    }

    private ItemStack getArmor(PlayerEntity entity, int slot) {
        return entity.getInventory().getArmorStack(slot);
    }

    private boolean isNotColored(PlayerEntity entity, int slot) {
        return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
    }

    public boolean isBot(PlayerEntity player) {
        if (mode.is("RW")) {
            return bots.contains(player);
        } else if (mode.is("CubeCraft")) {
            return cubeCraftBots.contains(player.getUuid());
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!bots.isEmpty()) bots.clear();
        if (!cubeCraftBots.isEmpty()) cubeCraftBots.clear();
        playerJoinTimes.clear();

        if (ServerUtil.isConnected("aresmine")) {
            ChatUtil.sendError("На АресМайне нет ботов, нужно выключить АнтиБот");
            disable();
        }

        if (mode.is("CubeCraft") && ServerUtil.isConnected("cubecraft")) {
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!bots.isEmpty()) bots.clear();
        if (!cubeCraftBots.isEmpty()) cubeCraftBots.clear();
        playerJoinTimes.clear();
    }
}