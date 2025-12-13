package dev.luxury.utils.managers;

import com.google.common.collect.ImmutableList;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.List;

public class SyncManager {
    @Getter
    public static SyncManager instance;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static volatile List<Entity> cachedEntities = Collections.emptyList();
    private static volatile List<AbstractClientPlayerEntity> cachedPlayers = Collections.emptyList();
    private static volatile List<ItemStack> cachedItems = Collections.emptyList();

    private float visualBodyYaw, visualHeadYaw, visualHeadPitch;
    private float visualPrevBodyYaw, visualPrevHeadYaw, visualPrevHeadPitch;

    public float tps = 20;
    private long lastTime = System.nanoTime();
    private final float NANOS_IN_SECOND = 1_000_000_000F;

    public static List<Entity> getEntities() {
        return cachedEntities;
    }

    public static List<ItemStack> getItems() {
        return cachedItems;
    }

    public static List<AbstractClientPlayerEntity> getPlayers() {
        return cachedPlayers;
    }

    public float getBodyYaw() {
        var player = mc.player;
        if (player == null) return visualBodyYaw;

        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;

        float offset = visualBodyYaw;
        if ((dx * dx + dz * dz) > 0.0025f) {
            offset = (float) (MathHelper.atan2(dz, dx) * (180f / Math.PI) - 90.0f);
        }

        float lastYaw = player.getYaw();

        if (player.handSwingProgress > 0.0f) {
            offset = lastYaw;
        }

        float deltaBodyYaw = MathHelper.clamp(MathHelper.wrapDegrees(lastYaw - (visualBodyYaw + MathHelper.wrapDegrees(offset - visualBodyYaw) * 0.3f)), -45.0f, 75.0f);

        return (deltaBodyYaw > 50f ? deltaBodyYaw * 0.2f : 0) + lastYaw - deltaBodyYaw;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null || mc.player == null) return;

        cachedEntities = ImmutableList.copyOf(mc.world.getEntities());
        cachedPlayers = ImmutableList.copyOf(mc.world.getPlayers());
        cachedItems = ImmutableList.copyOf(mc.player.getArmorItems());
    }

}