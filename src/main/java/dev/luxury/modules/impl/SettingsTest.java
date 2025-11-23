package dev.luxury.modules.impl;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.*;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "SettingsTest",
        desc = "Тестовый модуль для демонстрации всех типов настроек",
        category = Category.Render
)
public class SettingsTest extends Module {

    private final ModeSetting type = new ModeSetting("Тип", "Плавная", new String[]{"Плавная", "Резкая"});
    
    private final SliderSetting attackRange = new SliderSetting("Дистанция атаки", 3.0, 3.0, 6.0, 0.1);
    
    final ModeListSetting targets = new ModeListSetting("Таргеты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Голые невидимки", true),
            new BooleanSetting("Невидимки", true));
    
    final ModeListSetting options = new ModeListSetting("Опции",
            new BooleanSetting("Только криты", true),
            new BooleanSetting("Ломать щит", true),
            new BooleanSetting("Отжимать щит", true),
            new BooleanSetting("Ускорять ротацию при атаке", false),
            new BooleanSetting("Синхронизировать атаку с ТПС", false),
            new BooleanSetting("Фокусировать одну цель", true),
            new BooleanSetting("Коррекция движения", true));
    
    final ModeSetting correctionType = new ModeSetting("Тип коррекции", "Незаметный", new String[]{"Незаметный", "Сфокусированный"});
    
    private final BooleanSetting testBoolean = new BooleanSetting("Test Boolean", "Тестовая булева настройка", false);
    private final SliderSetting testSlider = new SliderSetting("Test Slider", "Тестовая слайдер настройка", 50.0, 0.0, 100.0, 1.0);
    private final ColorSetting testColor = new ColorSetting("Test Color", "Тестовая настройка цвета", 0xFFFFFFFF);
    private final KeySetting testKey = new KeySetting("Test Key", "Тестовая настройка клавиши", GLFW.GLFW_KEY_R);
    private final StringSetting testString = new StringSetting("Test String", "Тестовая строковая настройка", "Default Text");

    public SettingsTest() {
        addSettings(type, attackRange, targets, options, correctionType, testBoolean, testSlider, testColor, testKey, testString);
    }
}

