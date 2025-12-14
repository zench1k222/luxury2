package dev.luxury.utils.commands;

import net.minecraft.text.Text;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Помощь по командам", ".help");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendMessage("§cДоступные команды:");
            sendMessage("§c.cfg - Управление конфигами");
            sendMessage("§c.friend - Управление списком друзей");
        }
    }
    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }
}
