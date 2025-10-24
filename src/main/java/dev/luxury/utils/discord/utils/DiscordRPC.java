package dev.luxury.utils.discord.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiscordRPC extends Library {
    DiscordRPC INSTANCE = load();

    static DiscordRPC load() {
        String libName = getLibraryNameForOS();
        return Native.load(libName, DiscordRPC.class);
    }

    static String getLibraryNameForOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "discord-rpc"; 
        } else if (osName.contains("linux")) {
            return "discord-rpc";
        } else if (osName.contains("mac")) {
            return "discord-rpc";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }

    void Discord_UpdateHandlers(DiscordEventHandlers var1);
    void Discord_UpdatePresence(DiscordRichPresence var1);
    void Discord_Respond(String var1, int var2);
    void Discord_Register(String var1, String var2);
    void Discord_Shutdown();
    void Discord_UpdateConnection();
    void Discord_RegisterSteamGame(String var1, String var2);
    void Discord_RunCallbacks();
    void Discord_Initialize(String var1, DiscordEventHandlers var2, boolean var3, String var4);
    void Discord_ClearPresence();

    enum DiscordReply {
        NO(0), IGNORE(2), YES(1);
        public final int reply;
        DiscordReply(int reply) { this.reply = reply; }
    }
}
