package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

@ModuleAnnotation(
        name = "PacketCriticals",
        desc = "Отправляет пакеты для получения критов",
        category = Category.Combat
)
public class PacketCriticals extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Default", new String[]{"Default", "RW"});
    public static boolean cancelCrit = false;

    public PacketCriticals() {
        addSettings(mode);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (e.isSend()) {
            if (e.getPacket() instanceof PlayerInteractEntityC2SPacket) {
                PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) e.getPacket();

                if (isAttackPacket(packet)) {
                    if (mode.get().equals("Default")) {
                        handleDefaultCrit();
                    } else if (mode.get().equals("RW")) {
                        handleRWCrit();
                    }
                }
            }
        }
    }

    private boolean isAttackPacket(PlayerInteractEntityC2SPacket packet) {
        try {
            var typeField = packet.getClass().getDeclaredField("type");
            typeField.setAccessible(true);
            var type = typeField.get(packet);
            return type != null && type.toString().contains("ATTACK");
        } catch (Exception e) {
            return false;
        }
    }

    private void handleDefaultCrit() {
        if (cancelCrit) return;

        if (!mc.player.isOnGround() && !mc.player.isGliding()) {
            sendDefaultCrit();
        }
    }

    private void handleRWCrit() {
        if (canCritRW()) {
            sendRWCritPackets();
        }
    }

    private void sendDefaultCrit() {
        critPacket(-1.0E-6, false);
    }

    private void critPacket(double yDelta, boolean full) {
        double x = mc.player.getX();
        double y = mc.player.getY() + yDelta;
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        if (full) {
            mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false)
            );
        } else {
            mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false)
            );
        }
    }

    private boolean canCritRW() {
        boolean hasSlowFalling = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
        boolean inCobweb = isInCobweb();
        return hasSlowFalling || inCobweb;
    }

    private boolean isInCobweb() {
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB;
    }

    private void sendRWCritPackets() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        sendPositionPacket(x, y + 0.0625, z);
        sendPositionPacket(x, y + 0.1, z);
        sendPositionPacket(x, y + 0.15, z);
        sendPositionPacket(x, y + 0.1, z);
        sendPositionPacket(x, y + 0.0625, z);
        sendPositionPacket(x, y, z);
    }

    private void sendPositionPacket(double x, double y, double z) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
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