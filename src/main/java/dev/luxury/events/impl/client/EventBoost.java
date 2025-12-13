package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.Getter;
import lombok.Setter;

/**
 * Аналог leet EventBoost для обработки ускорения от фейерверков.
 */
@Getter
@Setter
public class EventBoost implements Event {
    private static final EventBoost INSTANCE = new EventBoost();

    public double speedx, speedy, speedz, a, b;
    public float yaw, pitch;

    public static EventBoost build(float yaw, float pitch) {
        INSTANCE.speedx = 1.5;
        INSTANCE.speedy = 1.5;
        INSTANCE.speedz = 1.5;
        INSTANCE.a = 0.1;
        INSTANCE.b = 0.5;
        INSTANCE.yaw = yaw;
        INSTANCE.pitch = pitch;
        return INSTANCE;
    }
}

