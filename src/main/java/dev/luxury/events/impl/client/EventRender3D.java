package dev.luxury.events.impl.client;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

@Getter
public class EventRender3D implements Event {
    MinecraftClient mc = MinecraftClient.getInstance();
    private MatrixStack matrices;
    private float partialTicks;
    private DrawContext context;

    public EventRender3D(MatrixStack matrices,float partialTicks){
        this.matrices = matrices;
        this.partialTicks = partialTicks;
        this.context = new DrawContext(mc,mc.getBufferBuilders().getEntityVertexConsumers());
    }
}
