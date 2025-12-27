package dev.luxury.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.luxury.modules.impl.AutoSell;
import dev.luxury.ui.AutoSellUI;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AutoSellData {
    private static AutoSellData instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configsDir;
    private final File luxuryDir;

    private AutoSellData() {
        luxuryDir = new File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "luxury");
        if (!luxuryDir.exists()) luxuryDir.mkdirs();

        configsDir = new File(luxuryDir, "autosell");
        if (!configsDir.exists()) configsDir.mkdirs();
    }

    public static AutoSellData getInstance() {
        if (instance == null) {
            instance = new AutoSellData();
        }
        return instance;
    }

    public boolean saveConfig(String name) {
        try {
            AutoSellManager manager = AutoSellManager.getInstance();
            if (manager == null || manager.getAutoSellModule() == null) {
                return false;
            }

            AutoSell autoSellModule = manager.getAutoSellModule();
            AutoSellConfig config = new AutoSellConfig();

            // Сохраняем настройки модуля
            config.sellDelay = autoSellModule.sellDelay.getValue();
            config.debugMode = autoSellModule.debugMode.get();
            config.autoStart = autoSellModule.autoStart.get();
            config.sellEnabled = autoSellModule.sellEnabled.get();
            config.hotbarReplacement = autoSellModule.hotbarReplacement.get();
            config.priceMargin = autoSellModule.priceMargin.getValue();

            // Сохраняем предметы для продажи
            config.sellItems = new ArrayList<>();
            for (AutoSell.SellItem item : autoSellModule.getSellItems()) {
                if (item.sellPrice > Integer.MAX_VALUE) {
                    dev.luxury.utils.client.ChatUtil.sendError("Цена " + item.sellPrice +
                            " для " + item.id + " слишком большая для сохранения!");
                    continue;
                }
                config.sellItems.add(new AutoSellItemData(
                        item.id,
                        (int) item.sellPrice,
                        item.enabled
                ));
            }

            File configFile = new File(configsDir, name + ".aslux");
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadConfig(String name) {
        try {
            File configFile = new File(configsDir, name + ".aslux");
            if (!configFile.exists()) {
                return false;
            }

            AutoSellConfig config;
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, AutoSellConfig.class);
            }

            if (config == null) return false;

            AutoSellManager manager = AutoSellManager.getInstance();
            if (manager == null || manager.getAutoSellModule() == null) {
                return false;
            }

            AutoSell autoSellModule = manager.getAutoSellModule();

            // Загружаем настройки модуля
            autoSellModule.sellDelay.setValue(config.sellDelay);
            autoSellModule.debugMode.setValue(config.debugMode);
            autoSellModule.autoStart.setValue(config.autoStart);
            autoSellModule.sellEnabled.setValue(config.sellEnabled);
            autoSellModule.hotbarReplacement.setValue(config.hotbarReplacement);
            autoSellModule.priceMargin.setValue(config.priceMargin);

            // Загружаем предметы для продажи
            if (config.sellItems != null && !config.sellItems.isEmpty()) {
                List<AutoSell.SellItem> items = new ArrayList<>();
                for (AutoSellItemData data : config.sellItems) {
                    items.add(new AutoSell.SellItem(
                            data.id,
                            data.sellPrice,
                            data.enabled
                    ));
                }
                autoSellModule.setSellItems(items);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".aslux"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".aslux", "");
                configs.add(name);
            }
        }
        return configs;
    }

    public boolean deleteConfig(String name) {
        File configFile = new File(configsDir, name + ".aslux");
        return configFile.exists() && configFile.delete();
    }

    public void openConfigsFolder() {
        try {
            if (!configsDir.exists()) {
                configsDir.mkdirs();
            }

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(configsDir);
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

    public boolean saveAutoSellItems(List<AutoSellUI.SellItem> items) {
        try {
            List<AutoSellItemData> itemDataList = new ArrayList<>();
            for (AutoSellUI.SellItem item : items) {
                itemDataList.add(new AutoSellItemData(item.id, item.sellPrice, item.enabled));
            }

            File itemsFile = new File(configsDir, "items.json");
            try (FileWriter writer = new FileWriter(itemsFile)) {
                gson.toJson(itemDataList, writer);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<AutoSellUI.SellItem> loadAutoSellItems() {
        List<AutoSellUI.SellItem> items = new ArrayList<>();
        File itemsFile = new File(configsDir, "items.json");

        if (!itemsFile.exists()) {
            return createDefaultItems();
        }

        try (FileReader reader = new FileReader(itemsFile)) {
            Type listType = new TypeToken<List<AutoSellItemData>>() {}.getType();
            List<AutoSellItemData> itemDataList = gson.fromJson(reader, listType);

            if (itemDataList != null) {
                for (AutoSellItemData data : itemDataList) {
                    items.add(new AutoSellUI.SellItem(data.id, data.sellPrice, data.enabled));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    private List<AutoSellUI.SellItem> createDefaultItems() {
        List<AutoSellUI.SellItem> defaultItems = new ArrayList<>();
        defaultItems.add(new AutoSellUI.SellItem("end_crystal", 50000, true));
        defaultItems.add(new AutoSellUI.SellItem("ender_pearl", 10000, true));
        defaultItems.add(new AutoSellUI.SellItem("diamond", 15000, true));
        defaultItems.add(new AutoSellUI.SellItem("emerald", 12000, true));
        return defaultItems;
    }

    // Классы для хранения данных конфига
    private static class AutoSellConfig {
        public double sellDelay;
        public boolean debugMode;
        public boolean autoStart;
        public boolean sellEnabled;
        public boolean hotbarReplacement;
        public double priceMargin;
        public List<AutoSellItemData> sellItems;
    }

    public static class AutoSellItemData {
        public String id;
        public int sellPrice;
        public boolean enabled;

        public AutoSellItemData() {}

        public AutoSellItemData(String id, int sellPrice, boolean enabled) {
            this.id = id;
            this.sellPrice = sellPrice;
            this.enabled = enabled;
        }
    }
}