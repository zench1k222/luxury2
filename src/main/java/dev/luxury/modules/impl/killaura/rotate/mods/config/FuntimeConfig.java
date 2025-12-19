package dev.luxury.modules.impl.killaura.rotate.mods.config;


import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationModeType;

public class FuntimeConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.FUNTIME;
    }
}