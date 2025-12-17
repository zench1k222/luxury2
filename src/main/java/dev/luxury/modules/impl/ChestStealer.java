package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.math.StopWatch;
import dev.luxury.utils.player.InventoryUtil;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleAnnotation(
        name = "ChestStealer",
        desc = "Автоматически забирает предметы из сундуков",
        category = dev.luxury.modules.api.Category.Misc
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChestStealer extends Module {
    StopWatch stopWatch = new StopWatch();

    ModeSetting modeSetting = new ModeSetting("Тип", "Default", new String[]{"Default", "WhiteList", "FunTime"});
    SliderSetting delaySetting = new SliderSetting("Задержка", 100, 0, 1000, 50);
    private final ModeListSetting itemSettings = new ModeListSetting("Предметы",
            new BooleanSetting("Player Head", true),
            new BooleanSetting("Totem Of Undying", true),
            new BooleanSetting("Elytra", true),
            new BooleanSetting("Netherite Sword", true),
            new BooleanSetting("Netherite Helmet", true),
            new BooleanSetting("Netherite ChestPlate", true),
            new BooleanSetting("Netherite Leggings", true),
            new BooleanSetting("Netherite Boots", true),
            new BooleanSetting("Netherite Ingot", true),
            new BooleanSetting("Netherite Scrap", true));

    public ChestStealer() {
        addSettings(modeSetting, delaySetting, itemSettings);
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (modeSetting.is("FunTime")) {
            if (mc.currentScreen instanceof GenericContainerScreen sh &&
                    sh.getTitle().getString().toLowerCase().contains("мистический") &&
                    !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {

                sh.getScreenHandler().slots.stream()
                        .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && stopWatch.every(150))
                        .forEach(s -> {
                            int slotId = s.id;
                            InventoryUtil.clickSlotLegit(slotId, 0, SlotActionType.QUICK_MOVE, true);
                        });
            }
        }

        if (modeSetting.is("WhiteList") || modeSetting.is("Default")) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) {
                sh.slots.forEach(s -> {
                    if (s.hasStack() &&
                            !s.inventory.equals(mc.player.getInventory()) &&
                            (modeSetting.is("Default") || isInWhiteList(s.getStack().getItem())) &&
                            stopWatch.every(delaySetting.getIntValue())) {

                        int slotId = s.id;
                        InventoryUtil.clickSlotLegit(slotId, 0, SlotActionType.QUICK_MOVE, true);
                    }
                });
            }
        }
    }


    private boolean isInWhiteList(Item item) {
        String itemName = item.toString().toLowerCase().replace("_", " ");

        for (BooleanSetting setting : itemSettings.getSettings()) {
            String settingName = setting.getName().toLowerCase();

            if (itemName.contains(settingName) || settingName.contains(itemName)) {
                return setting.get();
            }
        }

        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        stopWatch.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}