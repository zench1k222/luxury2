package dev.luxury.utils.render;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

@UtilityClass
public class RenderHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vec3d cameraPos() {
        Camera camera = mc.gameRenderer.getCamera();
        return camera.getPos();
    }
}

