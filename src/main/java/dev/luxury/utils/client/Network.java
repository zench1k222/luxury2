package dev.luxury.utils.client;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@UtilityClass
public class Network {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter
    public String server = "Vanilla";
    @Getter
    public float TPS = 20;
    @Getter
    public long timestamp;

    public void tick() {
        server = getServer();
    }

    public void onPacketReceive(net.minecraft.network.packet.Packet<?> packet) {
        if (packet instanceof WorldTimeUpdateS2CPacket) {
            long nanoTime = System.nanoTime();
            float maxTPS = 20;
            float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));
            TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
            timestamp = nanoTime;
        }
    }

    public String getServer() {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null ||
                mc.getNetworkHandler().getBrand() == null) return "Vanilla";

        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") ||
                serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        else if (serverIp.contains("gulpvp")) return "GulPvP";
        return "Vanilla";
    }

    public boolean isCopyTime() { return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime"); }
    public boolean isFunTime() { return server.equals("FunTime"); }
    public boolean isReallyWorld() { return server.equals("ReallyWorld"); }
    public boolean isGulPvP() { return server.equals("GulPvP"); }
    public boolean isHolyWorld() { return server.equals("HolyWorld"); }
    public boolean isSpookyTime() { return server.equals("SpookyTime"); }
    public boolean isVanilla() { return server.equals("Vanilla"); }
}