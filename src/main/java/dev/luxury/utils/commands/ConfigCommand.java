package dev.luxury.utils.commands;

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
            sendMessage("§cИспользование: .cfg <save/load/list/delete/dir> [имя]");
            return;
        }

        ConfigManager configManager = ConfigManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length < 2) {
                    sendMessage("§cИспользование: .cfg save <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.saveConfig(configName)) {
                    sendMessage("§aКонфиг §f" + configName + " §aсохранен!", "Config Manager");
                } else {
                    sendMessage("§cОшибка сохранения конфига!", "Config Manager");
                }
            }
            case "load" -> {
                if (args.length < 2) {
                    sendMessage("§cИспользование: .cfg load <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.loadConfig(configName)) {
                    sendMessage("§aКонфиг §f" + configName + " §aзагружен!", "Config Manager");
                } else {
                    sendMessage("§cКонфиг §f" + configName + " §cне найден!", "Config Manager");
                }
            }
            case "list" -> {
                List<String> configs = configManager.getConfigs();
                if (configs.isEmpty()) {
                    sendMessage("§eНет сохраненных конфигов", "Config Manager");
                    return;
                }
                sendMessage("§aКонфиги §7(" + configs.size() + ")§7:", "Config Manager");
                for (String config : configs) {
                    sendMessage("§7- §f" + config, "Config Manager");
                }
            }
            case "delete", "del" -> {
                if (args.length < 2) {
                    sendMessage("§cИспользование: .cfg delete <имя>");
                    return;
                }
                String configName = args[1];
                if (configManager.deleteConfig(configName)) {
                    sendMessage("§cКонфиг §f" + configName + " §cудален!", "Config Manager");
                } else {
                    sendMessage("§cКонфиг §f" + configName + " §cне найден!", "Config Manager");
                }
            }
            case "dir", "folder", "open" -> {
                configManager.openConfigsFolder();
                sendMessage("§aПапка с конфигами открыта!", "Config Manager");
            }
            default -> sendMessage("§cНеизвестная подкоманда! Используйте: save, load, list, delete, dir");
        }
    }

    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }

    private void sendMessage(String message, String prefix) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§a" + prefix + " §7» " + message), false);
        }
    }
}