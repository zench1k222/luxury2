package dev.luxury.utils.math;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerUtils {

    private long millis;
    private long startTime;

    public TimerUtils() {
        reset();
    }

    public boolean finished(float delay) {
        return System.currentTimeMillis() - millis >= delay;
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - millis >= delay;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
    }

    public long getMillis() {
        return System.currentTimeMillis() - millis;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }

    public void reset2() {
        startTime = System.currentTimeMillis();
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - startTime > time;
    }

    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }
}