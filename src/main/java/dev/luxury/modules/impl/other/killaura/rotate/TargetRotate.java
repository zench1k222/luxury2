package dev.luxury.modules.impl.other.killaura.rotate;




import dev.luxury.modules.impl.other.killaura.rotate.mods.config.api.RotationConfig;

import java.util.function.Supplier;


public record TargetRotate(Rotate targetRotate, Supplier<Rotate> rotation, RotationConfig rotationConfigBack) {
}
