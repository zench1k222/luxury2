package dev.luxury.events.impl.client;


import dev.luxury.events.impl.eventapi.events.callables.EventCancellable;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventCamera extends EventCancellable {
    boolean cameraClip;
    float distance;
    Rotate angle;
}
