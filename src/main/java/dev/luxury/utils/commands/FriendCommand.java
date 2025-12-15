package dev.luxury.utils.commands;


import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.text.Text;


public class FriendCommand extends Command {

    public FriendCommand() {
        super("friend", "Управление друзьями", ".friend <add/del/list> [имя]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cИспользование: .friend <add/del/list>");
            return;
        }

        FriendManager friendManager = FriendManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .friend add <имя>");
                    return;
                }
                String nameToAdd = args[1];
                if (friendManager.addFriend(nameToAdd)) {
                    ChatUtil.sendChat("§aДруг §f" + nameToAdd + " §aдобавлен!");
                } else {
                    ChatUtil.sendChat("§c" + nameToAdd + " уже в друзьях!");
                }
            }
            case "delete", "remove", "del" -> {
                if (args.length < 2) {
                    ChatUtil.sendChat("§cИспользование: .friend del <имя>");
                    return;
                }
                String nameToRemove = args[1];
                if (friendManager.removeFriend(nameToRemove)) {
                    ChatUtil.sendChat("§cДруг §f" + nameToRemove + " §cудален!");
                } else {
                    ChatUtil.sendChat("§c" + nameToRemove + " не найден в друзьях!");
                }
            }
            case "list" -> {
                if (friendManager.getFriends().isEmpty()) {
                    ChatUtil.sendChat("§eСписок друзей пуст");
                    return;
                }
                ChatUtil.sendChat("§aДрузья §7(" + friendManager.getFriends().size() + ")§7:");
                for (String friend : friendManager.getFriends()) {
                    ChatUtil.sendChat("§7- §f" + friend);
                }
            }
            case "clear" -> {
                friendManager.clear();
                ChatUtil.sendChat("§cВсе друзья удалены!");
            }
            default -> ChatUtil.sendChat("§cНеизвестная подкоманда! Используйте: add, delete, list, clear");
        }
    }
}