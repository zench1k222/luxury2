package dev.luxury.modules.impl.render;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.player.ServerUtil;
import dev.luxury.utils.render.ColorRGBA;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@ModuleAnnotation(
        name = "Arrows",
        category = Category.Render,
        desc = "Показывает направление на игроков вокруг"
)
public class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();
    public static Identifier TRIANGLE_TEXTURE = Identifier.of("luxury", "images/triangle.png");

    private static final ColorRGBA FRIEND_COLOR = new ColorRGBA(20, 225, 20, 220);
    private static final ColorRGBA ENEMY_COLOR = new ColorRGBA(20, 250, 20, 200);
    private static final ColorRGBA DIST_COLOR = new ColorRGBA(150, 150, 160, 220);

    private final SliderSetting size = new SliderSetting("Радиус", "Радиус стрелок", 60, 40, 200, 5);
    private final SliderSetting distance = new SliderSetting("Дистанция", "Максимальная дистанция отображения", 50, 10, 200, 5);
    private final BooleanSetting showNames = new BooleanSetting("Показывать имена", true);
    private final BooleanSetting showDistance = new BooleanSetting("Показывать дистанцию", true);
    private final BooleanSetting showHealth = new BooleanSetting("Показывать здоровье", false);
    private final BooleanSetting animate = new BooleanSetting("Анимация", true);

    public float animationStep;
    private float lastYaw;
    private float lastPitch;
    private float animatedYaw;
    private float animatedPitch;

    public Arrows() {
        addSettings(
                size, distance,
                showNames, showDistance, showHealth,
                animate
        );
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) {
            return;
        }

        float targetSize = size.getFloatValue();
        if (mc.currentScreen instanceof InventoryScreen) {
            targetSize += 100;
        }

        if (animate.get()) {
            animationStep += (targetSize - animationStep) * 0.1f;
        } else {
            animationStep = targetSize;
        }

        if (mc.options.getPerspective().isFirstPerson() && animationStep <= 200) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.getName().getString().isEmpty()) {
                    continue;
                }

                double playerDistance = player.distanceTo(mc.player);
                if (playerDistance > distance.getFloatValue()) {
                    continue;
                }

                double x = player.prevX + (player.getX() - player.prevX) * event.getDeltatick().getTickDelta(true)
                        - mc.gameRenderer.getCamera().getPos().getX();
                double z = player.prevZ + (player.getZ() - player.prevZ) * event.getDeltatick().getTickDelta(true)
                        - mc.gameRenderer.getCamera().getPos().getZ();

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

                boolean isFriend = FriendManager.getInstance().isFriend(player.getName().getString());

                drawTriangle(matrices, player, isFriend, playerDistance);

                matrices.pop();

                if (showNames.get() || showDistance.get() || showHealth.get()) {
                    drawPlayerInfo(event, player, (float) x2, (float) y2, angle, isFriend, playerDistance);
                }
            }
        }

        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
    }

    private void drawTriangle(MatrixStack matrices, PlayerEntity player, boolean isFriend, double distance) {
        ColorRGBA color = isFriend ? FRIEND_COLOR : ENEMY_COLOR;

        float distanceAlpha = (float) (1.0 - (distance / this.distance.getFloatValue()) * 0.5);
        int newAlpha = (int) (color.getAlpha() * distanceAlpha);
        color = new ColorRGBA(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);

        RenderUtil.drawImageAlpha(matrices, TRIANGLE_TEXTURE, -8, +30, 12, 12, color, color, color, color);
    }

    private void drawPlayerInfo(EventRender2D e, PlayerEntity player, float x, float y, float angle,
                                boolean isFriend, double distance) {
        FontDraw sfpro2 = FontHelper.sfprobold[15];

        int friendColor = isFriend ? FRIEND_COLOR.getRGB() : DIST_COLOR.getRGB();
        int neutralColor = DIST_COLOR.getRGB();

        int startY = (int)(y + 20);
        int lineHeight = 10;
        int lineIndex = 0;

        if (showNames.get()) {
            String name = player.getName().getString();
            sfpro2.drawCentered(e.getDrawContext().getMatrices(), name, x, startY + (lineIndex * lineHeight), neutralColor);
            lineIndex++;
        }

        if (showHealth.get()) {
            float health = ServerUtil.getHealth(player);
            String healthText = "hp " + String.format("%.1f", health);
            sfpro2.drawCentered(e.getDrawContext().getMatrices(), healthText, x, startY + (lineIndex * lineHeight), friendColor);
            lineIndex++;
        }

        if (showDistance.get()) {
            String distanceText = "↔ " + String.format("%.1f", distance) + " m";
            sfpro2.drawCentered(e.getDrawContext().getMatrices(), distanceText, x, startY + (lineIndex * lineHeight), neutralColor);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        animationStep = size.getFloatValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        animationStep = size.getFloatValue();
    }

    public void tick() {
        if (mc.player != null) {
            float yawDiff = mc.player.getYaw() - lastYaw;
            float pitchDiff = mc.player.getPitch() - lastPitch;

            animatedYaw += yawDiff * 0.1f;
            animatedPitch += pitchDiff * 0.1f;

            animatedYaw *= 0.9f;
            animatedPitch *= 0.9f;
        }
    }
}