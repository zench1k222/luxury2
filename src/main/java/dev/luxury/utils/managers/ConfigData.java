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

    // Добавляем новые поля для расширенной сериализации настроек
    private Map<String, Map<String, SettingData>> advancedModuleSettings = new HashMap<>();

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

    public Map<String, Map<String, SettingData>> getAdvancedModuleSettings() {
        return advancedModuleSettings;
    }

    public void setAdvancedModuleSettings(Map<String, Map<String, SettingData>> advancedModuleSettings) {
        this.advancedModuleSettings = advancedModuleSettings;
    }

    // Внутренний класс для хранения данных настроек с типом
    public static class SettingData {
        private String type;
        private Object value;
        private Map<String, Object> additionalData;

        public SettingData() {}

        public SettingData(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        public SettingData(String type, Object value, Map<String, Object> additionalData) {
            this.type = type;
            this.value = value;
            this.additionalData = additionalData;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }

        public void setAdditionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
        }
    }
}