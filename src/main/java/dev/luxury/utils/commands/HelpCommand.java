package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;
import net.minecraft.text.Text;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Помощь по командам", ".help");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.sendChat("§cДоступные команды:");
            ChatUtil.sendChat("§c.cfg - Управление конфигами");
            ChatUtil.sendChat("§c.friend - Управление списком друзей");
        }
    }
}
