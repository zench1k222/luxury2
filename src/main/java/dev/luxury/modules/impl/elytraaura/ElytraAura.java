package dev.luxury.modules.impl.elytraaura;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.client.EventRotate;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.modules.impl.elytraaura.attack.ElytraAuraAttackHandler;
import dev.luxury.modules.impl.killaura.ValidTarget;
import dev.luxury.modules.impl.killaura.ValidPoint;
import dev.luxury.modules.impl.killaura.rotate.Aim;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.RotateUtils;
import dev.luxury.modules.impl.killaura.rotate.TargetRotate;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Arrays;

@ModuleAnnotation(name = "ElytraAura", desc = "Elytra combat aura", category = Category.Combat)
public class ElytraAura extends Module {

    private final SliderSetting attackRange = new SliderSetting("Дистанция атаки", 3.2, 2.5, 6.0, 0.1);
    private final SliderSetting searchRange = new SliderSetting("Радиус поиска", 40.0, 5.0, 80.0, 1.0);
    private final BooleanSetting predictMovement = new BooleanSetting("Обгон", true);
    private final SliderSetting predictDistance = new SliderSetting("Дистанция обгона", 1.5, 0.5, 5.0, 0.1);

    private final ValidTarget targetSelector = new ValidTarget();
    private final ValidTarget.EntityFilter entityFilter = new ValidTarget.EntityFilter(Arrays.asList("Игроки"));
    private final ValidPoint validPoint = new ValidPoint();
    private final Aim aim = new Aim();

    private LivingEntity target;
    private Vec3d predictedPos;

    public ElytraAura() {
        addSettings(attackRange, searchRange, predictMovement, predictDistance);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        predictedPos = null;
        targetSelector.releaseTarget();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        predictedPos = null;
        targetSelector.releaseTarget();
    }

    @EventTarget
    public void eventRotate(EventRotate e) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.isGliding()) {
            target = null;
            predictedPos = null;
            return;
        }

        target = updateTarget();
        if (target == null) {
            predictedPos = null;
            return;
        }

        Vec3d targetPos = calculateTargetPosition(target);
        predictedPos = targetPos;

        boolean usePredict = predictMovement.get() && isTargetMoving(target);

        Vec3d aimPos = usePredict ? predictedPos : target.getBoundingBox().getCenter();

        Vec3d eyes = mc.player.getEyePos();
        Rotate angle = RotateUtils.fromVec3d(aimPos.subtract(eyes));

        Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getInstantSetup(), angle), aim.getInstantSetup()), 3, this);

        double distanceToTarget = mc.player.getEyePos().distanceTo(aimPos);
        if (distanceToTarget <= attackRange.getValue()) {
            ElytraAuraAttackHandler.updateAttack(target, (float) attackRange.getValue());
        }
    }

    @EventTarget
    public void onRender3d(EventRender3D e) {
        if (target == null) return;

        Vec3d currentPos = target.getBoundingBox().getCenter();
        RenderUtil3D.drawBoxOutlines(new Box(currentPos.subtract(0.25, 0.25, 0.25), currentPos.add(0.25, 0.25, 0.25)), e.getMatrices(), new Color(0x55FFFFFF));

        if (predictMovement.get() && predictedPos != null && isTargetMoving(target)) {
            Box predictedBox = new Box(predictedPos.subtract(target.getWidth() / 2, 0, target.getWidth() / 2), predictedPos.add(target.getWidth() / 2, target.getHeight(), target.getWidth() / 2));

            RenderUtil3D.drawBoxOutlines(predictedBox, e.getMatrices(), new Color(0x5500FF00));
        }
    }

    private LivingEntity updateTarget() {
        targetSelector.searchTargets(
                mc.world.getEntities(),
                (float) searchRange.getValue(),
                true
        );

        targetSelector.validateTarget(entity -> {
            if (!(entity instanceof PlayerEntity)) return false;
            if (entity == mc.player) return false;
            if (!entity.isAlive()) return false;
            if (entity.isRemoved()) return false;

            return entityFilter.isValid(entity);
        });

        return targetSelector.getCurrentTarget();
    }

    private Vec3d calculateTargetPosition(LivingEntity entity) {
        Vec3d targetPos = entity.getPos();

        if (!predictMovement.get() || !isTargetMoving(entity)) {
            return targetPos;
        }

        Vec3d motion = new Vec3d(
                entity.getX() - entity.prevX,
                entity.getY() - entity.prevY,
                entity.getZ() - entity.prevZ
        );

        double predictFactor = Math.max(0.1, Math.min(predictDistance.getValue(), 6.0));
        Vec3d predicted = motion.multiply(predictFactor * 1.5);

        if (predicted.lengthSquared() < 1.0E-4) {
            predicted = entity.getRotationVecClient().normalize().multiply(predictFactor);
        }

        Vec3d predictedPos = targetPos.add(predicted);

        return predictedPos;
    }

    private boolean isTargetMoving(LivingEntity entity) {
        Vec3d velocity = entity.getVelocity();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        return speed > 0.05;
    }
    public LivingEntity getTarget(){
        return target;
    }
}