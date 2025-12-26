package dev.luxury.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.luxury.common.way.Way;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.api.settings.*;
import dev.luxury.modules.impl.hud.api.DraggableHudElement;
import dev.luxury.modules.impl.hud.api.HUD;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configsDir;
    private final ModuleManager moduleManager;
    private final File luxuryDir;

    private ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        luxuryDir = new File(MinecraftClient.getInstance().runDirectory, "luxury");
        if (!luxuryDir.exists()) luxuryDir.mkdirs();

        configsDir = new File(luxuryDir, "configs");
        if (!configsDir.exists()) configsDir.mkdirs();
    }

    public static void init(ModuleManager moduleManager) {
        if (instance == null) {
            instance = new ConfigManager(moduleManager);
        }
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    public boolean saveConfig(String name) {
        try {
            ConfigData config = new ConfigData();

            Map<String, Boolean> modules = new HashMap<>();
            for (Module module : ModuleManager.getModules()) {
                modules.put(module.getName(), module.isEnabled());
            }
            config.setModules(modules);

            Map<String, Integer> keybinds = new HashMap<>();
            for (Module module : ModuleManager.getModules()) {
                keybinds.put(module.getName(), module.getKey());
            }
            config.setKeybinds(keybinds);

            config.setFriends(new ArrayList<>(FriendManager.getInstance().getFriends()));

            config.setWays(new ArrayList<>(dev.luxury.common.way.WayRepository.getInstance().wayList));

            config.setMacros(new ArrayList<>(MacrosManager.getInstance().getMacros()));

            Map<String, Map<String, Object>> moduleSettings = new HashMap<>();
            for (Module module : ModuleManager.getModules()) {
                Map<String, Object> settings = serializeModuleSettings(module);
                if (!settings.isEmpty()) {
                    moduleSettings.put(module.getName(), settings);
                }
            }
            config.setModuleSettings(moduleSettings);

            Map<String, HudElementData> hudPositions = new HashMap<>();
            if (HUD.getInstance() != null) {
                for (DraggableHudElement element : HUD.getInstance().getElements()) {
                    HudElementData elementData = new HudElementData(
                            element.getX(),
                            element.getY(),
                            element.getWidth(),
                            element.getHeight()
                    );
                    hudPositions.put(element.getName(), elementData);
                }
            }
            config.setHudPositions(hudPositions);

            File configFile = new File(configsDir, name + ".lux");
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadConfig(String name) {
        try {
            File configFile = new File(configsDir, name + ".lux");
            if (!configFile.exists()) {
                return false;
            }

            ConfigData config;
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, ConfigData.class);
            }

            if (config == null) return false;

            Map<String, Boolean> modulesState = config.getModules();
            if (modulesState != null) {
                for (Module module : ModuleManager.getModules()) {
                    String moduleName = module.getName();
                    if (modulesState.containsKey(moduleName)) {
                        boolean shouldBeEnabled = modulesState.get(moduleName);

                        if (shouldBeEnabled && !module.isEnabled()) {
                            module.enable();
                        }
                        else if (!shouldBeEnabled && module.isEnabled()) {
                            module.disable();
                        }
                    }
                }
            }

            Map<String, Integer> keybinds = config.getKeybinds();
            if (keybinds != null) {
                for (Module module : ModuleManager.getModules()) {
                    String moduleName = module.getName();
                    if (keybinds.containsKey(moduleName)) {
                        module.setKey(keybinds.get(moduleName));
                    }
                }
            }

            if (config.getFriends() != null) {
                FriendManager.getInstance().setFriends(config.getFriends());
            }

            if (config.getWays() != null) {
                dev.luxury.common.way.WayRepository.getInstance().wayList.clear();
                dev.luxury.common.way.WayRepository.getInstance().wayList.addAll(config.getWays());
            }

            if (config.getMacros() != null) {
                MacrosManager.getInstance().setMacros(config.getMacros());
            }

            Map<String, Map<String, Object>> moduleSettings = config.getModuleSettings();
            if (moduleSettings != null) {
                for (Module module : ModuleManager.getModules()) {
                    String moduleName = module.getName();
                    if (moduleSettings.containsKey(moduleName)) {
                        deserializeModuleSettings(module, moduleSettings.get(moduleName));
                    }
                }
            }

            Map<String, HudElementData> hudPositions = config.getHudPositions();
            if (hudPositions != null && HUD.getInstance() != null) {
                for (DraggableHudElement element : HUD.getInstance().getElements()) {
                    String elementName = element.getName();
                    if (hudPositions.containsKey(elementName)) {
                        HudElementData data = hudPositions.get(elementName);
                        element.setPosition(data.getX(), data.getY());
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> serializeModuleSettings(Module module) {
        Map<String, Object> settingsMap = new HashMap<>();

        for (Setting setting : module.getSettings()) {
            String settingName = setting.getName();

            if (setting instanceof BooleanSetting) {
                settingsMap.put(settingName, ((BooleanSetting) setting).get());
            }
            else if (setting instanceof ModeSetting) {
                Map<String, Object> modeData = new HashMap<>();
                modeData.put("value", ((ModeSetting) setting).get());
                modeData.put("index", ((ModeSetting) setting).getIndex());
                settingsMap.put(settingName, modeData);
            }
            else if (setting instanceof ModeListSetting) {
                Map<String, Boolean> modeListData = new HashMap<>();
                for (BooleanSetting boolSetting : ((ModeListSetting) setting).getSettings()) {
                    modeListData.put(boolSetting.getName(), boolSetting.get());
                }
                settingsMap.put(settingName, modeListData);
            }
            else if (setting instanceof SliderSetting) {
                Map<String, Object> sliderData = new HashMap<>();
                sliderData.put("value", ((SliderSetting) setting).getValue());
                settingsMap.put(settingName, sliderData);
            }
            else if (setting instanceof StringSetting) {
                settingsMap.put(settingName, ((StringSetting) setting).getValue());
            }
            else if (setting instanceof ColorSetting) {
                settingsMap.put(settingName, ((ColorSetting) setting).getValue());
            }
            else if (setting instanceof KeySetting) {
                Map<String, Object> keyData = new HashMap<>();
                keyData.put("value", ((KeySetting) setting).getValue());
                keyData.put("mouse", ((KeySetting) setting).isMouse());
                settingsMap.put(settingName, keyData);
            }
        }

        return settingsMap;
    }

    private void deserializeModuleSettings(Module module, Map<String, Object> settingsMap) {
        for (Setting setting : module.getSettings()) {
            String settingName = setting.getName();

            if (!settingsMap.containsKey(settingName)) {
                continue;
            }

            Object value = settingsMap.get(settingName);

            if (setting instanceof BooleanSetting && value instanceof Boolean) {
                ((BooleanSetting) setting).setValue((Boolean) value);
            }
            else if (setting instanceof ModeSetting && value instanceof Map) {
                Map<String, Object> modeData = (Map<String, Object>) value;
                if (modeData.containsKey("value")) {
                    ((ModeSetting) setting).setValue(modeData.get("value").toString());
                }
            }
            else if (setting instanceof ModeListSetting && value instanceof Map) {
                Map<String, Boolean> modeListData = (Map<String, Boolean>) value;
                ModeListSetting modeListSetting = (ModeListSetting) setting;

                for (BooleanSetting boolSetting : modeListSetting.getSettings()) {
                    String boolName = boolSetting.getName();
                    if (modeListData.containsKey(boolName)) {
                        boolSetting.setValue(modeListData.get(boolName));
                    }
                }
            }
            else if (setting instanceof SliderSetting && value instanceof Map) {
                Map<String, Object> sliderData = (Map<String, Object>) value;
                if (sliderData.containsKey("value") && sliderData.get("value") instanceof Number) {
                    double doubleValue = ((Number) sliderData.get("value")).doubleValue();
                    ((SliderSetting) setting).setValue(doubleValue);
                }
            }
            else if (setting instanceof StringSetting && value instanceof String) {
                ((StringSetting) setting).setValue((String) value);
            }
            else if (setting instanceof ColorSetting && value instanceof Number) {
                ((ColorSetting) setting).setValue(((Number) value).intValue());
            }
            else if (setting instanceof KeySetting && value instanceof Map) {
                Map<String, Object> keyData = (Map<String, Object>) value;
                KeySetting keySetting = (KeySetting) setting;

                if (keyData.containsKey("value") && keyData.get("value") instanceof Number) {
                    keySetting.setValue(((Number) keyData.get("value")).intValue());
                }
                if (keyData.containsKey("mouse") && keyData.get("mouse") instanceof Boolean) {
                    keySetting.setMouse((Boolean) keyData.get("mouse"));
                }
            }
        }
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".lux"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".lux", "");
                configs.add(name);
            }
        }
        return configs;
    }

    public boolean deleteConfig(String name) {
        File configFile = new File(configsDir, name + ".lux");
        return configFile.exists() && configFile.delete();
    }

    public void openConfigsFolder() {
        try {
            if (!configsDir.exists()) {
                configsDir.mkdirs();
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(configsDir);
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                Runtime runtime = Runtime.getRuntime();

                if (os.contains("win")) {
                    runtime.exec("explorer " + configsDir.getAbsolutePath());
                } else if (os.contains("mac")) {
                    runtime.exec("open " + configsDir.getAbsolutePath());
                } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    runtime.exec("xdg-open " + configsDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось открыть папку: " + configsDir.getAbsolutePath(), e);
        }
    }

    public File getConfigsDir() {
        return configsDir;
    }

    private static class ConfigData {
        private Map<String, Boolean> modules;
        private Map<String, Integer> keybinds;
        private List<String> friends;
        private List<Way> ways;
        private List<Macro> macros;
        private Map<String, Map<String, Object>> moduleSettings;
        private Map<String, HudElementData> hudPositions;

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

        public List<Way> getWays() {
            return ways;
        }

        public void setWays(List<Way> ways) {
            this.ways = ways;
        }

        public List<Macro> getMacros() {
            return macros;
        }

        public void setMacros(List<Macro> macros) {
            this.macros = macros;
        }

        public Map<String, Map<String, Object>> getModuleSettings() {
            return moduleSettings;
        }

        public void setModuleSettings(Map<String, Map<String, Object>> moduleSettings) {
            this.moduleSettings = moduleSettings;
        }

        public Map<String, HudElementData> getHudPositions() {
            return hudPositions;
        }

        public void setHudPositions(Map<String, HudElementData> hudPositions) {
            this.hudPositions = hudPositions;
        }
    }

    private static class HudElementData {
        private float x;
        private float y;
        private float width;
        private float height;

        public HudElementData() {}

        public HudElementData(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }
    }
}