package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringSetting extends Setting {
    private String value;

    public StringSetting(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public StringSetting(String name, String description, String defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }
}

