package dev.luxury.utils.managers;

import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.commands.*;
import dev.luxury.modules.api.ModuleManager;
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
        registerCommand(new WayCommand());

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
                        ChatUtil.sendError("Ошибка выполнения команды: " + e.getMessage());
                    }
                    e.printStackTrace();
                }
                return true;
            }
        }

        if (mc.player != null) {
            ChatUtil.sendError("Команда не найдена: " + commandName);
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

            case "way" -> {
                if (parts.length == 2) {
                    return Stream.of("add", "remove", "delete", "del", "list", "clear")
                            .filter(a -> a.startsWith(currentArg))
                            .map(s -> prefix + cmdName + " " + s);
                }

                if (parts.length == 3) {
                    String subCommand = parts[1].toLowerCase();
                    switch (subCommand) {
                        case "add" -> {
                            if (currentArg.isEmpty()) {
                                return Stream.of(prefix + cmdName + " " + subCommand + " ");
                            }
                        }
                        case "remove", "delete", "del" -> {
                            List<String> ways = dev.luxury.common.way.WayRepository.getInstance().wayList
                                    .stream()
                                    .map(way -> way.name())
                                    .toList();
                            return ways.stream()
                                    .filter(w -> w.startsWith(currentArg))
                                    .map(s -> prefix + cmdName + " " + subCommand + " " + s);
                        }
                    }
                }

                if (parts.length == 4 && "add".equals(parts[1].toLowerCase())) {
                    return Stream.of(prefix + cmdName + " " + parts[1] + " " + parts[2] + " ")
                            .filter(a -> a.startsWith(message));
                }

                if (parts.length == 5 && "add".equals(parts[1].toLowerCase())) {
                    return Stream.of(prefix + cmdName + " " + parts[1] + " " + parts[2] + " " + parts[3] + " ")
                            .filter(a -> a.startsWith(message));
                }

                if (parts.length == 6 && "add".equals(parts[1].toLowerCase())) {
                    return Stream.of(prefix + cmdName + " " + parts[1] + " " + parts[2] + " " + parts[3] + " " + parts[4] + " ")
                            .filter(a -> a.startsWith(message));
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