package dev.luxury.modules.impl.killaura.rotate.mods.config;

import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationModeType;

public class SlothAIConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.SLOTH_AI;  // Измените на SLOTH_AI
    }
}