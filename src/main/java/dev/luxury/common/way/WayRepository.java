package dev.luxury.common.way;

import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class WayRepository {
    private static WayRepository instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final List<Way> wayList = new ArrayList<>();
    private FontDraw fontDraw;
    private boolean initialized = false;

    private WayRepository() {
    }

    private void initializeFont() {
        if (!initialized && FontHelper.monsterrat != null && FontHelper.monsterrat[18] != null) {
            fontDraw = FontHelper.monsterrat[18];
            initialized = true;
        }
    }

    public static WayRepository getInstance() {
        if (instance == null) {
            instance = new WayRepository();
        }
        return instance;
    }

    public boolean isEmpty() {
        return wayList.isEmpty();
    }

    public void addWay(String name, BlockPos pos, String server) {
        wayList.add(new Way(name, pos, server));
    }

    public boolean hasWay(String name) {
        return wayList.stream().anyMatch(way -> way.name().equalsIgnoreCase(name));
    }

    public void deleteWay(String name) {
        int before = wayList.size();
        wayList.removeIf(way -> way.name().equalsIgnoreCase(name));
        int after = wayList.size();
        if (before != after) {
        }
    }

    public void clearList() {
        wayList.clear();
    }

    public void render(MatrixStack matrices) {
        if (isEmpty() || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (!initialized) {
            initializeFont();
        }

        String currentServer = mc.getNetworkHandler().getServerInfo() != null
                ? mc.getNetworkHandler().getServerInfo().address
                : "vanilla";

        for (Way way : wayList) {
            if (!way.server().equalsIgnoreCase(currentServer)) {
                continue;
            }

            Vec3d wayPos = way.pos().toCenterPos();
            RenderUtil.render3D.setTranslation(matrices);
            Vec3d screenPos = RenderUtil.render3D.worldSpaceToScreenSpace(wayPos);

            if (screenPos.z > 0 && screenPos.z < 1) {
                float distance = (float) mc.player.getPos().distanceTo(wayPos);
                String text = way.name() + " (" + Math.round(distance) + "m)";

                int color = getColorByDistance(distance);
                drawWaypointMarker(matrices, screenPos.x, screenPos.y, text, color);
            }
        }
    }

    private int getColorByDistance(float distance) {
        if (distance < 20) return 0xFF00FF00;
        if (distance < 50) return 0xFFFFFF00;
        if (distance < 100) return 0xFFFFA500;
        return 0xFFFF0000;
    }

    private void drawWaypointMarker(MatrixStack matrices, double x, double y, String text, int color) {
        matrices.push();

        if (fontDraw != null) {
            float textWidth = fontDraw.getWidth(text);
            float textHeight = fontDraw.getHeight();

            RenderUtil.drawRoundedRect(matrices,
                    (float) x - textWidth / 2 - 4,
                    (float) y - textHeight / 2 - 4,
                    textWidth + 8,
                    textHeight + 6,
                    new Vector4f(4, 4, 4, 4),
                    0x80000000);

            fontDraw.drawCenteredString(matrices, text, (float) x, (float) y - 1, color);

            matrices.pop();
        }
    }
}