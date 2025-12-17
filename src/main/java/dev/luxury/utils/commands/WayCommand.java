package dev.luxury.utils.commands;

import dev.luxury.common.way.Way;
import dev.luxury.common.way.WayRepository;
import dev.luxury.utils.client.ChatUtil;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class WayCommand extends Command {

    private final WayRepository wayRepository = WayRepository.getInstance();

    public WayCommand() {
        super("way", "Управление метками", ".way <add/remove/list/clear> [имя] [x] [y] [z]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add" -> handleAdd(args);
            case "remove", "del", "delete" -> handleRemove(args);
            case "list" -> handleList();
            case "clear" -> handleClear();
            default -> sendUsage();
        }
    }

    private void handleAdd(String[] args) {
        if (args.length < 2) {
            ChatUtil.sendChat("§cИспользование: .way add <имя> [x] [y] [z]");
            ChatUtil.sendChat("§7Если x, y, z не указаны - используется текущая позиция игрока");
            return;
        }

        try {
            String name = args[1];

            if (wayRepository.hasWay(name)) {
                ChatUtil.sendChat("§cМетка с именем '" + name + "' уже существует!");
                return;
            }

            BlockPos pos;

            if (args.length >= 5) {
                int x = Integer.parseInt(args[2]);
                int y = Integer.parseInt(args[3]);
                int z = Integer.parseInt(args[4]);
                pos = new BlockPos(x, y, z);
            } else {
                if (mc.player == null) {
                    ChatUtil.sendChat("§cИгрок не найден!");
                    return;
                }
                pos = mc.player.getBlockPos();
            }

            String server = mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null
                    ? mc.getNetworkHandler().getServerInfo().address
                    : "vanilla";

            wayRepository.addWay(name, pos, server);
            ChatUtil.sendChat("§aМетка '" + name + "' добавлена на координаты (" +
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");

        } catch (NumberFormatException e) {
            ChatUtil.sendChat("§cКоординаты должны быть числами!");
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.sendChat("§cОшибка при добавлении метки: " + e.getMessage());
        }
    }

    private void handleRemove(String[] args) {
        if (args.length < 2) {
            ChatUtil.sendChat("§cИспользование: .way remove <имя>");
            return;
        }

        String name = args[1];

        if (wayRepository.hasWay(name)) {
            wayRepository.deleteWay(name);
            ChatUtil.sendChat("§aМетка '" + name + "' удалена!");
        } else {
            ChatUtil.sendChat("§cМетка '" + name + "' не найдена!");
        }
    }

    private void handleList() {
        List<Way> ways = wayRepository.wayList;

        if (ways.isEmpty()) {
            ChatUtil.sendChat("§eСписок меток пуст");
            return;
        }

        ChatUtil.sendChat("§aСписок меток §7(" + ways.size() + ")§7:");

        for (Way way : ways) {
            ChatUtil.sendChat("§7- §f" + way.name() +
                    " §7(" + way.pos().getX() + ", " + way.pos().getY() + ", " + way.pos().getZ() +
                    ") §8[" + way.server() + "]");
        }
    }

    private void handleClear() {
        if (wayRepository.isEmpty()) {
            ChatUtil.sendChat("§eСписок меток уже пуст");
            return;
        }

        wayRepository.clearList();
        ChatUtil.sendChat("§aВсе метки удалены!");
    }

    private void sendUsage() {
        ChatUtil.sendChat("§cИспользование команды way:");
        ChatUtil.sendChat("§7.way add <имя> [x] [y] [z] - Добавить метку");
        ChatUtil.sendChat("§7.way remove <имя> - Удалить метку");
        ChatUtil.sendChat("§7.way list - Показать все метки");
        ChatUtil.sendChat("§7.way clear - Очистить все метки");
        ChatUtil.sendChat("");
        ChatUtil.sendChat("§eПримеры:");
        ChatUtil.sendChat("§7.way add home - Добавить метку 'home' на текущей позиции");
        ChatUtil.sendChat("§7.way add spawn 100 64 -200 - Добавить метку с координатами");
        ChatUtil.sendChat("§7.way remove home - Удалить метку 'home'");
    }
}