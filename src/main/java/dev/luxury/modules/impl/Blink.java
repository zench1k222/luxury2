package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.eventapi.events.Event;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.network.NetWorkUtils;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleAnnotation(name = "Blink", desc = "Задерживает пакеты отправленные на сервер", category = Category.Movement,key = GLFW.GLFW_KEY_B)
public class Blink extends Module {

    float maxTicks = 15;
    private final CopyOnWriteArrayList<Packet<?>> packetBuffer = new CopyOnWriteArrayList<>();

    private Box playerBoundingBox;
    private int currentTick = 0;


    @EventTarget
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
    }

    @EventTarget
    public void packet(PacketEvent packetEvent) {
        Packet<?> packet = packetEvent.getPacket();
        if (packetEvent.isSend() && !(packet instanceof KeepAliveC2SPacket)) {
            packetBuffer.add(packet);
            packetEvent.setCancelled(true);
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        currentTick++;
        if (currentTick >= maxTicks) {
            send();
            currentTick = 0;
        }
    }

    @EventTarget
    public void onRender(EventRender3D event) {
        if (playerBoundingBox != null) {
            RenderUtil3D.drawBoxOutlines(playerBoundingBox, event.getMatrices(), new Color(0xFFFFFF));
        }
    }


    private void send() {
        if (mc.player == null || mc.world == null || packetBuffer.isEmpty()) return;
        for (Packet<?> packet : packetBuffer) {
            NetWorkUtils.sendSilentPacket(packet);
        }
        packetBuffer.clear();
        playerBoundingBox = mc.player.getBoundingBox();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        send();
        playerBoundingBox = null;
        currentTick = 0;
    }
}