package dev.luxury.utils.managers;

import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.AutoSell;
import dev.luxury.ui.AutoSellUI;
import dev.luxury.utils.client.ChatUtil;

import java.util.List;

public class AutoSellManager {
    private static AutoSellManager instance;
    private final AutoSellData autoSellData;

    private AutoSellManager() {
        autoSellData = AutoSellData.getInstance();
    }

    public static AutoSellManager getInstance() {
        if (instance == null) {
            instance = new AutoSellManager();
        }
        return instance;
    }

    public AutoSell getAutoSellModule() {
        Module module = ModuleManager.getModule(AutoSell.class);
        if (module instanceof AutoSell) {
            return (AutoSell) module;
        }
        return null;
    }

    public boolean saveConfig(String name) {
        boolean result = autoSellData.saveConfig(name);
        if (result) {
            ChatUtil.sendChat("§aКонфиг AutoSell '" + name + "' сохранен!");
        }
        return result;
    }

    public boolean loadConfig(String name) {
        boolean result = autoSellData.loadConfig(name);
        if (result) {
            ChatUtil.sendChat("§aКонфиг AutoSell '" + name + "' загружен!");
        }
        return result;
    }

    public java.util.List<String> getConfigs() {
        return autoSellData.getConfigs();
    }

    public boolean deleteConfig(String name) {
        boolean result = autoSellData.deleteConfig(name);
        if (result) {
            ChatUtil.sendChat("§cКонфиг AutoSell '" + name + "' удален!");
        }
        return result;
    }

    public void openConfigsFolder() {
        autoSellData.openConfigsFolder();
    }

    public boolean saveAutoSellItems(List<AutoSellUI.SellItem> items) {
        return autoSellData.saveAutoSellItems(items);
    }

    public List<AutoSellUI.SellItem> loadAutoSellItems() {
        return autoSellData.loadAutoSellItems();
    }

    // Метод для инициализации
    public static void init() {
        getInstance();
    }
}