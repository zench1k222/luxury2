package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.ConfigManager;
import net.minecraft.text.Text;

import java.util.List;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("cfg", "Управление конфигами", ".cfg <save/load/list/delete/dir> [имя]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cИспользование: .cfg <save/load/list/delete/dir> [имя]");
            return;
        }

        ConfigManager configManager = ConfigManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .cfg save <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.saveConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг §f" + configName + " §aсохранен!");
                } else {
                    ChatUtil.sendChat("§cОшибка сохранения конфига!");
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .cfg load <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.loadConfig(configName)) {
                    ChatUtil.sendChat("§aКонфиг §f" + configName + " §aзагружен!");
                } else {
                    ChatUtil.sendChat("§cКонфиг §f" + configName + " §cне найден!");
                }
            }
            case "list" -> {
                List<String> configs = configManager.getConfigs();
                if (configs.isEmpty()) {
                    ChatUtil.sendChat("§eНет сохраненных конфигов");
                    return;
                }
                ChatUtil.sendChat("§aКонфиги §7(" + configs.size() + ")§7:");
                for (String config : configs) {
                    ChatUtil.sendChat("§7- §f" + config);
                }
            }
            case "delete", "del" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .cfg delete <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.deleteConfig(configName)) {
                    ChatUtil.sendChat("§cКонфиг §f" + configName + " §cудален!");
                } else {
                    ChatUtil.sendChat("§cКонфиг §f" + configName + " §cне найден!");
                }
            }
            case "dir", "folder", "open" -> {
                configManager.openConfigsFolder();
                ChatUtil.sendChat("§aПапка с конфигами открыта!");
            }
            default -> ChatUtil.sendChat("§cНеизвестная подкоманда! Используйте: save, load, list, delete, dir");
        }
    }
}