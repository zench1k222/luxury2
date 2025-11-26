package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.callables.EventCancellable;
import net.minecraft.util.PlayerInput;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventMoveInput extends EventCancellable {
    private PlayerInput input;
    private float forward, strafe;
}