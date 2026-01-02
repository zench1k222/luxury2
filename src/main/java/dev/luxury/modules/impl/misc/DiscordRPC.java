package dev.luxury.modules.impl.misc;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.discord.utils.DiscordEventHandlers;
import dev.luxury.utils.discord.utils.DiscordRichPresence;
import dev.luxury.utils.discord.utils.RPCButton;
import lombok.Getter;

@ModuleAnnotation(
        name = "DiscordRPC",
        desc = "",
        category = Category.Misc
)
@Getter
public class DiscordRPC extends Module {
    private DiscordDaemonThread discordDaemonThread;
    public DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private boolean running = false;

    @Getter
    public static DiscordRPC instance;

    public DiscordRPC() {
        super();
        instance = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        discordDaemonThread = new DiscordDaemonThread();
        running = true;

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .ready((user) -> {
                    info = new DiscordInfo(user.username, user.avatar, user.userId);

                    String image = "https://s14.gifyu.com/images/bwwVF.gif";

                    DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                            .setStartTimestamp((System.currentTimeMillis() / 1000))
                            .setDetails("Role: " + getUserRole())
                            .setState("Ver: v0.8")
                            .setLargeImage(image)
                            .setButtons(
                                    RPCButton.create("Дискорд", "https://discord.gg/ypp22E3r4t"),
                                    RPCButton.create("Телеграм", "https://t.me/luxuryclientbestdlc")
                            ).build();

                    dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
                })
                .errored((errorCode, message) -> {
                    System.err.println("Discord RPC Error: " + errorCode + " - " + message);
                })
                .disconnected((errorCode, message) -> {
                    System.err.println("Discord RPC Disconnected: " + errorCode + " - " + message);
                })
                .build();

        String APPLICATION_ID = "1449108512599839001";
        dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_Initialize(APPLICATION_ID, handlers, true, "");
        discordDaemonThread.start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.stopRPC();

        if (discordDaemonThread != null && discordDaemonThread.isAlive()) {
            try {
                discordDaemonThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stopRPC() {
        dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_Shutdown();
        this.running = false;
        info = new DiscordInfo("Unknown", "", "");
    }

    public boolean isRunning() {
        return running;
    }

    public String getUserRole() {
        String userName = info.userName();
        if (userName == null || userName.equals("Unknown")) {
            return "User";
        }

        return "krasivih".equals(userName) || "_znchkx_".equals(userName) || "webimmortal".equals(userName)
                ? "Developer"
                : "User";
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (DiscordRPC.this.isRunning()) {
                    dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                System.err.println("Discord Daemon Thread Error: " + exception.getMessage());
                exception.printStackTrace();
                stopRPC();
            }
        }
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}