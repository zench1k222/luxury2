package dev.luxury.utils.commands;


import dev.luxury.utils.managers.FriendManager;
import net.minecraft.text.Text;


public class FriendCommand extends Command {

    public FriendCommand() {
        super("friend", "Управление друзьями", ".friend <add/del/list> [имя]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendMessage("§cИспользование: .friend <add/delete/list> [имя]");
            return;
        }

        FriendManager friendManager = FriendManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    sendMessage("§cИспользование: .friend add <имя>");
                    return;
                }
                String nameToAdd = args[1];
                if (friendManager.addFriend(nameToAdd)) {
                    sendMessage("§aДруг §f" + nameToAdd + " §aдобавлен!", "Friend Manager");
                } else {
                    sendMessage("§c" + nameToAdd + " уже в друзьях!", "Friend Manager");
                }
            }
            case "delete", "remove", "del" -> {
                if (args.length < 2) {
                    sendMessage("§cИспользование: .friend del <имя>");
                    return;
                }
                String nameToRemove = args[1];
                if (friendManager.removeFriend(nameToRemove)) {
                    sendMessage("§cДруг §f" + nameToRemove + " §cудален!", "Friend Manager");
                } else {
                    sendMessage("§c" + nameToRemove + " не найден в друзьях!", "Friend Manager");
                }
            }
            case "list" -> {
                if (friendManager.getFriends().isEmpty()) {
                    sendMessage("§eСписок друзей пуст", "Friend Manager");
                    return;
                }
                sendMessage("§aДрузья §7(" + friendManager.getFriends().size() + ")§7:", "Friend Manager");
                for (String friend : friendManager.getFriends()) {
                    sendMessage("§7- §f" + friend, "Friend Manager");
                }
            }
            case "clear" -> {
                friendManager.clear();
                sendMessage("§cВсе друзья удалены!", "Friend Manager");
            }
            default -> sendMessage("§cНеизвестная подкоманда! Используйте: add, delete, list, clear");
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