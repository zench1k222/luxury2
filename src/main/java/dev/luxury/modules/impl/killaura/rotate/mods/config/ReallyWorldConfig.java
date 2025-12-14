package dev.luxury.modules.impl.killaura.rotate.mods.config;

import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationModeType;

public class ReallyWorldConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.REALLY_WORLD;
    }
}