package dev.luxury.modules.impl.other.hud.api;

import dev.luxury.utils.render.RenderUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.Color;

public abstract class DraggableHudElement {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter
    private final String name;

    @Getter
    protected float x, y, width, height;

    protected float newX = -1, newY = -1;

    public DraggableHudElement(String name, float x, float y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public void tick() {}

    public abstract void render(DrawContext context);

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void drawBorder(DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        RenderUtil.drawBorder(matrices, x, y, width, height, new Vector4f(4, 4, 4, 4), new Color(100, 100, 255, 200).getRGB(), 1f, 1, 1, false);
    }

    public void drag(float mouseX, float mouseY, float screenWidth, float screenHeight, HUD dragManager) {
        float newPosX = mouseX;
        float newPosY = mouseY;

        newPosX = Math.max(0, Math.min(newPosX, screenWidth - width));
        newPosY = Math.max(0, Math.min(newPosY, screenHeight - height));

        this.x = newPosX;
        this.y = newPosY;

        Vector2f nearest = dragManager.getNearest(newPosX, newPosY);
        SheetCode xCode = new SheetCode(nearest.x, 0);
        SheetCode yCode = new SheetCode(nearest.y, 0);

        Vector2f nearest2 = dragManager.getNearest(newPosX + width, newPosY + height);
        SheetCode xCode2 = new SheetCode(nearest2.x, -width);
        SheetCode yCode2 = new SheetCode(nearest2.y, -height);

        Vector2f nearest3 = dragManager.getNearest(newPosX + width / 2, newPosY + height / 2);
        SheetCode xCode3 = new SheetCode(nearest3.x, -width / 2);
        SheetCode yCode3 = new SheetCode(nearest3.y, -height / 2);

        SheetCode finalX = getBestCode(xCode, xCode2, xCode3);
        SheetCode finalY = getBestCode(yCode, yCode2, yCode3);

        if (finalX.pos != -1) this.newX = finalX.pos + finalX.offset;
        else this.newX = -1;

        if (finalY.pos != -1) this.newY = finalY.pos + finalY.offset;
        else this.newY = -1;
    }

    private SheetCode getBestCode(SheetCode code1, SheetCode code2, SheetCode code3) {
        if (code1.pos != -1) return code1;
        if (code2.pos != -1) return code2;
        return code3;
    }

    public void renderSnapLines(DrawContext context) {
        MatrixStack matrices = context.getMatrices();

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        if (newX != -1) {
            float lineX = newX;
            RenderUtil.drawRoundedRect(matrices, lineX, 0, 1, screenHeight,
                    new Vector4f(0, 0, 0, 0),
                    new Color(100, 100, 255, 150).getRGB());
        }

        if (newY != -1) {
            float lineY = newY;
            RenderUtil.drawRoundedRect(matrices, 0, lineY, screenWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    new Color(100, 100, 255, 150).getRGB());
        }
    }

    public void release() {
        if (newX != -1) this.x = newX;
        if (newY != -1) this.y = newY;

        newX = -1;
        newY = -1;
    }

    public void windowResized(float newWindowWidth, float newWindowHeight) {
        if (newWindowHeight <= 0 || newWindowWidth <= 0) return;

        if (this.x < 0) this.x = 0;
        if (this.y < 0) this.y = 0;

        if (this.x + width > newWindowWidth) this.x = newWindowWidth - width;
        if (this.y + height > newWindowHeight) this.y = newWindowHeight - height;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Getter
    protected static class SheetCode {
        private final float pos;
        private final float offset;

        public SheetCode(float pos, float offset) {
            this.pos = pos;
            this.offset = offset;
        }
    }
}