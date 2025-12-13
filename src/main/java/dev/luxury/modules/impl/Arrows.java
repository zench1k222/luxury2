package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.render.ColorRGBA;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@ModuleAnnotation(
        name = "Arrows",
        category = Category.Render
)
public class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();

    public static Identifier TRIANGLE_TEXTURE = Identifier.of("luxury", "images/triangle.png");

    public float animationStep;
    private float lastYaw;
    private float lastPitch;
    private float animatedYaw;
    private float animatedPitch;

    public Arrows() {
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        float targetSize = 60;
        if (mc.currentScreen instanceof InventoryScreen) {
            targetSize += 100;
        }

        animationStep += (targetSize - animationStep) * 0.1f;

        if (mc.options.getPerspective().isFirstPerson() && animationStep <= 100) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.getName().getString().isEmpty()) {
                    continue;
                }

                double x = player.prevX + (player.getX() - player.prevX) * event.getDeltatick().getTickDelta(true) - mc.gameRenderer.getCamera().getPos().getX();
                double z = player.prevZ + (player.getZ() - player.prevZ) * event.getDeltatick().getTickDelta(true) - mc.gameRenderer.getCamera().getPos().getZ();

                double cos = MathHelper.cos((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI * 2 / 360)));
                double sin = MathHelper.sin((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI * 2 / 360)));
                double rotY = -(z * cos - x * sin);
                double rotX = -(x * cos + z * sin);

                float angle = (float) (Math.atan2(rotY, rotX) * 180 / Math.PI);

                double x2 = animationStep * MathHelper.cos((float) Math.toRadians(angle)) + mc.getWindow().getScaledWidth() / 2f;
                double y2 = animationStep * MathHelper.sin((float) Math.toRadians(angle)) + mc.getWindow().getScaledHeight() / 2f;

                x2 += animatedYaw;
                y2 += animatedPitch;

                MatrixStack matrices = event.getDrawContext().getMatrices();
                matrices.push();

                matrices.translate(x2, y2, 0);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle + 90));

                boolean isFriend = Luxury.getInstance().getFriendManager().isFriend(player.getName().getString());

                drawTriangle(event.getDrawContext().getMatrices(), isFriend);

                matrices.pop();
            }
        }

        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
    }

    private void drawTriangle(MatrixStack matrices, boolean isFriend) {
        if (isFriend) {
            ColorRGBA friendColor = new ColorRGBA(0, 255, 0, 200);
            RenderUtil.drawImageAlpha(matrices, TRIANGLE_TEXTURE, -8, -9, 18, 18, friendColor, friendColor, friendColor, friendColor);
        } else {
            ColorRGBA color1 = new ColorRGBA(0, 100, 255, 200);
            ColorRGBA color2 = new ColorRGBA(0, 150, 255, 200);
            ColorRGBA color3 = new ColorRGBA(0, 50, 200, 200);
            ColorRGBA color4 = new ColorRGBA(30, 120, 255, 200);
            RenderUtil.drawImageAlpha(matrices, TRIANGLE_TEXTURE, -8, -9, 18, 18, color1, color2, color3, color4);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        animationStep = 60;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        animationStep = 60;
    }
}