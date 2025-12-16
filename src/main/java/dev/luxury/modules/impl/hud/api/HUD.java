package dev.luxury.modules.impl.hud.api;

import com.google.gson.JsonObject;
import dev.luxury.events.impl.client.EventMouse;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.impl.hud.impl.KeyBinds;
import dev.luxury.modules.impl.hud.impl.Staffs;
import dev.luxury.modules.impl.hud.impl.TargetHud;
import dev.luxury.modules.impl.hud.impl.WaterMark;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleAnnotation(
        name = "HUD",
        desc = "Интерфейс чита",
        category = Category.Render
)
public class HUD extends Module {


    private final List<DraggableHudElement> draggableElements = new ArrayList<>();
    private DraggableHudElement draggingElement = null;
    private float dragOffsetX, dragOffsetY;

    private WaterMark draggableWaterMark;
    private TargetHud draggableTargetHud;
    private KeyBinds draggableKeyBinds;
    private Staffs draggableStaffs;

    private final ModeListSetting type = new ModeListSetting("Отображение",
            new BooleanSetting("WaterMark", true),
            new BooleanSetting("TargetHud", true),
            new BooleanSetting("Staffs", true),
            new BooleanSetting("KeyBinds", true)
    );

    public HUD() {
        addSettings(type);
        initDraggables();
    }

    private void initDraggables() {
        draggableWaterMark = new WaterMark("WaterMark", 5, 5);
        draggableTargetHud = new TargetHud("TargetHud", 400, 250);
        draggableKeyBinds = new KeyBinds("KeyBinds", 600, 50);
        draggableStaffs = new Staffs("Staffs", 600, 200);

        draggableElements.add(draggableWaterMark);
        draggableElements.add(draggableTargetHud);
        draggableElements.add(draggableKeyBinds);
        draggableElements.add(draggableStaffs);
    }


    @EventTarget
    public void onRender2D(EventRender2D e) {
        boolean editMode = mc.currentScreen instanceof ChatScreen;
        DrawContext dc = e.getDrawContext();

        if (editMode) {
            if (draggingElement != null) {
                double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
                double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

                float screenWidth = mc.getWindow().getScaledWidth();
                float screenHeight = mc.getWindow().getScaledHeight();

                draggingElement.drag((float) mouseX - dragOffsetX, (float) mouseY - dragOffsetY,
                        screenWidth, screenHeight, this);
            }

            for (DraggableHudElement element : draggableElements) {
                element.render(dc);

                if (element == draggingElement) {
                    element.drawBorder(dc);
                    element.renderSnapLines(dc);
                }
            }
        } else {
            if (draggingElement != null) {
                draggingElement.release();
                draggingElement = null;
            }

            BooleanSetting waterMarkSetting = type.getValueByName("WaterMark");
            if (waterMarkSetting != null && waterMarkSetting.isValue()) {
                draggableWaterMark.render(dc);
            }

            BooleanSetting targetHudSetting = type.getValueByName("TargetHud");
            if (targetHudSetting != null && targetHudSetting.isValue()) {
                draggableTargetHud.render(dc);
            }

            BooleanSetting keyBindsSetting = type.getValueByName("KeyBinds");
            if (keyBindsSetting != null && keyBindsSetting.isValue()) {
                draggableKeyBinds.render(dc);
            }

            BooleanSetting staffsSetting = type.getValueByName("Staffs");
            if (staffsSetting != null && staffsSetting.isValue()) {
                draggableStaffs.render(dc);
            }
        }
    }

    @EventTarget
    public void onMouse(EventMouse event) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            if (draggingElement != null) {
                draggingElement.release();
                draggingElement = null;
            }
            return;
        }

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        if (event.getAction() == 1 && event.getButton() == 0) {
            List<DraggableHudElement> reversed = new ArrayList<>(draggableElements);
            Collections.reverse(reversed);

            for (DraggableHudElement element : reversed) {
                if (element.isMouseOver(mouseX, mouseY)) {
                    draggingElement = element;
                    dragOffsetX = (float) mouseX - element.getX();
                    dragOffsetY = (float) mouseY - element.getY();
                    break;
                }
            }
        }
        else if (event.getAction() == 0 && event.getButton() == 0) {
            if (draggingElement != null) {
                draggingElement.release();
                draggingElement = null;
            }
        }
    }

    public Vector2f getNearest(float x, float y) {
        float minDeltaX = Float.MAX_VALUE;
        float minDeltaY = Float.MAX_VALUE;
        float snapDistance = 3f;
        Vector2f nearest = new Vector2f(-1, -1);

        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;

        if (Math.abs(x - centerX) < snapDistance) {
            nearest.x = centerX;
            minDeltaX = Math.abs(x - centerX);
        }

        if (Math.abs(y - centerY) < snapDistance) {
            nearest.y = centerY;
            minDeltaY = Math.abs(y - centerY);
        }

        for (DraggableHudElement element : draggableElements) {
            if (element == draggingElement) continue;

            float[] xPoints = {
                    element.getX(),
                    element.getX() + element.getWidth(),
                    element.getX() + element.getWidth() / 2
            };
            float[] yPoints = {
                    element.getY(),
                    element.getY() + element.getHeight(),
                    element.getY() + element.getHeight() / 2
            };

            for (float xPoint : xPoints) {
                float deltaX = Math.abs(x - xPoint);
                if (deltaX < minDeltaX && deltaX < snapDistance) {
                    nearest.x = xPoint;
                    minDeltaX = deltaX;
                }
            }

            for (float yPoint : yPoints) {
                float deltaY = Math.abs(y - yPoint);
                if (deltaY < minDeltaY && deltaY < snapDistance) {
                    nearest.y = yPoint;
                    minDeltaY = deltaY;
                }
            }
        }

        return nearest;
    }

}