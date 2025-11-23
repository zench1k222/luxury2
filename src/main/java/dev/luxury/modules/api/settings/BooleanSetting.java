package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }

    public boolean get() {
        return this.value;
    }

    public void toggle() {
        this.value = !this.value;
    }
}

