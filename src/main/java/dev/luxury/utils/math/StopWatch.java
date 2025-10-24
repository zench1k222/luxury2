package dev.luxury.utils.math;

import lombok.Getter;
import lombok.Setter;

public class StopWatch {
    private long nanoTime;

    @Getter
    @Setter
    private long lastMS;

    public StopWatch() {
        reset();
    }

    public void reset() {
        nanoTime = System.nanoTime();
        lastMS = System.currentTimeMillis();
    }

    public boolean hasElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public boolean every(float ms) {
        if (hasElapsed((long) ms)) {
            reset();
            return true;
        }
        return false;
    }

    public void setDelay(long delay) {
        lastMS = System.currentTimeMillis() + delay;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public boolean isRunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }

    public long getElapsedTimeNano() {
        return (System.nanoTime() - nanoTime) / 1000000L;
    }
}
