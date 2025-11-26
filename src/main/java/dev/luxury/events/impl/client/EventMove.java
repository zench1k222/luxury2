package dev.luxury.events.impl.client;


import dev.luxury.events.impl.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;

@AllArgsConstructor
@Getter
@Setter
public class EventMove extends EventCancellable {
    private Vec3d movePos;
}
