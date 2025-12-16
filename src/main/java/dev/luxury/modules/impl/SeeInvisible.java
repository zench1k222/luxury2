package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.player.EntityColorEvent;
import dev.luxury.utils.render.ColorUtil;

@ModuleAnnotation(
        name = "SeeInvisible",
        desc = "Позволяет видеть невидимых игроков",
        category = Category.Render
)
public class SeeInvisible extends Module {
    public final SliderSetting alphaSetting = new SliderSetting("Прозрачность", 0.5, 0.1, 1,0.1);

    public static boolean state = false;

    public SeeInvisible() {
        addSettings(alphaSetting);
    }

    @EventTarget
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorUtil.multAlpha(e.getColor(), alphaSetting.getIntValue()));
        e.cancel();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = true;
    }

    @Override
    public void onDisable() {
        state = false;
        super.onDisable();
    }
}
