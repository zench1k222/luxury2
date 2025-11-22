package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;

@ModuleAnnotation(
        name = "HUD",
        desc = "Интерфейс чита",
        category = Category.Render

)
public class HUD extends Module {
    WaterMark waterMark = new WaterMark();

    TargetHud targetHud = new TargetHud();
    MediaPlayer mediaPlayer = new MediaPlayer();
    KeyBinds keyBinds = new KeyBinds();
    Staffs staffs = new Staffs();
    private final ModeListSetting type = new ModeListSetting("Отображение"
            ,new BooleanSetting("WaterMark",true),
            new BooleanSetting("TargetHud",true),
            new BooleanSetting("Staffs",true)
            ,new BooleanSetting("KeyBinds",true));

public HUD(){
    addSettings(type);
}

@EventTarget
    public void onRender2D(EventRender2D e){
        BooleanSetting waterMarkSetting = type.getValueByName("WaterMark");
        if (waterMarkSetting != null && waterMarkSetting.isValue()) {
            waterMark.render(e);
        }


        BooleanSetting targetHudSetting = type.getValueByName("TargetHud");
        if (targetHudSetting != null && targetHudSetting.isValue()) {
            targetHud.render(e);
        }

        BooleanSetting keyBindsSetting = type.getValueByName("KeyBinds");
        if (keyBindsSetting != null && keyBindsSetting.isValue()) {
            keyBinds.render(e);
        }
        BooleanSetting staffsSetting = type.getValueByName("Staffs");
        if (staffsSetting != null && staffsSetting.isValue()){
            staffs.render(e);
        }
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
