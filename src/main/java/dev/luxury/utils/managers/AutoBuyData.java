package dev.luxury.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.luxury.modules.impl.AutoBuy;
import dev.luxury.ui.AutoBuyUI;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AutoBuyData {
    private static AutoBuyData instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configsDir;
    private final File luxuryDir;

    private AutoBuyData() {
        luxuryDir = new File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "luxury");
        if (!luxuryDir.exists()) luxuryDir.mkdirs();

        configsDir = new File(luxuryDir, "autobuy");
        if (!configsDir.exists()) configsDir.mkdirs();
    }

    public static AutoBuyData getInstance() {
        if (instance == null) {
            instance = new AutoBuyData();
        }
        return instance;
    }

    public boolean saveConfig(String name) {
        try {
            AutoBuyManager manager = AutoBuyManager.getInstance();
            if (manager == null || manager.getAutoBuyModule() == null) {
                return false;
            }

            AutoBuy autoBuyModule = manager.getAutoBuyModule();
            AutoBuyConfig config = new AutoBuyConfig();

            // Сохраняем настройки модуля
            config.delay = autoBuyModule.delay.getValue();
            config.debugMode = autoBuyModule.debugMode.get();
            config.autoStart = autoBuyModule.autoStart.get();
            config.buyEnabled = autoBuyModule.buyEnabled.get();
            config.refreshDelay = autoBuyModule.refreshDelay.getValue();

            // Сохраняем предметы для покупки
            config.buyItems = new ArrayList<>();
            for (AutoBuy.BuyItem item : autoBuyModule.getBuyItems()) {
                if (item.maxPricePerUnit > Integer.MAX_VALUE) {
                    dev.luxury.utils.client.ChatUtil.sendError("Цена " + item.maxPricePerUnit +
                            " для " + item.id + " слишком большая для сохранения!");
                    continue;
                }
                config.buyItems.add(new AutoBuyItemData(
                        item.id,
                        (int) item.maxPricePerUnit,
                        item.quantity,
                        item.enabled
                ));
            }

            File configFile = new File(configsDir, name + ".ablux");
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
            File configFile = new File(configsDir, name + ".ablux");
            if (!configFile.exists()) {
                return false;
            }

            AutoBuyConfig config;
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, AutoBuyConfig.class);
            }

            if (config == null) return false;

            AutoBuyManager manager = AutoBuyManager.getInstance();
            if (manager == null || manager.getAutoBuyModule() == null) {
                return false;
            }

            AutoBuy autoBuyModule = manager.getAutoBuyModule();

            // Загружаем настройки модуля
            autoBuyModule.delay.setValue(config.delay);
            autoBuyModule.debugMode.setValue(config.debugMode);
            autoBuyModule.autoStart.setValue(config.autoStart);
            autoBuyModule.buyEnabled.setValue(config.buyEnabled);
            autoBuyModule.refreshDelay.setValue(config.refreshDelay);

            // Загружаем предметы для покупки
            if (config.buyItems != null && !config.buyItems.isEmpty()) {
                List<AutoBuy.BuyItem> items = new ArrayList<>();
                for (AutoBuyItemData data : config.buyItems) {
                    items.add(new AutoBuy.BuyItem(
                            data.id,
                            data.maxPricePerUnit,
                            data.quantity,
                            data.enabled
                    ));
                }
                autoBuyModule.setBuyItems(items);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".ablux"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".ablux", "");
                configs.add(name);
            }
        }
        return configs;
    }

    public boolean deleteConfig(String name) {
        File configFile = new File(configsDir, name + ".ablux");
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

    public boolean saveAutoBuyItems(List<AutoBuyUI.BuyItem> items) {
        try {
            List<AutoBuyItemData> itemDataList = new ArrayList<>();
            for (AutoBuyUI.BuyItem item : items) {
                itemDataList.add(new AutoBuyItemData(item.id, item.maxPricePerUnit, item.quantity, item.enabled));
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

    public List<AutoBuyUI.BuyItem> loadAutoBuyItems() {
        List<AutoBuyUI.BuyItem> items = new ArrayList<>();
        File itemsFile = new File(configsDir, "items.json");

        if (!itemsFile.exists()) {
            return items;
        }

        try (FileReader reader = new FileReader(itemsFile)) {
            Type listType = new TypeToken<List<AutoBuyItemData>>() {}.getType();
            List<AutoBuyItemData> itemDataList = gson.fromJson(reader, listType);

            if (itemDataList != null) {
                for (AutoBuyItemData data : itemDataList) {
                    items.add(new AutoBuyUI.BuyItem(data.id, data.maxPricePerUnit, data.quantity, data.enabled));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    // Классы для хранения данных конфига
    private static class AutoBuyConfig {
        public double delay;
        public boolean debugMode;
        public boolean autoStart;
        public boolean buyEnabled;
        public double refreshDelay;
        public List<AutoBuyItemData> buyItems;
    }

    public static class AutoBuyItemData {
        public String id;
        public int maxPricePerUnit;
        public int quantity;
        public boolean enabled;

        public AutoBuyItemData() {}

        public AutoBuyItemData(String id, int maxPricePerUnit, int quantity, boolean enabled) {
            this.id = id;
            this.maxPricePerUnit = maxPricePerUnit;
            this.quantity = quantity;
            this.enabled = enabled;
        }
    }
}