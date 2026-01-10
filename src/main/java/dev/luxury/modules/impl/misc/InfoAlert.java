package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.notifications.NotificationsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.*;

@ModuleAnnotation(
        name = "InfoAlert",
        desc = "Инфо нахуй",
        category = Category.Misc
)
public class InfoAlert extends Module {

    private final BooleanSetting uses = new BooleanSetting("Использование", true);
    private final BooleanSetting effects = new BooleanSetting("Эффекты других", true);
    private final BooleanSetting potionEffects = new BooleanSetting("Эффекты зелий", true);
    private final BooleanSetting cooldown = new BooleanSetting("Кулдаун", true);
    private final SliderSetting cooldownTime = new SliderSetting("Время кулдауна", 2, 1, 10, 0.5);

    private MinecraftClient mc = MinecraftClient.getInstance();
    private boolean wasUsingItem = false;
    private String lastUsedItem = "";
    private int lastUseTime = 0;
    private long lastNotificationTime = 0;

    private final Map<UUID, Map<RegistryEntry<StatusEffect>, EffectData>> playerEffects = new HashMap<>();

    private final Map<SoundEvent, Long> lastSoundTimes = new HashMap<>();

    static class EffectData {
        int amplifier;
        long timestamp;

        EffectData(int amplifier, long timestamp) {
            this.amplifier = amplifier;
            this.timestamp = timestamp;
        }
    }

    public InfoAlert() {
        addSettings(uses, effects, potionEffects, cooldown, cooldownTime);
    }

    @EventTarget
    private void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (uses.get()) {
            trackItemUsage();
        }

        if (effects.get()) {
            trackOtherPlayersEffects();
        }

