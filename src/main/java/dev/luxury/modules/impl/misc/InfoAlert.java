package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.impl.other.hud.impl.EffectsList;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.notifications.NotificationsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "InfoAlert",
        desc = "Инфо нахуй",
        category = Category.Misc
)
public class InfoAlert extends Module {

    private final BooleanSetting uses = new BooleanSetting("Использование", true);
    private final BooleanSetting effects = new BooleanSetting("Эффекты", false);
    private final BooleanSetting notifyEnd = new BooleanSetting("Уведомлять о завершении", true);
    private final BooleanSetting onlyOnce = new BooleanSetting("Одно сообщение", true);

    private MinecraftClient mc = MinecraftClient.getInstance();
    private boolean wasUsingItem = false;
    private String lastUsedItem = "";
    private int lastUseTime = 0;

    public InfoAlert() {
        addSettings(uses, effects, notifyEnd, onlyOnce);
    }

    @EventTarget
    private void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (uses.get()) {
            boolean isUsingItemNow = mc.player.isUsingItem();

            if (isUsingItemNow && !wasUsingItem) {
                ItemStack activeItem = mc.player.getActiveItem();
                if (!activeItem.isEmpty()) {
                    lastUsedItem = activeItem.getItem().getName().getString();
                    lastUseTime = 0;

                    if (!notifyEnd.get()) {
                        String message = "Начато использование: " + lastUsedItem;
                        sendNotification(message);
                    }
                }
            }

            if (isUsingItemNow) {
                lastUseTime = mc.player.getItemUseTime();
            }

            if (!isUsingItemNow && wasUsingItem && lastUsedItem != null && !lastUsedItem.isEmpty()) {
                String message = "Использован предмет: " + lastUsedItem;

                if (lastUseTime > 0) {
                    message += " (время: " + lastUseTime + " тиков)";
                }

                ItemStack lastItemStack = findLastUsedItemStack(lastUsedItem);
                if (lastItemStack != null && lastItemStack.getItem() instanceof PotionItem) {
                    message += getPotionEffectsInfo(lastItemStack);
                }

                sendNotification(message);

                if (onlyOnce.get()) {
                    lastUsedItem = "";
                }
            }

            wasUsingItem = isUsingItemNow;
        }
    }

    private ItemStack findLastUsedItemStack(String itemName) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().getName().getString().equals(itemName)) {
                return stack;
            }
        }
        return null;
    }

    private String getPotionEffectsInfo(ItemStack potionStack) {
        StringBuilder effectsInfo = new StringBuilder();

        PotionContentsComponent potionContents = potionStack.get(DataComponentTypes.POTION_CONTENTS);

        if (potionContents != null) {
            Iterable<StatusEffectInstance> effectsIterable = potionContents.getEffects();
            List<StatusEffectInstance> effects = new ArrayList<>();

            for (StatusEffectInstance effect : effectsIterable) {
                effects.add(effect);
            }

            if (!effects.isEmpty()) {
                effectsInfo.append(" [Эффекты: ");
                for (int i = 0; i < effects.size(); i++) {
                    StatusEffectInstance effect = effects.get(i);
                    String effectName = effect.getEffectType().value().getName().getString();
                    int amplifier = effect.getAmplifier() + 1;
                    int duration = effect.getDuration() / 20;

                    String shortName = getShortEffectName(effectName);

                    effectsInfo.append(shortName).append(" ").append(amplifier)
                            .append(" (").append(duration).append("с)");

                    if (i < effects.size() - 1) {
                        effectsInfo.append(", ");
                    }
                }
                effectsInfo.append("]");
            }
        }

        return effectsInfo.toString();
    }

    private String getShortEffectName(String fullName) {
        if (fullName.contains("Скорость")) return "Скор";
        if (fullName.contains("Замедление")) return "Замед";
        if (fullName.contains("Сила")) return "Сила";
        if (fullName.contains("Слабость")) return "Слабка";
        if (fullName.contains("Прыжок")) return "Прыжок";
        if (fullName.contains("Регенерация")) return "Реген";
        if (fullName.contains("Отравление")) return "Яд";
        if (fullName.contains("Исцеление")) return "Лечение";
        if (fullName.contains("Урон")) return "Урон";
        if (fullName.contains("Ночное зрение")) return "Ноч.зр";
        if (fullName.contains("Невидимость")) return "Невидим";
        if (fullName.contains("Огнестойкость")) return "Огнест";
        if (fullName.contains("Водное дыхание")) return "Вод.дых";
        if (fullName.contains("Сопротивление")) return "Защ";

        return fullName.length() > 10 ? fullName.substring(0, 10) + "..." : fullName;
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
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}