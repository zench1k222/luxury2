// Файл: dev/luxury/utils/commands/AutoBuyCommand.java
package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.AutoBuyManager;

import java.util.List;

public class AutoBuyCommand extends Command {

    public AutoBuyCommand() {
        super("autobuy", "Управление конфигами AutoBuy", ".autobuy <save/load/list/delete/dir/items> [имя]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cИспользование: .autobuy <save/load/list/delete/dir/items> [имя]");
            return;
        }

        AutoBuyManager configManager = AutoBuyManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autobuy save <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.saveConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг AutoBuy §f" + configName + " §aсохранен!");
                } else {
                    ChatUtil.sendChat("§cОшибка сохранения конфига AutoBuy!");
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autobuy load <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.loadConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг AutoBuy §f" + configName + " §aзагружен!");
                } else {
                    ChatUtil.sendChat("§cКонфиг AutoBuy §f" + configName + " §cне найден!");
                }
            }
            case "list" -> {
                List<String> configs = configManager.getConfigs();
                if (configs.isEmpty()) {
                    ChatUtil.sendChat("§eНет сохраненных конфигов AutoBuy");
                    return;
                }
                ChatUtil.sendChat("§aКонфиги AutoBuy §7(" + configs.size() + ")§7:");
                for (String config : configs) {
                    ChatUtil.sendChat("§7- §f" + config);
                }
            }
            case "delete", "del" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autobuy delete <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.deleteConfig(configName)) {
                    ChatUtil.sendChat("§cКонфиг AutoBuy §f" + configName + " §cудален!");
                } else {
                    ChatUtil.sendChat("§cКонфиг AutoBuy §f" + configName + " §cне найден!");
                }
            }
            case "dir", "folder", "open" -> {
                configManager.openConfigsFolder();
                ChatUtil.sendChat("§aПапка с конфигами AutoBuy открыта!");
            }
            case "items" -> {
                ChatUtil.sendChat("§aИспользуйте UI для управления предметами!");
                ChatUtil.sendChat("§7Предметы сохраняются автоматически при закрытии UI.");
            }
            default -> ChatUtil.sendChat("§cНеизвестная подкоманда! Используйте: save, load, list, delete, dir, items");
        }
    }
}