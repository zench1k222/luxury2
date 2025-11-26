package dev.luxury.modules.impl.killaura.rotate;




import dev.luxury.modules.impl.killaura.rotate.mods.config.api.RotationConfig;

import java.util.function.Supplier;


public record TargetRotate(Rotate targetRotate, Supplier<Rotate> rotation, RotationConfig rotationConfigBack) {
}
