package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.notifications.NotificationsManager;

@ModuleAnnotation(
        name = "HPAlert",
        desc = "Предупреждение о низком уровне здоровья",
        category = Category.Misc
)
public class HPAlert extends Module {

    private final SliderSetting healthThreshold = new SliderSetting("Порог здоровья", 10, 1, 100, 1);
    private final SliderSetting cooldown = new SliderSetting("Задержка уведомлений (сек)", 5, 1, 30, 1);

    private long lastAlertTime = 0;

    public HPAlert() {
        addSettings(healthThreshold, cooldown);
    }

    @EventTarget
    public void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        float currentHealth = mc.player.getHealth();

        if (currentHealth <= healthThreshold.getFloatValue()) {
            long currentTime = System.currentTimeMillis();
            long lastAlert = lastAlertTime;
            long cooldownMs = (long) (cooldown.getFloatValue() * 1000);

            if (currentTime - lastAlert >= cooldownMs) {
                NotificationsManager.getInstance().warning(
                        String.format("Низкое здоровье: %.1f HP", currentHealth),
                        3000
                );
                lastAlertTime = currentTime;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastAlertTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastAlertTime = 0;
    }
}