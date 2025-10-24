package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.font.FontDraw;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "HUD",
        desc = "Интерфейс чита",
        category = Category.Render,
        key = GLFW.GLFW_KEY_U

)
public class HUD extends Module {
    WaterMark waterMark = new WaterMark();
    Info info = new Info();
    TargetHud targetHud = new TargetHud();
    MediaPlayer mediaPlayer = new MediaPlayer();



@EventTarget
    public  void initHud(EventRender2D e){

    waterMark.render(e);

    info.render(e);

    targetHud.render(e);

    mediaPlayer.render(e);
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
