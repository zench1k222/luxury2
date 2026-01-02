package dev.luxury.modules.impl.other.taksa;

import dev.luxury.events.impl.eventapi.events.Event;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.utils.math.TimerUtils;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@UtilityClass
public class TaksaScheduler {
    
    @Getter
    private final TimerUtils updateTimer = new TimerUtils();
    
    public void onEvent(Event event) {
        if (event instanceof EventTick) {
            if (updateTimer.passed(60_000)) {
                TaksaAI.update();
            }
        }
        
        if (event instanceof PacketEvent e && e.isReceive()) {
            if (e.getPacket() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();
                if (message != null && !message.isEmpty()) {
                    TaksaAI.setLastMsg(message);
                    TaksaAI.update();
                }
            }
        }
    }
}

