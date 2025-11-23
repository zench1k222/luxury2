package dev.luxury.events.impl.render;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

@Getter
@AllArgsConstructor
public class EventRenderWorldEntities implements Event {
    private MatrixStack matrix;
    private VertexConsumerProvider vertex;
    private float partialTicks;
}

