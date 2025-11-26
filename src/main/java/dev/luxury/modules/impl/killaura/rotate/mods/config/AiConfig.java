package dev.luxury.modules.impl.killaura.rotate.mods.config;


import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationModeType;
import dev.luxury.utils.math.IntRange;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiConfig extends RotationConfig {
    @Builder.Default
    private int tick = 3;
    @Builder.Default
    private InterpolationConfig interpolationConfig =new InterpolationConfig(new IntRange(2,5),new IntRange(5,8),new IntRange(20,30),0.35f);

    @Override
    public RotationModeType getType() {
        return RotationModeType.AI;
    }
}
