package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeySetting extends Setting {
    private int value;

    public KeySetting(String name, int defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public KeySetting(String name, String description, int defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }
}

