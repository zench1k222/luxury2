package dev.luxury.modules;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.mixin.render.impl.LivingEntityAccessor;
import dev.luxury.mixin.render.impl.MinecraftClientAccessor;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

@ModuleAnnotation(name = "NoDelay", category = Category.Player, desc = "Убирает задержку в изпользование чего либо")
public final class NoDelay extends Module {

        private final BooleanSetting jump = new BooleanSetting("Прыжок",true);
        private final BooleanSetting place = new BooleanSetting("Кнопка изпользования",false);
        private final BooleanSetting xp = new BooleanSetting("Пузырёк опыта",true);
        private final BooleanSetting crystal = new BooleanSetting("Кристал",true);


        public NoDelay() {
            addSettings(jump,xp,crystal,place);
        }

        @EventTarget
        public void onEvent(EventTick event) {
                if (jump.get()) {
                    ((LivingEntityAccessor) mc.player).setLastJumpCooldown(0);
                }
                if (check(mc.player.getMainHandStack().getItem()))
                    ((MinecraftClientAccessor) mc).setUseCooldown(0);
            }
        private boolean check(Item item) {
            return (item instanceof BlockItem && place.get()) || (item == Items.END_CRYSTAL && crystal.get()) || (item == Items.EXPERIENCE_BOTTLE && xp.get());
        }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}