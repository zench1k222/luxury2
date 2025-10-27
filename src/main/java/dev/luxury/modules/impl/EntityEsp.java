package dev.luxury.modules.impl;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.math.ProjectionUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector4d;
import org.joml.Vector4f;

@ModuleAnnotation(
        name = "EntityEsp",
        desc = "Отображает ESP для игроков",
        category = Category.Render
)
public class EntityEsp extends Module {

    @EventTarget
    public void render(EventRender2D e) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = e.getDrawContext().getMatrices();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            Vector4d vec4d = ProjectionUtil.getVector4D(player);

            if (ProjectionUtil.cantSee(vec4d)) continue;

            float posX = (float) vec4d.x;
            float posY = (float) vec4d.y;
            float endPosX = (float) vec4d.z;
            float endPosY = (float) vec4d.w;

            float width = endPosX - posX;
            float height = endPosY - posY;
            float padding = 2f;

drawFlatBox(vec4d,e);
        }
    }
    private void drawFlatBox(Vector4d vec,EventRender2D e) {
        int black = 0xA10a0a0a;

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;

        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 1F, posY - 1,size + 1,1.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 1F, posY + 0.5F,1.5F,size + 0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 1F, endPosY - size - 1,1.5F,size,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 1F, endPosY - 1,size + 1,1.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX - size + 0.5F, posY - 1,size + 1,1.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX, posY + 0.5F,1.5F,size + 0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX, endPosY - size - 1,1.5F,size,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX - size + 0.5F, endPosY - 1,size + 1,1.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 0.5F, posY - 0.5F,size,0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 0.5F, posY,0.5F,size + 0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 0.5F, endPosY - size - 0.5F,0.5F,size,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),posX - 0.5F, endPosY - 0.5F,size,0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX - size + 1, posY - 0.5F,size,0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX + 0.5F, posY,0.5F,size + 0.5F,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX + 0.5F, endPosY - size - 0.5F,0.5F,size,new Vector4f(0,0,0,0),black);
        RenderUtil.drawRoundedRect(e.getDrawContext().getMatrices(),endPosX - size + 1, endPosY - 0.5F,size,0.5F,new Vector4f(0,0,0,0),black);
    }
    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}