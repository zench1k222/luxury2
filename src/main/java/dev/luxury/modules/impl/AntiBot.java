package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Игнорирует ботов от античита",
        category = Category.Combat,
        key = GLFW.GLFW_KEY_Y
)
public class AntiBot extends Module {

    public enum BotDetectionMode {
        MATRIX,
        ADVANCED,
        HYBRID
    }

    private BotDetectionMode detectionMode = BotDetectionMode.HYBRID;

    private final Map<UUID, Long> spawnTime = new ConcurrentHashMap<>();
    private final Set<UUID> validPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> seenNames = ConcurrentHashMap.newKeySet();

    private final Set<Entity> matrixBots = ConcurrentHashMap.newKeySet();

    @EventTarget
    public void onTick(EventTick event) {
        if (detectionMode == BotDetectionMode.MATRIX || detectionMode == BotDetectionMode.HYBRID) {
            updateMatrixBots();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearAll();
    }

    public boolean isBot(Entity entity) {
        if (!isEnabled()) {
            return false;
        }

        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        if (player == mc.player) {
            return false;
        }

        switch (detectionMode) {
            case MATRIX:
                return isMatrixBot(entity);
            case ADVANCED:
                return isAdvancedBot(player);
            case HYBRID:
                return isMatrixBot(entity) || isAdvancedBot(player);
            default:
                return false;
        }
    }

    private boolean isMatrixBot(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        if (matrixBots.contains(entity)) {
            return true;
        }

        try {
            Iterable<ItemStack> armorItems = player.getArmorItems();
            List<ItemStack> armorList = new ArrayList<>();
            armorItems.forEach(armorList::add);

            if (armorList.size() != 4) {
                return false;
            }

            ItemStack boots = armorList.get(0);
            ItemStack leggings = armorList.get(1);
            ItemStack chestplate = armorList.get(2);
            ItemStack helmet = armorList.get(3);

            if (boots.isEmpty() || leggings.isEmpty() ||
                    chestplate.isEmpty() || helmet.isEmpty()) {
                return false;
            }

            if (!boots.isDamageable() || !leggings.isDamageable() ||
                    !chestplate.isDamageable() || !helmet.isDamageable()) {
                return false;
            }
            ItemStack offhand = player.getOffHandStack();
            if (!offhand.isEmpty()) {
                return false;
            }

            boolean validArmor = (
                    (boots.getItem() == Items.LEATHER_BOOTS ||
                            leggings.getItem() == Items.LEATHER_LEGGINGS ||
                            chestplate.getItem() == Items.LEATHER_CHESTPLATE ||
                            helmet.getItem() == Items.LEATHER_HELMET) ||
                            (boots.getItem() == Items.IRON_BOOTS ||
                                    leggings.getItem() == Items.IRON_LEGGINGS ||
                                    chestplate.getItem() == Items.IRON_CHESTPLATE ||
                                    helmet.getItem() == Items.IRON_HELMET)
            );

            if (!validArmor) {
                return false;
            }
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.isEmpty()) {
                return false;
            }

            if (boots.isDamaged() || leggings.isDamaged() ||
                    chestplate.isDamaged() || helmet.isDamaged()) {
                return false;
            }

            if (player.getHungerManager().getFoodLevel() != 20) {
                return false;
            }

            matrixBots.add(entity);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdvancedBot(PlayerEntity player) {
        UUID uuid = player.getUuid();

        if (validPlayers.contains(uuid)) {
            return false;
        }

        if (mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry == null) {
                return true;
            }
        } else {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        spawnTime.putIfAbsent(uuid, currentTime);
        long existTime = currentTime - spawnTime.get(uuid);

        if (existTime < 2000) {
            return true;
        }

        String playerName = player.getName().getString();
        if (mc.world != null) {
            int nameCount = 0;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p.getName().getString().equals(playerName)) {
                    nameCount++;
                }
            }

            if (nameCount > 1) {
                if (!seenNames.contains(playerName + uuid.toString())) {
                    return true;
                }
            } else {
                seenNames.add(playerName + uuid.toString());
            }
        }

        if (!isValidMinecraftName(playerName)) {
            return true;
        }

        validPlayers.add(uuid);
        return false;
    }

    /**
     * Обновление списка Matrix ботов (вызывается каждый тик)
     */
    private void updateMatrixBots() {
        if (mc.world == null) {
            matrixBots.clear();
            return;
        }
        matrixBots.removeIf(bot -> {
            if (mc.world == null) return true;
            return !mc.world.getPlayers().contains(bot);
        });
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (isMatrixBot(player) && !matrixBots.contains(player)) {
                matrixBots.add(player);
            } else if (!isMatrixBot(player) && matrixBots.contains(player)) {
                matrixBots.remove(player);
            }
        }
    }

    public static boolean checkBot(LivingEntity entity) {
        try {
            return entity instanceof PlayerEntity && false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidMinecraftName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() < 3 || name.length() > 16) {
            return false;
        }

        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
            if (c > 127) {
                return false;
            }
        }

        return true;
    }

    public void removePlayer(UUID uuid) {
        spawnTime.remove(uuid);
        validPlayers.remove(uuid);

        // Удаляем из Matrix списка по UUID
        matrixBots.removeIf(bot -> {
            if (bot instanceof PlayerEntity player) {
                return player.getUuid().equals(uuid);
            }
            return false;
        });
    }

    public void clearAll() {
        spawnTime.clear();
        validPlayers.clear();
        seenNames.clear();
        matrixBots.clear();
    }

    public BotDetectionMode getDetectionMode() {
        return detectionMode;
    }

    public void setDetectionMode(BotDetectionMode mode) {
        this.detectionMode = mode;
        clearAll();
    }

    public Set<Entity> getMatrixBots() {
        return new HashSet<>(matrixBots);
    }

    public int getBotCount() {
        return matrixBots.size() + validPlayers.size();
    }
}