package dev.luxury.events.impl.eventapi.events.callables;

import dev.luxury.events.impl.eventapi.events.Cancellable;
import dev.luxury.events.impl.eventapi.events.Event;

/**
 * Abstract example implementation of the Cancellable interface.
 *
 * @author DarkMagician6
 * @since August 27, 2013
 */
public abstract class EventCancellable implements Event, Cancellable {

    private boolean cancelled;

    protected EventCancellable() {
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }


    @Override
    public void setCancelled(boolean state) {
        cancelled = state;
    }

}
