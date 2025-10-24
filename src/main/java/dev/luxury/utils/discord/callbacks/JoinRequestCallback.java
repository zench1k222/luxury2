package dev.luxury.utils.discord.callbacks;

import com.sun.jna.Callback;
import dev.luxury.utils.discord.utils.DiscordUser;


public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser var1);
}