package dev.luxury.modules.impl;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;

import dev.luxury.utils.discord.utils.DiscordEventHandlers;
import dev.luxury.utils.discord.utils.DiscordRichPresence;
import dev.luxury.utils.discord.utils.RPCButton;
import lombok.Getter;
import lombok.experimental.NonFinal;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "DiscordRPC",
        desc = "",
        category = Category.Misc
)
@Getter
public class DiscordRPC extends Module {
    private DiscordDaemonThread discordDaemonThread;
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


                    String image = "https://s14.gifyu.com/images/bwwVF.gif";



                    DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                            .setStartTimestamp((System.currentTimeMillis() / 1000))
                            .setDetails("User: Developer")
                            .setState("Uid: 1337")
                            .setLargeImage(image)
                            .setButtons(
                                    RPCButton.create("Дискорд", "https://discord.gg/ypp22E3r4t"),  RPCButton.create("Телеграм", "https://t.me/luxuryclientbestdlc")
                            ).build();

                    dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
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
    }

    public boolean isRunning() {
        return running;
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (DiscordRPC.this.isRunning()) {
                    dev.luxury.utils.discord.utils.DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15 * 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                stopRPC();
            }
        }
    }
}