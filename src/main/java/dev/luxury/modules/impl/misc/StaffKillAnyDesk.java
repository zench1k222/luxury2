package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleAnnotation(
        name = "StaffKillAnyDesk",
        desc = "Обнаруживает подключение через AnyDesk и отправляет IP:порт админа",
        category = Category.Misc
)
public class StaffKillAnyDesk extends Module {

    private static final String LOG_PATH_USER = System.getProperty("user.home") + "\\AppData\\Roaming\\AnyDesk\\ad.trace";
    private static final String LOG_PATH_PROG = "C:\\ProgramData\\AnyDesk\\ad_svc.trace";

    private final Pattern LOGGED_IN_PATTERN = Pattern.compile("Logged in from (\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)");

    private File logFile;
    private long lastFileSize = 0;
    private String lastDetected = null;

    @Override
    public void onEnable() {
        super.onEnable();

        File userLog = new File(LOG_PATH_USER);
        File progLog = new File(LOG_PATH_PROG);

        if (userLog.exists()) {
            logFile = userLog;
        } else if (progLog.exists()) {
            logFile = progLog;
        } else {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§c[StaffKillAnyDesk] Лог-файл AnyDesk не найден! Убедитесь, что AnyDesk установлен."), false);
            }
            toggle();
            return;
        }

        lastFileSize = logFile.length();
        lastDetected = null;

        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§a[StaffKillAnyDesk] Мониторинг AnyDesk включён. Лог: " + logFile.getAbsolutePath()), false);
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || logFile == null || !logFile.exists()) return;

        long currentSize = logFile.length();

        if (currentSize > lastFileSize) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                reader.skip(lastFileSize);

                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = LOGGED_IN_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String ip = matcher.group(1);
                        String port = matcher.group(2);
                        String info = ip + ":" + port;

                        if (!info.equals(lastDetected)) {
                            lastDetected = info;

                            Text message = Text.literal("§c§l[STAFF DETECTED] AnyDesk подключение от: §f" + info + " ").append(Text.literal("§7[§aКопировать§7]").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, info)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("§aНажмите, чтобы скопировать IP:порт в буфер обмена")))));

                            mc.player.sendMessage(message, false);

                        }
                    }
                }

                lastFileSize = currentSize;

            } catch (IOException ex) {
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        logFile = null;
        lastFileSize = 0;
        lastDetected = null;
    }
}