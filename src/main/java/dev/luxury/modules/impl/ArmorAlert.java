package dev.luxury.modules.impl;


import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.notifications.NotificationsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(
        name = "ArmorAlert",
        desc = "Предупреждение о поломке брони",
        category = Category.Player
)
public class ArmorAlert extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SliderSetting durabilityThreshold = new SliderSetting("Порог прочности", 10, 1, 50, 1);
    public final SliderSetting cooldown = new SliderSetting("Задержка звука (сек)", 5, 1, 30, 1);

    private final Map<Integer, Long> lastAlertTime = new HashMap<>();

    public ArmorAlert() {
        addSettings(durabilityThreshold, cooldown);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastAlertTime.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastAlertTime.clear();
    }
@EventTarget
    public void onUpdate(EventTick event) {
        if (mc.player == null) return;

        for (int i = 0; i < 4; i++) {
            ItemStack armor = mc.player.getInventory().getArmorStack(i);
            checkItemDurability(armor, i);
        }

    }

    private void checkItemDurability(ItemStack item, int slot) {
        if (item == null || item.isEmpty() || !item.isDamageable()) return;

        int maxDamage = item.getMaxDamage();
        int damage = item.getDamage();

        if (damage > 0) {
            float durabilityPercent = 1.0f - ((float) damage / (float) maxDamage);
            float thresholdPercent = durabilityThreshold.getFloatValue() / 100f;

            if (durabilityPercent <= thresholdPercent) {
                long currentTime = System.currentTimeMillis();
                long lastAlert = lastAlertTime.getOrDefault(slot, 0L);
                long cooldownMs = (long) (cooldown.getFloatValue() * 1000);

                if (currentTime - lastAlert >= cooldownMs) {
                    ClientSounds.getInstance().playArmorAlertSound();
                    NotificationsManager.getInstance().warning("Скоро сломается броня", 3000);
                    lastAlertTime.put(slot, currentTime);
                }
            } else {
                lastAlertTime.remove(slot);
            }
        }
    }
}