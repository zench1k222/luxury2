package dev.luxury.utils.network;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NetWorkUtils {


        private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean sendingSilent = false;
    public static void sendSilentPacket(Packet<?> packet) {
        try {
            sendingSilent = true;
            mc.player.networkHandler.sendPacket(packet);
        } finally {
            sendingSilent = false;
        }
    }

    public void sendPacket(Packet<?> packet) {
        mc.player.networkHandler.sendPacket(packet);
    }

    public static boolean isSendingSilent() {
        return sendingSilent;
    }
}


