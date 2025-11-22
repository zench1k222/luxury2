package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventKeyInput implements Event {
    private int key, action, modifiers;
}

