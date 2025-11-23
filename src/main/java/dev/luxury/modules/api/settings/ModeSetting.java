package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class ModeSetting extends Setting {
    private List<String> modes;
    private String value;
    private int index;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.value = defaultValue;
        this.index = this.modes.indexOf(defaultValue);
        if (this.index == -1) {
            this.index = 0;
            this.value = this.modes.get(0);
        }
    }

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description);
        this.modes = Arrays.asList(modes);
        this.value = defaultValue;
        this.index = this.modes.indexOf(defaultValue);
        if (this.index == -1) {
            this.index = 0;
            this.value = this.modes.get(0);
        }
    }

    public void cycle() {
        this.index = (this.index + 1) % this.modes.size();
        this.value = this.modes.get(this.index);
    }

    public void setValue(String value) {
        int newIndex = this.modes.indexOf(value);
        if (newIndex != -1) {
            this.index = newIndex;
            this.value = value;
        }
    }

    public String get() {
        return this.value;
    }

    public boolean is(String mode) {
        return this.value.equalsIgnoreCase(mode);
    }
}