        trackUsageSounds();
    }

    private void trackItemUsage() {
        boolean isUsingItemNow = mc.player.isUsingItem();

        if (isUsingItemNow && !wasUsingItem) {
            ItemStack activeItem = mc.player.getActiveItem();
            if (!activeItem.isEmpty()) {
                lastUsedItem = activeItem.getItem().getName().getString();
                lastUseTime = 0;

                if (isInstantUseItem(activeItem.getItem())) {
                    handleInstantItemUse(activeItem, true);
                }
            }
        }

        if (isUsingItemNow) {
            lastUseTime = mc.player.getItemUseTime();
        }

        if (!isUsingItemNow && wasUsingItem && lastUsedItem != null && !lastUsedItem.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (cooldown.get() && currentTime - lastNotificationTime < (cooldownTime.getValue() * 1000)) {
                lastUsedItem = "";
                wasUsingItem = false;
                return;
            }

            ItemStack currentActive = mc.player.getActiveItem();
            if (currentActive.isEmpty() || !isSameItem(currentActive, lastUsedItem)) {
                handleItemUsed(lastUsedItem, lastUseTime);
                lastNotificationTime = currentTime;
            }

            lastUsedItem = "";
        }

        wasUsingItem = isUsingItemNow;
    }

    private boolean isInstantUseItem(Item item) {
        return item instanceof SplashPotionItem ||
                item instanceof LingeringPotionItem ||
                item instanceof ExperienceBottleItem ||
                item instanceof EnderPearlItem ||
                item instanceof SnowballItem ||
                item instanceof EggItem;
    }

    private boolean isSameItem(ItemStack stack, String itemName) {
        return !stack.isEmpty() && stack.getItem().getName().getString().equals(itemName);
    }

    private void handleInstantItemUse(ItemStack itemStack, boolean isSelf) {
        Item item = itemStack.getItem();
        String itemName = item.getName().getString();

        long currentTime = System.currentTimeMillis();
        if (cooldown.get() && currentTime - lastNotificationTime < (cooldownTime.getValue() * 1000)) {
            return;
        }

        String message = isSelf ? "Использован предмет: " + itemName :
                "§eИгрок §fиспользует: §a" + itemName;

        if ((item instanceof SplashPotionItem ||
                item instanceof LingeringPotionItem) && potionEffects.get()) {
            message += getPotionEffectsInfo(itemStack);
        }

        sendNotification(message);
        lastNotificationTime = currentTime;
    }

    private void handleItemUsed(String itemName, int useTime) {
        String message = "Использован предмет: " + itemName;

        ItemStack itemStack = findLastUsedItemStack(itemName);
        if (itemStack != null && itemStack.getItem() instanceof PotionItem && potionEffects.get()) {
            message += getPotionEffectsInfo(itemStack);
        }

        sendNotification(message);
    }

    private void trackOtherPlayersEffects() {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player != mc.player) {
                UUID playerId = player.getUuid();
                String playerName = player.getName().getString();

                Map<RegistryEntry<StatusEffect>, EffectData> currentEffects = new HashMap<>();
                long currentTime = System.currentTimeMillis();

                for (StatusEffectInstance effect : player.getStatusEffects()) {
                    currentEffects.put(effect.getEffectType(),
                            new EffectData(effect.getAmplifier(), currentTime));
                }

                Map<RegistryEntry<StatusEffect>, EffectData> previousEffects = playerEffects.get(playerId);

                if (previousEffects != null) {
                    for (Map.Entry<RegistryEntry<StatusEffect>, EffectData> entry : currentEffects.entrySet()) {
                        RegistryEntry<StatusEffect> effectType = entry.getKey();
                        EffectData currentData = entry.getValue();

                        if (!previousEffects.containsKey(effectType)) {
                            String effectName = effectType.value().getName().getString();
                            int amplifier = currentData.amplifier + 1;

                            long timeSinceLastNotif = currentTime - lastNotificationTime;
                            if (!cooldown.get() || timeSinceLastNotif >= (cooldownTime.getValue() * 1000)) {
                                String message = "§e" + playerName +
                                        " §fполучил эффект: §a" + effectName +
                                        " §7(ур. " + amplifier + ")";
                                sendNotification(message);
                                lastNotificationTime = currentTime;
                            }
                        }
                    }
                }

                playerEffects.put(playerId, currentEffects);
            }
        }

        cleanOldPlayerData();
    }

    private void trackUsageSounds() {
    }

    private void cleanOldPlayerData() {
        long currentTime = System.currentTimeMillis();
        long timeout = 120000;

        Iterator<UUID> iterator = playerEffects.keySet().iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            boolean playerFound = false;
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof PlayerEntity player && player.getUuid().equals(playerId)) {
                    playerFound = true;
                    break;
                }
            }

            if (!playerFound) {
                iterator.remove();
            }
        }
    }

    private ItemStack findLastUsedItemStack(String itemName) {
        ItemStack activeItem = mc.player.getActiveItem();
        if (!activeItem.isEmpty() && activeItem.getItem().getName().getString().equals(itemName)) {
            return activeItem;
        }

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().getName().getString().equals(itemName)) {
                return stack;
            }
        }
        return null;
    }

    private String getPotionEffectsInfo(ItemStack potionStack) {
        PotionContentsComponent potionContents = potionStack.get(DataComponentTypes.POTION_CONTENTS);

        if (potionContents != null) {
            Iterable<StatusEffectInstance> effectsIterable = potionContents.getEffects();
            List<StatusEffectInstance> effects = new ArrayList<>();

            for (StatusEffectInstance effect : effectsIterable) {
                effects.add(effect);
            }

            if (!effects.isEmpty()) {
                StringBuilder effectsInfo = new StringBuilder();
                effectsInfo.append(" [");

                for (int i = 0; i < effects.size(); i++) {
                    StatusEffectInstance effect = effects.get(i);
                    StatusEffect statusEffect = effect.getEffectType().value();
                    int amplifier = effect.getAmplifier() + 1;

                    // Получаем ID эффекта
                    String effectId = getEffectId(statusEffect);
                    String shortName = getShortEffectNameById(effectId);

                    effectsInfo.append(shortName).append(" ").append(amplifier);

                    if (i < effects.size() - 1) {
                        effectsInfo.append(", ");
                    }
                }

                effectsInfo.append("]");
                return effectsInfo.toString();
            }
        }

        return "";
    }

    private String getShortEffectNameById(String effectId) {
        switch (effectId.toLowerCase()) {
            case "speed":
            case "move_speed":
                return "Speed";
            case "slowness":
            case "move_slowdown":
                return "Slow";
            case "strength":
            case "damage_boost":
                return "Str";
            case "weakness":
                return "Weak";
            case "jump_boost":
                return "Jump";
            case "regeneration":
                return "Regen";
            case "poison":
                return "Poison";

            case "instant_health":
            case "heal":
                return "Heal";
            case "instant_damage":
            case "harm":
                return "Harm";

            case "fire_resistance":
                return "FireRes";
            case "water_breathing":
                return "WaterBr";
            case "resistance":
                return "Resist";
            case "slow_falling":
                return "SlowFall";

            case "night_vision":
                return "NVision";
            case "invisibility":
                return "Invis";
            case "haste":
            case "dig_speed":
                return "Haste";
            case "saturation":
                return "Saturation";
            case "luck":
                return "Luck";
            case "conduit_power":
                return "Conduit";
            case "dolphins_grace":
                return "Dolphin";

            case "mining_fatigue":
            case "dig_slowdown":
                return "Fatigue";
            case "levitation":
                return "Levitate";
            case "blindness":
                return "Blind";
            case "nausea":
                return "Nausea";
            case "hunger":
                return "Hunger";
            case "glowing":
                return "Glowing";
            case "unluck":
            case "bad_omen":
                return "Unluck";
            case "village_hero":
                return "Hero";

            case "long_night_vision":
                return "LongNV";
            case "long_invisibility":
                return "LongInvis";
            case "long_fire_resistance":
                return "LongFireRes";
            case "long_water_breathing":
                return "LongWaterBr";
            case "long_strength":
                return "LongStr";
            case "long_slowness":
                return "LongSlow";
            case "long_leaping":
                return "LongJump";
            case "long_regeneration":
                return "LongRegen";
            case "long_poison":
                return "LongPoison";

            case "strong_strength":
                return "StrongStr";
            case "strong_slowness":
                return "StrongSlow";
            case "strong_leaping":
                return "StrongJump";
            case "strong_regeneration":
                return "StrongRegen";
            case "strong_poison":
                return "StrongPoison";
            case "strong_healing":
                return "StrongHeal";
            case "strong_harming":
                return "StrongHarm";

            default:
                return effectId.length() > 8 ? effectId.substring(0, 8) + "..." : effectId;
        }
    }

    private String getEffectId(StatusEffect effect) {
        try {
            var effectId = net.minecraft.registry.Registries.STATUS_EFFECT.getId(effect);
            if (effectId != null) {
                String idString = effectId.toString();
                if (idString.startsWith("minecraft:")) {
                    return idString.substring("minecraft:".length());
                }
                return idString;
            }
        } catch (Exception e) {
        }

        String translationKey = effect.getTranslationKey();
        if (translationKey.startsWith("effect.minecraft.")) {
            return translationKey.substring("effect.minecraft.".length());
        }

        return translationKey;
    }

    private void sendNotification(String message) {
        ChatUtil.sendChat(message);
        NotificationsManager.getInstance().info(message, 3000);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        wasUsingItem = false;
        lastUsedItem = "";
        lastUseTime = 0;
        lastNotificationTime = 0;
        playerEffects.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        playerEffects.clear();
    }
}