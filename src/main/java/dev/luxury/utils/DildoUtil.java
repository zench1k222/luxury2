package dev.luxury.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public class DildoUtil{
    private static MinecraftClient mc = MinecraftClient.getInstance();
    public static void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null);
    }
}
