package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SliderSetting extends Setting {
    private double value;
    private double min;
    private double max;
    private double increment;

    public SliderSetting(String name, double defaultValue, double min, double max) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = 0.1;
    }

    public SliderSetting(String name, String description, double defaultValue, double min, double max) {
        super(name, description);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = 0.1;
    }

    public SliderSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public SliderSetting(String name, String description, double defaultValue, double min, double max, double increment) {
        super(name, description);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public void setValue(double value) {
        this.value = Math.max(this.min, Math.min(this.max, value));
    }

    public int getIntValue() {
        return (int) this.value;
    }

    public float getFloatValue() {
        return (float) this.value;
    }
}

