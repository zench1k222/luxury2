package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;

@ModuleAnnotation(name = "SkeletonEsp", desc = "Рендерит скелет игроков линиями", category = Category.Render)
public class SkeletonEsp extends Module {

    public final Color color = new Color(255, 255, 255);
    public final Color friendColor = new Color(0, 255, 0);
    public final SliderSetting size = new SliderSetting("Толщина", 1.5f, 0.5f, 3f, 0.5f);

    public SkeletonEsp() {
        addSettings(size);
    }

    @EventTarget
    public void onRender(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        float partialTicks = event.getPartialTicks();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isAlive() && !player.isInvisible()) {
                boolean isLocalPlayer = player == mc.player;
                boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();

                if (!isLocalPlayer || !isFirstPerson) {
                    renderSkeleton(player, partialTicks);
                }
            }
        }
    }

    private void renderSkeleton(PlayerEntity player, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, player.prevX, player.getX());
        double y = MathHelper.lerp(partialTicks, player.prevY, player.getY());
        double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ());

        float scale = player.isBaby() ? 0.5f : 1.05f;

        double headY = 1.6 * scale;
        double torsoTopY = 1.2 * scale;
        double torsoBottomY = 0.6 * scale;
        double armOffset = 0.33;
        double handY = 0.8 * scale;
        double legOffset = 0.15 * scale;
        double footY = 0;

        if (player.isSwimming()) {
            headY = 1.5 * scale;
            torsoTopY = 1.2 * scale;
            torsoBottomY = 0.6 * scale;
            handY = 0.8 * scale;
            footY = 0.1 * scale;
            armOffset = 0.33;
            legOffset = 0.25 * scale;
        } else if (player.isSneaking()) {
            headY = 1.25 * scale;
            torsoTopY = 0.95 * scale;
            torsoBottomY = 0.45 * scale;
            handY = 0.55 * scale;
            footY = -0.1 * scale;
        } else if (player.isGliding()) {
            headY = 1.5 * scale;
            torsoTopY = 1.2 * scale;
            torsoBottomY = 0.6 * scale;
            handY = 0.8 * scale;
            footY = 0.1 * scale;
            armOffset = 0.33;
        }

        Color lineColor = isFriend(player) ? friendColor : color;

        float bodyYaw = interpolateRotation(player.prevBodyYaw, player.bodyYaw, partialTicks);
        float headYaw = interpolateRotation(player.prevHeadYaw, player.headYaw, partialTicks);
        float headPitch = MathHelper.lerp(partialTicks, player.prevPitch, player.getPitch());
        float netHeadYaw = headYaw - bodyYaw;

        headPitch = MathHelper.clamp(headPitch, -60.0f, 60.0f);

        float limbPos = player.limbAnimator.getPos(partialTicks);
        float limbSpeed = player.limbAnimator.getSpeed(partialTicks);

        if (player.isBaby()) {
            limbPos *= 3.0f;
            limbSpeed *= 0.8f;
        }
        limbSpeed = Math.min(limbSpeed, 1.0f);

        float rightArmRotateX = MathHelper.cos(limbPos * 0.6662f + (float) Math.PI) * 2.0f * limbSpeed * 0.5f;
        float leftArmRotateX = MathHelper.cos(limbPos * 0.6662f) * 2.0f * limbSpeed * 0.5f;
        float rightLegRotateX = MathHelper.cos(limbPos * 0.6662f) * 1.4f * limbSpeed;
        float leftLegRotateX = MathHelper.cos(limbPos * 0.6662f + (float) Math.PI) * 1.4f * limbSpeed;

        float swingProgress = player.getHandSwingProgress(partialTicks);
        if (swingProgress > 0.0f) {
            float swingAngle = -MathHelper.sin(swingProgress * (float) Math.PI) * 1.5f;
            leftArmRotateX += swingAngle;
        }

        MatrixStack matrices = new MatrixStack();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(bodyYaw));

        if (player.isSwimming() || player.isGliding()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch + 90.0f));
        } else if (player.isSneaking()) {
            matrices.translate(0.0f, -0.1f * scale, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(4.0f));
        }

        Matrix4f baseMatrix = new Matrix4f(matrices.peek().getPositionMatrix());

        matrices.push();
        matrices.translate(0.0f, torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch * 0.5f));
        addLine(matrices.peek().getPositionMatrix(), 0, 0, 0, 0, (float) (headY - torsoTopY), 0, lineColor);
        matrices.pop();

        addLine(baseMatrix, 0, (float) torsoTopY, 0, 0, (float) torsoBottomY, 0, lineColor);

        matrices.push();
        matrices.translate(-armOffset, torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toDegrees(leftArmRotateX)));
        addLine(matrices.peek().getPositionMatrix(), 0, 0, 0, 0, (float) (handY - torsoTopY), 0, lineColor);
        matrices.pop();
        addLine(baseMatrix, 0, (float) torsoTopY, 0, (float) -armOffset, (float) torsoTopY, 0, lineColor);

        matrices.push();
        matrices.translate(armOffset, torsoTopY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toDegrees(rightArmRotateX)));
        addLine(matrices.peek().getPositionMatrix(), 0, 0, 0, 0, (float) (handY - torsoTopY), 0, lineColor);
        matrices.pop();
        addLine(baseMatrix, 0, (float) torsoTopY, 0, (float) armOffset, (float) torsoTopY, 0, lineColor);

        matrices.push();
        matrices.translate(-legOffset, torsoBottomY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toDegrees(leftLegRotateX)));
        addLine(matrices.peek().getPositionMatrix(), 0, 0, 0, 0, (float) (footY - torsoBottomY), 0, lineColor);
        matrices.pop();
        addLine(baseMatrix, 0, (float) torsoBottomY, 0, (float) -legOffset, (float) torsoBottomY, 0, lineColor);

        matrices.push();
        matrices.translate(legOffset, torsoBottomY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(toDegrees(rightLegRotateX)));
        addLine(matrices.peek().getPositionMatrix(), 0, 0, 0, 0, (float) (footY - torsoBottomY), 0, lineColor);
        matrices.pop();
        addLine(baseMatrix, 0, (float) torsoBottomY, 0, (float) legOffset, (float) torsoBottomY, 0, lineColor);
    }

    private void addLine(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        Vec3d start = transformPoint(matrix, x1, y1, z1);
        Vec3d end = transformPoint(matrix, x2, y2, z2);

        RenderUtil3D.lineQueue.add(new RenderUtil3D.LineAction(start, end, color));
    }

    private Vec3d transformPoint(Matrix4f matrix, float x, float y, float z) {
        Vector4f point = new Vector4f(x, y, z, 1.0f);
        point.mul(matrix);
        return new Vec3d(point.x, point.y, point.z);
    }

    private float interpolateRotation(float prev, float current, float partialTicks) {
        float delta = current - prev;
        while (delta < -180.0f) delta += 360.0f;
        while (delta >= 180.0f) delta -= 360.0f;
        return prev + partialTicks * delta;
    }

    private float toDegrees(float radians) {
        return radians * (180f / (float) Math.PI);
    }

    private boolean isFriend(PlayerEntity player) {
         return FriendManager.getInstance().isFriend(player.getName().getString());
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