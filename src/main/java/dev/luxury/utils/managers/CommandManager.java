package dev.luxury.utils.managers;

import dev.luxury.utils.commands.Command;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.utils.commands.ConfigCommand;
import dev.luxury.utils.commands.FriendCommand;
import dev.luxury.utils.commands.HelpCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        registerCommand(new HelpCommand());

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

    public Stream<String> tabComplete(String message) {
        if (!message.startsWith(prefix)) return Stream.empty();

        String withoutPrefix = message.substring(prefix.length());
        String[] parts = withoutPrefix.split(" ", -1);
        String commandPart = parts[0];
        String currentArg = parts.length > 1 ? parts[parts.length - 1] : "";

        if (parts.length == 1) {
            return commands.stream()
                    .map(Command::getName)
                    .filter(c -> c.startsWith(commandPart))
                    .map(s -> prefix + s);
        }

        String cmdName = commandPart.toLowerCase();

        switch (cmdName) {
            case "cfg" -> {
                if (parts.length == 2) {
                    return Stream.of("save", "load", "list", "delete", "dir")
                            .filter(a -> a.startsWith(currentArg));
                }

                if (parts.length == 3) {
                    String subCommand = parts[1].toLowerCase();
                    switch (subCommand) {
                        case "load", "delete" -> {
                            List<String> configs = ConfigManager.getInstance().getConfigs();
                            return configs.stream().filter(c -> c.startsWith(currentArg));
                        }
                        case "save", "list", "dir" -> {
                            return Stream.empty();
                        }
                    }
                }

                return Stream.empty();
            }

            case "friend" -> {
                if (parts.length == 2) {
                    return Stream.of("add", "del", "delete", "remove", "list", "clear")
                            .filter(a -> a.startsWith(currentArg));
                }

                if (parts.length == 3) {
                    String subCommand = parts[1].toLowerCase();
                    switch (subCommand) {
                        case "add" -> {
                            List<String> playersOnline = mc.world != null
                                    ? mc.world.getPlayers().stream()
                                    .map(p -> p.getName().getString())
                                    .toList()
                                    : List.of();
                            return playersOnline.stream().filter(p -> p.startsWith(currentArg));
                        }
                        case "del", "delete", "remove" -> {
                            List<String> friends = FriendManager.getInstance().getFriends();
                            return friends.stream().filter(f -> f.startsWith(currentArg));
                        }
                        case "list", "clear" -> {
                            return Stream.empty();
                        }
                    }
                }

                return Stream.empty();
            }

            case "help" -> Stream.empty();
        }

        return Stream.empty();
    }

    public String getPrefix() {
        return prefix;
    }

    public List<Command> getCommands() {
        return commands;
    }
}