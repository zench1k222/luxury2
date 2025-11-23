package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Setting {
    private String name;
    private String description;
    private boolean visible = true;

    public Setting(String name) {
        this.name = name;
    }

    public Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }
}

