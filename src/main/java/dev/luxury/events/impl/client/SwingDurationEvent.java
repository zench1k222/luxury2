package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.callables.EventCancellable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwingDurationEvent extends EventCancellable {
    float animation;
}
