package dev.luxury.modules.api.settings;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModeListSetting extends Setting {
    private List<BooleanSetting> settings = new ArrayList<>();

    public ModeListSetting(String name, BooleanSetting... settings) {
        super(name);
        for (BooleanSetting setting : settings) {
            this.settings.add(setting);
        }
    }

    public ModeListSetting(String name, String description, BooleanSetting... settings) {
        super(name, description);
        for (BooleanSetting setting : settings) {
            this.settings.add(setting);
        }
    }

    public BooleanSetting getValueByName(String name) {
        for (BooleanSetting setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public List<BooleanSetting> getSettings() {
        return settings;
    }
}

