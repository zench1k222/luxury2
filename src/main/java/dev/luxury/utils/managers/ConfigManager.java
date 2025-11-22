package dev.luxury.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
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

    private ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        File luxuryDir = new File(MinecraftClient.getInstance().runDirectory, "luxury");
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

            Map<String, Map<String, Object>> moduleSettings = new HashMap<>();
            for (Module module : ModuleManager.getModules()) {
                Map<String, Object> settings = new HashMap<>();
                if (!settings.isEmpty()) {
                    moduleSettings.put(module.getName(), settings);
                }
            }
            config.setModuleSettings(moduleSettings);

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

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
}