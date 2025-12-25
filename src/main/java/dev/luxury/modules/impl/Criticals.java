package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

@ModuleAnnotation(
        name = "Criticals",
        desc = "Позволяет бить критами в паутине или с эффектом плавного падения",
        category = Category.Combat
)
public class Criticals extends Module {

    public static boolean cancelCrit = false;

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isGliding() || mc.player.hasVehicle()) {
            return;
        }

        if (e.isSend() && e.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            if (packet.type.getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {

                if (!canUseCriticals()) {
                    return;
                }

                handleRW();
            }
        }
    }

    protected boolean canUseCriticals() {
        return hasSlowFalling() || isInCobweb();
    }

    protected boolean hasSlowFalling() {
        return mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
    }

    protected boolean isInCobweb() {
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBlockState(pos).isOf(Blocks.COBWEB);
    }

    protected void handleRW() {
        if (cancelCrit) return;

        boolean inCobweb = isInCobweb();
        boolean hasSlowFall = hasSlowFalling();

        if (!inCobweb && !hasSlowFall) {
            return;
        }

        if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            mc.player.fallDistance = 0.08f;

            sendPositionPacket(x, y + 0.035, z, yaw, pitch, false);
            sendPositionPacket(x, y, z, yaw, pitch, false);
            sendPositionPacket(x, y + 0.011, z, yaw, pitch, false);
            sendPositionPacket(x, y, z, yaw, pitch, false);
        }
    }

    protected void handleDefaultCrit() {
        if (cancelCrit) return;
        if (!mc.player.isOnGround() && !mc.player.isGliding()) {
            sendCritPacket(-1.0E-6, false);
        }
    }

    protected void handleRWCrit() {
        if (canUseCriticals()) {
            sendRWCritPackets();
        }
    }

    protected void sendCritPacket(double yDelta, boolean positionOnly) {
        double x = mc.player.getX();
        double y = mc.player.getY() + yDelta;
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        if (positionOnly) {
            sendSequencedPacket(world -> new PlayerMoveC2SPacket.PositionAndOnGround(
                    x, y, z, false, mc.player.horizontalCollision
            ));
        } else {
            sendSequencedPacket(world -> new PlayerMoveC2SPacket.Full(
                    x, y, z, yaw, pitch, false, mc.player.horizontalCollision
            ));
        }
    }

    protected void sendRWCritPackets() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        sendPositionOnlyPacket(x, y + 0.0625, z, false);
        sendPositionOnlyPacket(x, y + 0.1, z, false);
        sendPositionOnlyPacket(x, y + 0.15, z, false);
        sendPositionOnlyPacket(x, y + 0.1, z, false);
        sendPositionOnlyPacket(x, y + 0.0625, z, false);
        sendPositionOnlyPacket(x, y, z, false);
    }

    protected void sendPositionPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        sendSequencedPacket(world -> new PlayerMoveC2SPacket.Full(
                x, y, z, yaw, pitch, onGround, mc.player.horizontalCollision
        ));
    }

    protected void sendPositionOnlyPacket(double x, double y, double z, boolean onGround) {
        sendSequencedPacket(world -> new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, onGround, mc.player.horizontalCollision
        ));
    }

    protected void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.interactionManager != null && mc.world != null) {
            mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
        }
    }

    protected void handleTestCrit() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cancelCrit = false;
    }
}