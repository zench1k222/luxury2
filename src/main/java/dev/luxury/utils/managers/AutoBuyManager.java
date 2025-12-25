// Файл: dev/luxury/utils/managers/AutoBuyManager.java
package dev.luxury.utils.managers;

import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.AutoBuy;
import dev.luxury.ui.AutoBuyUI;
import dev.luxury.utils.client.ChatUtil;

import java.util.List;

public class AutoBuyManager {
    private static AutoBuyManager instance;
    private final AutoBuyData autoBuyData;

    private AutoBuyManager() {
        autoBuyData = AutoBuyData.getInstance();
    }

    public static AutoBuyManager getInstance() {
        if (instance == null) {
            instance = new AutoBuyManager();
        }
        return instance;
    }

    public AutoBuy getAutoBuyModule() {
        Module module = ModuleManager.getModule(AutoBuy.class);
        if (module instanceof AutoBuy) {
            return (AutoBuy) module;
        }
        return null;
    }

    public boolean saveConfig(String name) {
        boolean result = autoBuyData.saveConfig(name);
        if (result) {
            ChatUtil.sendChat("§aКонфиг AutoBuy '" + name + "' сохранен!");
        }
        return result;
    }

    public boolean loadConfig(String name) {
        boolean result = autoBuyData.loadConfig(name);
        if (result) {
            ChatUtil.sendChat("§aКонфиг AutoBuy '" + name + "' загружен!");
        }
        return result;
    }

    public java.util.List<String> getConfigs() {
        return autoBuyData.getConfigs();
    }

    public boolean deleteConfig(String name) {
        boolean result = autoBuyData.deleteConfig(name);
        if (result) {
            ChatUtil.sendChat("§cКонфиг AutoBuy '" + name + "' удален!");
        }
        return result;
    }

    public void openConfigsFolder() {
        autoBuyData.openConfigsFolder();
    }

    public boolean saveAutoBuyItems(List<AutoBuyUI.BuyItem> items) {
        return autoBuyData.saveAutoBuyItems(items);
    }

    public List<AutoBuyUI.BuyItem> loadAutoBuyItems() {
        return autoBuyData.loadAutoBuyItems();
    }

    // Метод для инициализации
    public static void init() {
        getInstance();
    }
}