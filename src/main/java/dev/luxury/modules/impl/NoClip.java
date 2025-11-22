package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.network.NetWorkUtils;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "Noclip",
        desc = "",
        category = Category.Movement
)
public class NoClip extends Module {
    private final List<Packet<?>> bufferedPackets = new ArrayList<>();

    public NoClip() {
    }

    @EventTarget
    private void onPacket(PacketEvent eventPacket) {
        if (mc.player == null || mc.player.networkHandler == null) return;

        Packet<?> packet = eventPacket.getPacket();

        if (packet instanceof PlayerMoveC2SPacket) {
            bufferedPackets.add(packet);
            eventPacket.setCancelled(true);
        }
    }

    @EventTarget
    private void onUpdate(EventTick eventUpdate) {
        if (mc.player == null || mc.world == null) return;

        mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
        mc.player.fallDistance = 0;

        Box box = mc.player.getBoundingBox().shrink(0.001, 0.001, 0.001);
        int totalStates = 0;
        int solidStates = 0;

        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    totalStates++;
                    if (state.isSolid()) {
                        solidStates++;
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (mc.player == null || mc.player.networkHandler == null) {
            bufferedPackets.clear();
            return;
        }

        if (!bufferedPackets.isEmpty()) {
            for (Packet<?> packet : bufferedPackets) {
                NetWorkUtils.sendSilentPacket(packet);
            }
            bufferedPackets.clear();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        bufferedPackets.clear();
    }
}