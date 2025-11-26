package dev.luxury.utils.math;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class TimerUtils {

    private long millis;

    public TimerUtils() {
        reset();
    }
    public boolean finished(float delay) {
        return System.currentTimeMillis() - delay >= millis;
    }
    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }
    @Setter private long startTime = System.currentTimeMillis();

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