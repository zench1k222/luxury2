package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.Macro;
import dev.luxury.utils.managers.MacrosManager;
import org.lwjgl.glfw.GLFW;

public class MacrosCommand extends Command {

    public MacrosCommand() {
        super("macros", "Управление макросами", ".macros <add/delete/list/clear> [название] [сообщение] [бинд]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cИспользование: .macros <add/delete/list/clear> [название] [сообщение] [бинд]");
            return;
        }

        MacrosManager macrosManager = MacrosManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 4) {
                    ChatUtil.sendChat("§cИспользование: .macros add <название> <сообщение> <бинд>");
                    ChatUtil.sendChat("§7Пример: .macros add hello Привет! H");
                    ChatUtil.sendChat("§7Для команд: .macros add hub /hub H");
                    return;
                }

                String name = args[1];

                StringBuilder messageBuilder = new StringBuilder();
                int lastArgIndex = args.length - 1;

                for (int i = 2; i < lastArgIndex; i++) {
                    if (i > 2) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }

                String message = messageBuilder.toString();

                String keyString = args[lastArgIndex].toUpperCase();
                int key = getKeyFromString(keyString);

                if (key == -1) {
                    ChatUtil.sendChat("§cНеверная клавиша: " + keyString);
                    return;
                }

                macrosManager.addMacro(name, message, key);

                ChatUtil.sendChat("§aМакрос §f" + name + " §aдобавлен!");
                ChatUtil.sendChat("§7Сообщение: §f" + message);
                ChatUtil.sendChat("§7Бинд: §f" + keyString);
            }

            case "delete", "del", "remove" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .macros delete <название>");
                    return;
                }

                String name = args[1];
                if (macrosManager.removeMacro(name)) {
                    ChatUtil.sendChat("§cМакрос §f" + name + " §cудален!");
                } else {
                    ChatUtil.sendChat("§cМакрос §f" + name + " §cне найден!");
                }
            }

            case "list" -> {
                if (macrosManager.getMacros().isEmpty()) {
                    ChatUtil.sendChat("§eНет сохраненных макросов");
                    return;
                }

                ChatUtil.sendChat("§aМакросы §7(" + macrosManager.getMacros().size() + ")§7:");
                for (Macro macro : macrosManager.getMacros()) {
                    String keyName = getKeyName(macro.getKey());
                    ChatUtil.sendChat("§7- §f" + macro.getName() + " §7[" + keyName + "] §8» §f" + macro.getMessage());
                }
            }

            case "clear" -> {
                macrosManager.clearMacros();
                ChatUtil.sendChat("§cВсе макросы удалены!");
            }

            default -> ChatUtil.sendChat("§cНеизвестная подкоманда! Используйте: add, delete, list, clear");
        }
    }

    private int getKeyFromString(String keyString) {
        if (keyString.length() == 1) {
            char c = keyString.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        return switch (keyString) {
            case "LSHIFT", "LEFTSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT", "RIGHTSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL", "LEFTCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL", "RIGHTCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT", "LEFTALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT", "RIGHTALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGEUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGEDOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "F1" -> GLFW.GLFW_KEY_F1;
            case "F2" -> GLFW.GLFW_KEY_F2;
            case "F3" -> GLFW.GLFW_KEY_F3;
            case "F4" -> GLFW.GLFW_KEY_F4;
            case "F5" -> GLFW.GLFW_KEY_F5;
            case "F6" -> GLFW.GLFW_KEY_F6;
            case "F7" -> GLFW.GLFW_KEY_F7;
            case "F8" -> GLFW.GLFW_KEY_F8;
            case "F9" -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            default -> -1;
        };
    }

    private String getKeyName(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        }

        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGEUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGEDOWN";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            default -> "UNKNOWN";
        };
    }
}