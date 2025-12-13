package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleAnnotation(
        name = "HitBubbles",
        category = Category.Render
)
public class HitBubles extends Module {

    private final List<HitBubble> bubbles = new CopyOnWriteArrayList<>();
    final Identifier bubbleTexture = Identifier.of("luxury", "images/bubble.png");

    private boolean lastAttackKeyPressed = false;


    @EventTarget
    public void onTick(EventTick event) {
        boolean currentAttack = mc.options.attackKey.isPressed();

        if (currentAttack && !lastAttackKeyPressed && mc.player != null) {
            LivingEntity target = getTarget();

            if (target != null) {
                Vec3d bubblePos = getHitPosition(target);
                bubbles.add(new HitBubble(bubblePos, new TimerUtils()));
            }
        }

        lastAttackKeyPressed = currentAttack;
        bubbles.removeIf(b -> b.timer().finished(3000));
    }

    private LivingEntity getTarget() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }

    private Vec3d getHitPosition(LivingEntity target) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
            return entityHit.getPos();
        }

        return new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
    }

    @EventTarget
    public void onWorldRender(EventRender3D e) {
        renderBubbles(e.getContext().getMatrices());
    }

    private void renderBubbles(MatrixStack stack) {
        if (bubbles.isEmpty()) return;

        for (HitBubble bubble : bubbles) {
            renderSingleBubble(stack, bubble);
        }
    }

    private void renderSingleBubble(MatrixStack stack, HitBubble bubble) {
        float progress = (float) bubble.timer().getMillis() / 3000f;

        if (progress >= 1f) return;

        float scale = progress * 2f;
        float alpha = 1f - progress;
        float rotation = bubble.timer().getMillis() / 10f;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = bubble.pos().subtract(camera.getPos());

        MatrixStack matrix = new MatrixStack();
        matrix.push();

        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        MatrixStack.Entry entry = matrix.peek();

        int r = 255;
        int g = 105;
        int b = 180;
        int a = (int)(alpha * 255);
        int pinkColor = (a << 24) | (r << 16) | (g << 8) | b;

        Vector4i colors = new Vector4i(pinkColor, pinkColor, pinkColor, pinkColor);

        RenderUtil3D.drawTexture(entry, bubbleTexture, -scale / 2, -scale / 2, scale, scale, colors, true);
        matrix.pop();
    }

    public record HitBubble(Vec3d pos, TimerUtils timer) {
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