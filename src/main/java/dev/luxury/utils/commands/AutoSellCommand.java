package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.AutoSellManager;
import ru.nexusguard.protection.annotations.Native;

import java.util.List;

public class AutoSellCommand extends Command {

    public AutoSellCommand() {
        super("autosell", "Управление конфигами AutoSell", ".autosell <save/load/list/delete/dir/items> [имя]");
    }

    @Native
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cИспользование: .autosell <save/load/list/delete/dir/items> [имя]");
            return;
        }

        AutoSellManager configManager = AutoSellManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autosell save <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.saveConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг AutoSell §f" + configName + " §aсохранен!");
                } else {
                    ChatUtil.sendChat("§cОшибка сохранения конфига AutoSell!");
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autosell load <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.loadConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг AutoSell §f" + configName + " §aзагружен!");
                } else {
                    ChatUtil.sendChat("§cКонфиг AutoSell §f" + configName + " §cне найден!");
                }
            }
            case "list" -> {
                List<String> configs = configManager.getConfigs();
                if (configs.isEmpty()) {
                    ChatUtil.sendChat("§eНет сохраненных конфигов AutoSell");
                    return;
                }
                ChatUtil.sendChat("§aКонфиги AutoSell §7(" + configs.size() + ")§7:");
                for (String config : configs) {
                    ChatUtil.sendChat("§7- §f" + config);
                }
            }
            case "delete", "del" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .autosell delete <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.deleteConfig(configName)) {
                    ChatUtil.sendChat("§cКонфиг AutoSell §f" + configName + " §cудален!");
                } else {
                    ChatUtil.sendChat("§cКонфиг AutoSell §f" + configName + " §cне найден!");
                }
            }
            case "dir", "folder", "open" -> {
                configManager.openConfigsFolder();
                ChatUtil.sendChat("§aПапка с конфигами AutoSell открыта!");
            }
            case "items" -> {
                ChatUtil.sendChat("§aИспользуйте UI для управления предметами!");
                ChatUtil.sendChat("§7Предметы сохраняются автоматически при закрытии UI.");
            }
            default -> ChatUtil.sendChat("§cНеизвестная подкоманда! Используйте: save, load, list, delete, dir, items");
        }
    }
}