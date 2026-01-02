package dev.luxury.modules.impl.other.killaura.rotate.mods;


import dev.luxury.Luxury;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import dev.luxury.modules.impl.other.killaura.rotate.mods.api.RotationMode;

public class OrdinaryMode extends RotationMode {

    public Rotate process(Rotate target) {

        return Luxury.getInstance().getRotationManager().getCurrentRotate().add(Luxury.getInstance().getRotationManager().getCurrentRotate().rotationDeltaTo(target));
    }
}
