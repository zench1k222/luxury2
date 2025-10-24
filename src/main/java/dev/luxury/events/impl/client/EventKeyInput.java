package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.Getter;

@Getter
public class EventKeyInput implements Event {
    private final int key,action;

    public EventKeyInput(int key, int action) {
        this.key = key;
        this.action = action;
    }
}
