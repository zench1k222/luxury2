package dev.luxury.modules.impl.other.killaura.rotate.mods.config;


import dev.luxury.modules.impl.other.killaura.rotate.mods.config.api.RotationConfig;
import dev.luxury.modules.impl.other.killaura.rotate.mods.config.api.RotationModeType;
import dev.luxury.utils.math.IntRange;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InterpolationConfig extends RotationConfig {

    private final IntRange horizontalSpeedSetting;
    private final IntRange verticalSpeedSetting  ;
    private final IntRange directionChangeFactor ;
    private final float midPoint ;

    @Override
    public RotationModeType getType() {
        return RotationModeType.INTERPOLATION;
    }
}
