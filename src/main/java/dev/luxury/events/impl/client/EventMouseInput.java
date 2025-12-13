package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventMouseInput implements Event {
    private final int button;
    private final int action;
    private final int mods;
}
