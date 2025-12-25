package dev.luxury.utils.animations;

import dev.luxury.utils.math.TimerUtils;
import lombok.Setter;

public class Animation {
    private final TimerUtils timer = new TimerUtils();
    @Setter private long duration;
    private final Easing easing;
    private boolean forward;
    @Setter private double targetValue;
    private double startValue;
    private boolean initialized = false;

    public Animation(long duration, double value, boolean forward, Easing easing) {
        this.duration = duration;
        this.targetValue = value;
        this.startValue = 0;
        this.forward = forward;
        this.easing = easing;
        this.timer.reset();
    }

    public Animation(long duration, double value, Easing easing) {
        this(duration, value, false, easing);
    }

    public Animation(long duration, double value) {
        this(duration, value, Easing.LINEAR);
    }

    public Animation(long duration) {
        this(duration, 1.0);
    }

    public void setValue(double value) {
        this.targetValue = value;
        if (!initialized) {
            this.startValue = forward ? 0 : value;
            initialized = true;
        }
    }

    public void update(boolean forward) {
        if (this.forward != forward) {
            this.startValue = getValue();
            this.forward = forward;
            timer.reset();
        }
    }

    public boolean finished(boolean forward) {
        return timer.passed(duration) && (forward == this.forward);
    }

    public boolean finished() {
        return timer.passed(duration) && this.forward;
    }

    public float getValue() {
        double elapsed = timer.getElapsed();

        if (duration <= 0) {
            return (float) (forward ? targetValue : startValue);
        }

        if (timer.passed(duration)) {
            return (float) (forward ? targetValue : startValue);
        }

        double progress = Math.min(elapsed / (double) duration, 1.0);
        double easedProgress = easing.apply(progress);

        if (forward) {
            return (float) (startValue + (targetValue - startValue) * easedProgress);
        } else {
            return (float) (targetValue - (targetValue - startValue) * easedProgress);
        }
    }

    public float getLinear() {
        double elapsed = timer.getElapsed();

        if (duration <= 0) {
            return (float) (forward ? targetValue : startValue);
        }

        if (timer.passed(duration)) {
            return (float) (forward ? targetValue : startValue);
        }

        double progress = Math.min(elapsed / (double) duration, 1.0);

        if (forward) {
            return (float) (startValue + (targetValue - startValue) * progress);
        } else {
            return (float) (targetValue - (targetValue - startValue) * progress);
        }
    }

    public float getReversedValue() {
        float value = getValue();
        return (float) (targetValue - value);
    }

    public void reset() {
        timer.reset();
        this.startValue = forward ? 0 : targetValue;
    }

    public void forceValue(double value) {
        this.targetValue = value;
        this.startValue = value;
        timer.reset();
    }

    public boolean isAnimating() {
        return !timer.passed(duration);
    }

    public double getTargetValue() {
        return targetValue;
    }

    public double getStartValue() {
        return startValue;
    }

    public float getProgress() {
        if (duration <= 0) return 1.0f;
        return (float) Math.min(timer.getElapsed() / (double) duration, 1.0);
    }
}