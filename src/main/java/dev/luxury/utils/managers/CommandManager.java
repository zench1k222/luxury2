package dev.luxury.utils.managers;

import dev.luxury.utils.commands.Command;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.utils.commands.ConfigCommand;
import dev.luxury.utils.commands.FriendCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private static CommandManager instance;
    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ModuleManager moduleManager;

    private CommandManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        registerCommand(new FriendCommand());
        registerCommand(new ConfigCommand());

        ConfigManager.init(moduleManager);
    }

    public static void init(ModuleManager moduleManager) {
        if (instance == null) {
            instance = new CommandManager(moduleManager);
        }
    }

    public static CommandManager getInstance() {
        return instance;
    }

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public boolean executeCommand(String message) {
        if (!message.startsWith(prefix)) return false;

        String[] parts = message.substring(prefix.length()).split(" ");
        String commandName = parts[0].toLowerCase();

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                String[] args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 0, args.length);

                try {
                    command.execute(args);
                } catch (Exception e) {
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§cОшибка выполнения команды: " + e.getMessage()), false);
                    }
                    e.printStackTrace();
                }
                return true;
            }
        }

        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§cКоманда не найдена: " + commandName), false);
        }
        return true;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<Command> getCommands() {
        return commands;
    }
}