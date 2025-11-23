package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorSetting extends Setting {
    private int value;

    public ColorSetting(String name, int defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public ColorSetting(String name, String description, int defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }

    public int getRed() {
        return (this.value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (this.value >> 8) & 0xFF;
    }

    public int getBlue() {
        return this.value & 0xFF;
    }

    public int getAlpha() {
        return (this.value >> 24) & 0xFF;
    }

    public void setColor(int r, int g, int b, int a) {
        this.value = (a << 24) | (r << 16) | (g << 8) | b;
    }
}

