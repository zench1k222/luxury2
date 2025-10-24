package dev.luxury.events.impl.render;



import dev.luxury.events.impl.eventapi.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;


@SuppressWarnings("All")
public class EventRender2D  implements Event {
    private static DrawContext drawContext;
    private static MatrixStack matrixStack;
    private static RenderTickCounter deltatick;

    public EventRender2D(DrawContext drawContext, MatrixStack matrixStack, RenderTickCounter deltatick) {
        this.drawContext = drawContext;
        this.matrixStack = matrixStack;
        this.deltatick = deltatick;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public RenderTickCounter getDeltatick() {
        return deltatick;
    }
}
