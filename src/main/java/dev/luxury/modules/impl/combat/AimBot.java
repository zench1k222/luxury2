package dev.luxury.modules.impl.combat;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleAnnotation(
        name = "AimBot",
        desc = "Простой аим для трезубца и лука",
        category = Category.Combat
)
public class AimBot extends Module {
    private final SliderSetting range = new SliderSetting("Дистанция", 20.0, 1.0, 50.0, 0.5);
    private final SliderSetting fov = new SliderSetting("Угол поиска", 90.0, 1.0, 180.0, 1.0);
    private final SliderSetting speed = new SliderSetting("Скорость", 25.0, 1.0, 50.0, 0.5);
    private final SliderSetting smoothness = new SliderSetting("Плавность", 0.15, 0.01, 1.0, 0.01);

    private final BooleanSetting verticalAim = new BooleanSetting("Вертикальный аим", true);
    private final BooleanSetting predict = new BooleanSetting("Предсказывать", true);
    private final BooleanSetting onlyTrident = new BooleanSetting("Только трезубец", false);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Игнорировать друзей", true);
    private final BooleanSetting ignoreInvisible = new BooleanSetting("Игнорировать невидимых", true);
    private final BooleanSetting autoThrow = new BooleanSetting("Авто бросок", false);

    private PlayerEntity target = null;
    private int throwCooldown = 0;

    public static AimBot instance;

    public AimBot() {
        addSettings(range, fov, speed, smoothness, verticalAim, predict,
                onlyTrident, ignoreFriends, ignoreInvisible, autoThrow);
        instance = this;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (onlyTrident.get() &&
                mc.player.getMainHandStack().getItem() != Items.TRIDENT &&
                mc.player.getOffHandStack().getItem() != Items.TRIDENT) {
            target = null;
            return;
        }

        if (throwCooldown > 0) throwCooldown--;

        target = findTarget();

        if (target != null) {
            aimAtTarget();

            if (autoThrow.get() && throwCooldown == 0 && shouldThrow()) {
                throwItem();
            }
        }
    }

    private PlayerEntity findTarget() {
        if (mc.player == null || mc.world == null) return null;

        // Получаем всех игроков в радиусе
        List<PlayerEntity> players = mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> player.isAlive() && !player.isDead())
                .filter(player -> !player.isSpectator())
                .filter(player -> mc.player.distanceTo(player) <= range.getValue())
                .filter(player -> {
                    if (ignoreInvisible.get() && player.isInvisible()) return false;
                    if (ignoreFriends.get() && FriendManager.getInstance().isFriend(player.getName().getString())) return false;
                    return true;
                })
                .sorted(Comparator.comparingDouble(player -> mc.player.distanceTo(player)))
                .collect(Collectors.toList());

        for (PlayerEntity player : players) {
            if (isInFOV(player)) {
                return player;
            }
        }

        return null;
    }

    private boolean isInFOV(PlayerEntity player) {
        if (fov.getValue() >= 180.0) return true;

        Vec3d playerPos = mc.player.getCameraPosVec(1.0f);
        Vec3d targetPos = player.getBoundingBox().getCenter();
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        double dot = direction.dotProduct(lookVec);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));

        return angle <= fov.getValue() / 2.0;
    }

    private void aimAtTarget() {
        if (target == null || mc.player == null) return;

        Vec3d targetPos = getTargetPosition();

        Vec3d eyePos = mc.player.getEyePos();
        double deltaX = targetPos.x - eyePos.x;
        double deltaY = targetPos.y - eyePos.y;
        double deltaZ = targetPos.z - eyePos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;

        float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        smoothAim(targetYaw, targetPitch);
    }

    private Vec3d getTargetPosition() {
        if (!predict.get() || target == null) {
            return target.getBoundingBox().getCenter();
        }

        Vec3d currentPos = target.getBoundingBox().getCenter();
        Vec3d velocity = target.getVelocity();

        double distance = mc.player.distanceTo(target);
        double time = distance / 20.0;

        return currentPos.add(velocity.multiply(time));
    }

    private void smoothAim(float targetYaw, float targetPitch) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        currentYaw = MathHelper.wrapDegrees(currentYaw);
        targetYaw = MathHelper.wrapDegrees(targetYaw);

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float maxChange = speed.getFloatValue();
        yawDiff = MathHelper.clamp(yawDiff, -maxChange, maxChange);
        pitchDiff = MathHelper.clamp(pitchDiff, -maxChange, maxChange);

        float smooth = smoothness.getFloatValue();
        float newYaw = currentYaw + yawDiff * smooth;
        float newPitch = currentPitch;

        if (verticalAim.get()) {
            newPitch += pitchDiff * smooth;
            newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }

    private boolean shouldThrow() {
        if (target == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d direction = targetPos.subtract(eyePos).normalize();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        double dot = direction.dotProduct(lookVec);
        return dot > 0.98;
    }

    private void throwItem() {
        boolean hasTrident = mc.player.getMainHandStack().getItem() == Items.TRIDENT ||
                mc.player.getOffHandStack().getItem() == Items.TRIDENT;
        boolean hasBow = mc.player.getMainHandStack().getItem() == Items.BOW ||
                mc.player.getOffHandStack().getItem() == Items.BOW;
        boolean hasCrossbow = mc.player.getMainHandStack().getItem() == Items.CROSSBOW ||
                mc.player.getOffHandStack().getItem() == Items.CROSSBOW;

        if (!hasTrident && !hasBow && !hasCrossbow) return;

        if (hasTrident) {
            if (mc.player.getItemUseTime() == 0) {
                mc.options.useKey.setPressed(true);
            } else if (mc.player.getItemUseTime() >= 10) {
                mc.options.useKey.setPressed(false);
                throwCooldown = 20;
            }
        } else if (hasBow || hasCrossbow) {
            if (mc.player.getItemUseTime() == 0) {
                mc.options.useKey.setPressed(true);
            } else if (mc.player.getItemUseTime() >= (hasBow ? 20 : 25)) {
                mc.options.useKey.setPressed(false);
                throwCooldown = 20;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        throwCooldown = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        throwCooldown = 0;
        mc.options.useKey.setPressed(false);
    }

    public PlayerEntity getTarget() {
        return target;
    }
}