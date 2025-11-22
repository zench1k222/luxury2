package dev.luxury.utils.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigData {
    private Map<String, Boolean> modules = new HashMap<>();
    private Map<String, Integer> keybinds = new HashMap<>();
    private List<String> friends = new ArrayList<>();
    private Map<String, Map<String, Object>> moduleSettings = new HashMap<>();

    public Map<String, Boolean> getModules() {
        return modules;
    }

    public void setModules(Map<String, Boolean> modules) {
        this.modules = modules;
    }

    public Map<String, Integer> getKeybinds() {
        return keybinds;
    }

    public void setKeybinds(Map<String, Integer> keybinds) {
        this.keybinds = keybinds;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public Map<String, Map<String, Object>> getModuleSettings() {
        return moduleSettings;
    }

    public void setModuleSettings(Map<String, Map<String, Object>> moduleSettings) {
        this.moduleSettings = moduleSettings;
    }
}