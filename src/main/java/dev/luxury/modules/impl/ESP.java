package dev.luxury.modules.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

@ModuleAnnotation(
        name = "EntityEsp",
        desc = "Красивые квадраты на игроках",
        category = Category.Render
)
public class ESP extends Module {

    private final ModeListSetting targets = new ModeListSetting(
            "Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Меня", false),
            new BooleanSetting("Предметы", false)
    );

    private final BooleanSetting corners = new BooleanSetting("Углы", false);
    private final SliderSetting cornerLength = new SliderSetting("Высота", 0.3f, 0.1f, 0.4f, 0.1f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.9f, 0.5f, 2.0f, 0.1f);
    private final BooleanSetting showNameTags = new BooleanSetting("Неймтеги",true);
    private ArrayList<Entity> toRender = new ArrayList<>();

    public ESP() {
        addSettings(targets, corners, cornerLength, thickness,showNameTags);
    }

    @EventTarget
    public void onGameTick(EventTick event) {
        if (!isEnabled()) return;

        toRender.clear();

        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ClientPlayerEntity && mc.options.getPerspective() == Perspective.FIRST_PERSON)
                continue;
            if (entity.isRemoved())
                continue;

            if (shouldRenderEntity(entity)) {
                toRender.add(entity);
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!isEnabled()) return;
        if (mc.options.hudHidden) return;

        Matrix4f matrix = e.getDrawContext().getMatrices().peek().getPositionMatrix();

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Iterator<Entity> iterator = toRender.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();

            // Интерполяция позиции как в Leet
            Vec3d interp = entity.getLerpedPos(mc.getRenderTickCounter().getTickDelta(true));
            Box box = entity.getBoundingBox().offset(interp.subtract(entity.getPos()));

            // Получаем 8 углов бокса
            Vec3d[] corners = new Vec3d[]{
                    new Vec3d(box.minX, box.minY, box.minZ),
                    new Vec3d(box.minX, box.minY, box.maxZ),
                    new Vec3d(box.maxX, box.minY, box.minZ),
                    new Vec3d(box.maxX, box.minY, box.maxZ),
                    new Vec3d(box.minX, box.maxY, box.minZ),
                    new Vec3d(box.minX, box.maxY, box.maxZ),
                    new Vec3d(box.maxX, box.maxY, box.minZ),
                    new Vec3d(box.maxX, box.maxY, box.maxZ)
            };

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            boolean anyVisible = false;

            // Проецируем все углы на экран
            for (Vec3d corner : corners) {
                Vec3d projected = RenderUtil.render3D.worldSpaceToScreenSpace(corner);
                if (projected.z <= 0 || projected.z >= 1) continue;

                anyVisible = true;

                minX = (float) Math.min(minX, projected.x);
                minY = (float) Math.min(minY, projected.y);
                maxX = (float) Math.max(maxX, projected.x);
                maxY = (float) Math.max(maxY, projected.y);
            }

            if (!anyVisible) continue;

            // Проверка видимости на экране
            if (maxX < 0 || maxY < 0 || minX > mc.getWindow().getScaledWidth() || minY > mc.getWindow().getScaledHeight())
                continue;
            if (showNameTags.get()) {
                renderNameTag(e, minX, minY, maxX, maxY, entity);
            }
            renderBox(e.getMatrixStack(), minX, minY, maxX, maxY, entity);
            // Рендерим неймтег если включен

        }

        RenderUtil.render3D.endBuilding(buffer);
        RenderUtil.disableRender();
    }

    private boolean shouldRenderEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (entity == mc.player) {
                return getTargetSetting("Меня");
            }
            if (getTargetSetting("Друзей") && FriendManager.getInstance().isFriend(player.getName().getString())) {
                return true;
            }
            return getTargetSetting("Игроков");
        }

        if (entity instanceof ItemEntity) {
            return getTargetSetting("Предметы");
        }

        return false;
    }

    private void renderBox(MatrixStack matrix, float minX, float minY, float maxX, float maxY, Entity entity) {
        float width = thickness.getFloatValue();
        float w = maxX - minX;
        float h = maxY - minY;

        int color1 = ColorUtil.getColorStyle(0);
        int color2 = ColorUtil.getColorStyle(90);

        Vector4f zero = new Vector4f(0, 0, 0, 0); // скругления нет

        if (!corners.get()) {
            // ==== FULL BOX ====

            // Top
            RenderUtil.drawRoundedRect(matrix, minX, minY, w, width, zero, color1);
            // Bottom
            RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w, width, zero, color2);

            // Left
            RenderUtil.drawRoundedRect(matrix, minX, minY, width, h, zero, color1);
            // Right
            RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h, zero, color2);

            return;
        }

        // ==== CORNERS MODE ====
        float cornerLen = cornerLength.getFloatValue();

        // TOP-LEFT
        RenderUtil.drawRoundedRect(matrix, minX, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, minX, minY, width, h * cornerLen, zero, color2);

        // TOP-RIGHT
        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h * cornerLen, zero, color2);

        // BOTTOM-LEFT
        RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, minX, maxY - h * cornerLen, width, h * cornerLen, zero, color1);

        // BOTTOM-RIGHT
        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, maxX - width, maxY - h * cornerLen, width, h * cornerLen, zero, color1);
    }


    private void drawGradientRect(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2,
                                  int c1, int c2, int c3, int c4) {
        buffer.vertex(matrix, x1, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y2, 0f).color(c2);
        buffer.vertex(matrix, x2, y1, 0f).color(c3);
        buffer.vertex(matrix, x1, y1, 0f).color(c4);
    }

    private boolean getTargetSetting(String name) {
        BooleanSetting setting = targets.getValueByName(name);
        return setting != null && setting.get();
    }

    private void renderNameTag(EventRender2D e, float minX, float minY, float maxX, float maxY, Entity entity) {
        Text text = entity.getCustomName() == null ? entity.getDisplayName() : entity.getCustomName();

        if (entity instanceof ItemEntity itemEntity) {
            text = itemEntity.getStack().getName();
        }

        String textString = text.getString();
        FontDraw font = FontHelper.sfprobold[15];
        if (font == null) return;

        MatrixStack matrixStack = e.getMatrixStack();

        // Вычисляем ширину текста
        float textWidth = font.getWidth(textString);
        float textWidth2 = textWidth + 6;
        float espWidth = (maxX - minX);

        // Цвет фона
        Color bgColor;
        if (entity instanceof PlayerEntity player && FriendManager.getInstance().isFriend(player.getName().getString())) {
            bgColor = new Color(0, 255, 0, 76); // Зелёный для друзей
        } else {
            bgColor = new Color(30, 30, 30, 150); // Обычный серый
        }

        float boxX = minX + espWidth / 2F - textWidth2 / 2F;
        float boxY = minY - 20; // Ещё выше

        // Рисуем фон с rounded rect
        RenderUtil.drawRoundedRect(matrixStack, boxX, boxY, textWidth2, 10, new Vector4f(2f, 2f, 2f, 2f), bgColor.getRGB());

        // Рисуем текст напрямую как в Leet
        float textX = minX + espWidth / 2F - 1 - textWidth / 2F;
        float textY = boxY + 1.5f;

        font.drawFontLeft(matrixStack, textString, textX + 1, textY - 1, -1);
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