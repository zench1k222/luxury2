package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ButtonSetting extends Setting {
    private Runnable action;
    private float width;
    private float height;

    public ButtonSetting(String name, Runnable action, float width, float height) {
        super(name);
        this.action = action;
        this.width = width;
        this.height = height;
    }

    public ButtonSetting(String name, String description, Runnable action, float width, float height) {
        super(name, description);
        this.action = action;
        this.width = width;
        this.height = height;
    }

    public void execute() {
        if (action != null) {
            action.run();
        }
    }
}