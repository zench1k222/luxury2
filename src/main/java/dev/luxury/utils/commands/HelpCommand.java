package dev.luxury.utils.commands;

import dev.luxury.utils.client.ChatUtil;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Помощь по командам", ".help");
    }

    @Override
    public void execute(String[] args) {
        ChatUtil.sendChat("§aДоступные команды:");
        ChatUtil.sendChat("§7.help - Показать это сообщение");
        ChatUtil.sendChat("§7.cfg - Управление конфигами");
        ChatUtil.sendChat("§7.autobuy - Управление модулем AutoBuy(RW)");
        ChatUtil.sendChat("§7.friend - Управление списком друзей");
        ChatUtil.sendChat("§7.way - Управление метками");
        ChatUtil.sendChat("");
        ChatUtil.sendChat("§eИспользуйте .<команда> без аргументов для справки");
    }
}