package dev.luxury.ui.settings;

import dev.luxury.modules.api.settings.*;

import java.util.HashMap;
import java.util.Map;

public class SettingRendererManager {
    private static final Map<Class<? extends Setting>, SettingRenderer> renderers = new HashMap<>();
    
    static {
        registerRenderer(BooleanSetting.class, new BooleanSettingRenderer());
        registerRenderer(ModeSetting.class, new ModeSettingRenderer());
        registerRenderer(SliderSetting.class, new SliderSettingRenderer());
        registerRenderer(ModeListSetting.class, new ModeListSettingRenderer());
        registerRenderer(ColorSetting.class, new ColorSettingRenderer());
        registerRenderer(KeySetting.class, new KeySettingRenderer());
        registerRenderer(StringSetting.class, new StringSettingRenderer());
        registerRenderer(ButtonSetting.class, new ButtonSettingRenderer());
    }
    
    public static void registerRenderer(Class<? extends Setting> settingClass, SettingRenderer renderer) {
        renderers.put(settingClass, renderer);
    }
    
    public static SettingRenderer getRenderer(Setting setting) {
        return renderers.get(setting.getClass());
    }
}

