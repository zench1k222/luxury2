package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.network.NetWorkUtils;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleAnnotation(
        name = "Noclip",
        desc = "",
        category = Category.Movement,
        key = GLFW.GLFW_KEY_X
)
public class NoClip extends Module {
    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;
    private int tickCounter = 0;

    @EventTarget
    public void onEvent(EventTick event) {
        if (mc.player == null || mc.world == null) {
            toggle();
        }
    }
@EventTarget
        public void packet (PacketEvent ep) {
            if (ep.isSend()) {
                Packet<?> p = ep.getPacket();

                if (shouldPhase() && !(p instanceof KeepAliveC2SPacket) && !(p instanceof CommonPongC2SPacket)) {
                    packets.add(p);
                    ep.setCancelled(true);
                }
            }

            if (ep.isReceive() && ep.getPacket() instanceof PlayerPositionLookS2CPacket) {
                resumePackets();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        mc.player.isOnGround(),
                        false
                ));
            }
        }
@EventTarget
        public void onTick ( EventTick e) {
            tickCounter++;
            if (tickCounter >= 15
    ) {
                resumePackets();
                tickCounter = 0;
            }

            if (shouldPhase()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        mc.player.isOnGround(),
                        false
                ));
            }
            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        }
@EventTarget
       public void onRender3D(EventRender3D e) {
            if (box != null) {
                RenderUtil3D.drawBoxOutlines(box,e.getMatrices(), Color.white);
            }
        }


    private boolean shouldPhase() {
        if (mc.player == null || mc.world == null) return false;

        Box hitbox = mc.player.getBoundingBox();
        BlockPos min = new BlockPos((int) Math.floor(hitbox.minX), (int) Math.floor(hitbox.minY), (int) Math.floor(hitbox.minZ));
        BlockPos max = new BlockPos((int) Math.floor(hitbox.maxX), (int) Math.floor(hitbox.maxY), (int) Math.floor(hitbox.maxZ));

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isAir() && mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).getBoundingBoxes().stream().anyMatch(box -> box.intersects(hitbox.offset(-pos.getX(), -pos.getY(), -pos.getZ())))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void resumePackets() {
        if (mc.player == null || mc.world == null) {
            toggle();
        }
        if (!packets.isEmpty()) {
            for (Packet<?> packet : new ArrayList<>(packets)) {
                NetWorkUtils.sendSilentPacket(packet);
            }
            packets.clear();
            box = mc.player.getBoundingBox();
        }
    }


    @Override
    public void onEnable() {

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        packets.clear();
        tickCounter = 0;
    }
}