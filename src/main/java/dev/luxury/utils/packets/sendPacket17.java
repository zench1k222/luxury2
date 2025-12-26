package dev.luxury.utils.packets;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.protocol.Protocol;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import static dev.luxury.modules.impl.targetesp.mode.Circle.mc;

public class sendPacket17 {

    public void sendRWCritPackets() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        send1_17_1PositionPacket(x, y + 0.0625, z, false);
        send1_17_1PositionPacket(x, y + 0.1, z, false);
        send1_17_1PositionPacket(x, y + 0.15, z, false);
        send1_17_1PositionPacket(x, y + 0.1, z, false);
        send1_17_1PositionPacket(x, y + 0.0625, z, false);
        send1_17_1PositionPacket(x, y, z, false);
    }

    public void send1_17_1PositionPacket(double x, double y, double z, boolean onGround) {
        UserConnection user = Via.getManager().getConnectionManager()
                .getConnectedClient(mc.player.getUuid());

        if (user == null) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false)
            );
            return;
        }

        try {
            PacketWrapper wrapper = PacketWrapper.create(0x14, null, user);

            wrapper.write(Types.DOUBLE, x);
            wrapper.write(Types.DOUBLE, y);
            wrapper.write(Types.DOUBLE, z);
            wrapper.write(Types.BOOLEAN, onGround);

            wrapper.sendToServer(Protocol.class);

        } catch (Exception e) {
            e.printStackTrace();
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false)
            );
        }
    }
}