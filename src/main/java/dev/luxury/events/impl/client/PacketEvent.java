package dev.luxury.events.impl.client;


import dev.luxury.events.impl.eventapi.events.callables.EventCancellable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;

@Getter @Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PacketEvent extends EventCancellable {
    Packet<?> packet;
    Type type;
    public boolean isSend() {
        return type.equals(Type.SEND);
    }
    public boolean isReceive(){
        return type.equals(Type.RECEIVE);
    }

    public enum Type {
        SEND, RECEIVE
    }
}
