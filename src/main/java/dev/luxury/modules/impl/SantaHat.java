package dev.luxury.modules.impl;


import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "SantaHat",
        category = Category.Render
)
public class SantaHat extends Module {
    private static SantaHat instance;

    public SantaHat() {
        super();
        instance = this;
    }

    public static SantaHat getInstance() {
        return instance;
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