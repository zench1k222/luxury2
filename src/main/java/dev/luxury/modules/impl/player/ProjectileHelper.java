package dev.luxury.modules.impl.player;


import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventRotate;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.modules.impl.other.killaura.rotate.Aim;
import dev.luxury.modules.impl.other.killaura.rotate.Rotate;
import dev.luxury.modules.impl.other.killaura.rotate.TargetRotate;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ModuleAnnotation(
        name = "ProjectileHelper",
        desc = "Автоматически целится при использовании луков, арбалетов и трезубцев",
        category = Category.Player
)
public class ProjectileHelper extends Module {

    private final SliderSetting searchDistance = new SliderSetting("Дистанция поиска", "Радиус поиска цели вокруг игрока", 16.0, 5.0, 64.0, 1.0);

    private final ModeListSetting targetTypeSetting = new ModeListSetting("Кого атаковать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Враждебные мобы", true),
            new BooleanSetting("Мирные мобы", true),
            new BooleanSetting("Стойки для брони", false));

    private LivingEntity currentTarget;

    public ProjectileHelper() {
        addSettings(searchDistance, targetTypeSetting);
    }

    @EventTarget
    public void onRotate(EventRotate e) {
        ItemStack stack = mc.player.getMainHandStack();

        boolean holdingBow = stack.getItem() instanceof BowItem;
        boolean holdingCrossbow = stack.getItem() instanceof CrossbowItem && ((CrossbowItem) stack.getItem()).isCharged(stack);
        boolean holdingTrident = stack.getItem() instanceof TridentItem;

        if (!holdingBow && !holdingCrossbow && !holdingTrident) {
            currentTarget = null;
            return;
        }

        if (holdingBow && mc.player.getActiveItem() != stack) {
            currentTarget = null;
            return;
        }

        if (currentTarget != null && !currentTarget.isAlive()) {
            currentTarget = null;
        }

        if (currentTarget == null) {
            currentTarget = getTarget(mc.world.getEntities());
            if (currentTarget == mc.player) currentTarget = null;
        }

        if (currentTarget != null && FriendManager.getInstance().isFriend(currentTarget.getName().getString())) {
            currentTarget = null;
        }

        if (currentTarget != null) {
            Vec3d shooterPos = mc.player.getPos()
                    .add(0, mc.player.getEyeHeight(mc.player.getPose()), 0)
                    .add(mc.player.getVelocity());

            float projectileSpeed = 6.0f;
            float gravity = 0.02f;

            Vec3d predictedPos = getPredictedPosition(currentTarget, shooterPos, projectileSpeed, gravity);

            double dx = predictedPos.x - shooterPos.x;
            double dy = predictedPos.y - shooterPos.y;
            double dz = predictedPos.z - shooterPos.z;
            double distanceXZ = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f + getRandomFloat(-1, 1);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ)) + getRandomFloat(-1, 1);

            Rotate angle = new Rotate(yaw, pitch);

           Aim aim = new Aim();
           Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getInstantSetup(), angle), aim.getInstantSetup()), 3, this);
        }
    }

    private LivingEntity getTarget(Iterable<Entity> entities) {
        List<Entity> entityList = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());

        List<LivingEntity> validTargets = entityList.stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).filter(this::isValidTarget).collect(Collectors.toList());

        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        Vec3d playerPos = mc.player.getPos();

        for (LivingEntity target : validTargets) {
            double distance = target.getPos().distanceTo(playerPos);
            if (distance < nearestDistance && distance <= searchDistance.getValue()) {
                nearestDistance = distance;
                nearestTarget = target;
            }
        }

        return nearestTarget;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;

        List<String> selectedTypes = targetTypeSetting.getSettings().stream()
                .filter(BooleanSetting::get)
                .map(BooleanSetting::getName)
                .collect(Collectors.toList());

        if (entity instanceof PlayerEntity && !selectedTypes.contains("Игроки")) return false;
        if (entity instanceof MobEntity && !selectedTypes.contains("Враждебные мобы")) return false;
        if (entity instanceof AnimalEntity && !selectedTypes.contains("Мирные мобы")) return false;
        if (entity instanceof ArmorStandEntity && !selectedTypes.contains("Стойки для брони")) return false;

        return true;
    }

    private Vec3d getPredictedPosition(LivingEntity target, Vec3d shooterPos, float projectileSpeed, float gravity) {
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d targetVelocity = target.getVelocity();
        Vec3d delta = targetPos.subtract(shooterPos);

        double a = projectileSpeed * projectileSpeed - targetVelocity.lengthSquared();
        double b = -2 * delta.dotProduct(targetVelocity);
        double c = -delta.lengthSquared();

        double t;
        double discriminant = b * b - 4 * a * c;
        if (discriminant > 0) {
            double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
            double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);
            t = Math.max(t1, t2);
        } else {
            t = delta.length() / projectileSpeed;
        }

        Vec3d predicted = targetPos.add(targetVelocity.multiply(t));
        predicted = predicted.add(0, 0.5 * gravity * t * t, 0);

        return predicted;
    }

    private float getRandomFloat(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }
}