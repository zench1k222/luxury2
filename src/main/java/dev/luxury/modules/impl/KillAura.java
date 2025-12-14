package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventMoveInput;
import dev.luxury.events.impl.client.EventRotate;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.modules.impl.killaura.*;
import dev.luxury.modules.impl.killaura.rotate.*;
import dev.luxury.utils.managers.FriendManager;
import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

@ModuleAnnotation(
        name = "KillAura",
        desc = "",
        category = Category.Combat
)
public class KillAura extends Module {
    private final ModeSetting rotationMode = new ModeSetting("Ротация", "HvH", new String[]{"HvH", "SlothAI", "ReallyWorld"});
    private final ModeSetting sprintMode = new ModeSetting("Бег", "Ordinary", new String[]{"HvH", "Ordinary", "Legit", "Без сброса"});
    private final ModeSetting correction = new ModeSetting("Коррекция Движения", "Свободная", new String[]{"Сфокусированная", "Свободная", "Без корекции"});

    public final SliderSetting distance = new SliderSetting("Расстония", "Дистанция атаки", 3.0, 0.5, 6.0, 0.1);
    private final SliderSetting distanceRotation = new SliderSetting("пре-расстония", 0.1, 0.0, 6.0, 0.1);

    private final ModeListSetting settings = new ModeListSetting("Настройки",
            new BooleanSetting("Ломать щит", true),
            new BooleanSetting("Отжимать щит", true),
            new BooleanSetting("Бить и есть", true),
            new BooleanSetting("Бить через стены", true));

    private final ModeListSetting targetTypeSetting = new ModeListSetting("Кого атаковать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Враждебные мобы", false),
            new BooleanSetting("Мирные мобы", false));

    private final BooleanSetting onlyCrit = new BooleanSetting("Только криты", true);
    private final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Бьет критами если зажата кнопка прыжка", false);

    public KillAura() {
        addSettings(rotationMode, sprintMode, correction, distance, distanceRotation, settings, targetTypeSetting, onlyCrit, smartCrit);
    }

    Aim aim = new Aim();

    private final ValidTarget targetSelector = new ValidTarget();
    private final ValidPoint validPoint = new ValidPoint();
    private LivingEntity target = null;
    private LivingEntity lastTarget = null;
    private float lastTargetHealth = 0f;
    private boolean legitBackStop = false;
    @Getter
    private boolean preAttack = false;
    @Getter
    private boolean isCanAttack = false;

    @EventTarget
    public void eventRotate(EventRotate e) {
        if (legitBackStop) {
            legitBackStop = false;
            mc.options.forwardKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode()));
        }

        target = updateTarget();

        checkTargetKilled();

        preAttack = false;
        isCanAttack = false;
        if (target == null) return;

        BooleanSetting attackIgnoreWals = settings.getValueByName("Бить через стены");

        float rotationRange = distance.getFloatValue() + distanceRotation.getFloatValue();

        Pair<Vec3d, Box> point = validPoint.computeVector(target, rotationRange, Luxury.getInstance().getRotationManager().getCurrentRotate(), new Vec3d(0, 0, 0), attackIgnoreWals != null && attackIgnoreWals.get()
        );

        Vec3d eyes = Simulation.simulateLocalPlayer(1).pos.add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Rotate angle = RotateUtils.fromVec3d(point.getLeft().subtract(eyes));

        Box box = point.getRight();
        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        if (RayTrace.rayTrace(Luxury.getInstance().getRotationManager().getCurrentRotate().toVector(), distance.getFloatValue(), box)
                && isCanAttack
                && (!Luxury.getInstance().getServerHandler().isServerSprint() || mc.player.isGliding() || Criticals.hasMovementRestrictions() || sprintMode.is("HvH") || sprintMode.is("Без сброса"))) {

            if (sprintMode.is("HvH")) {
                mc.player.setSprinting(false);
                mc.player.sendSprintingPacket();
            }

            Criticals.attackEntity(target);

            mc.options.sprintKey.setPressed(true);
        }

        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        if (rotationMode.is("HvH")) {
            Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getInstantSetup(), angle), aim.getInstantSetup()), 3, this);
        }

        if (rotationMode.is("SlothAI")) {
            Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getSlothAISetup(), angle), aim.getSlothAISetup()), 3, this);
        }

        if (rotationMode.is("ReallyWorld")) {
            Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getReallyWorldSetup(), angle), aim.getReallyWorldSetup()), 3, this);
        }

        if (preAttack || isCanAttack) {
            updateSprint();
        }

        if (target != null) {
            lastTarget = target;
            lastTargetHealth = target.getHealth();
        }
    }



    private void checkTargetKilled() {

        if (lastTarget == null) {
            return;
        }

        if (!lastTarget.isRemoved()) {
            float currentHealth = lastTarget.getHealth();

            if (currentHealth <= 0 || lastTarget.isDead() || !lastTarget.isAlive()) {
                ClientSounds.getInstance().playKillSound();
                lastTarget = null;
                lastTargetHealth = 0f;
                return;
            }

            lastTargetHealth = currentHealth;
        } else {
            if (lastTargetHealth > 0) {
                ClientSounds.getInstance().playKillSound();
            }
            lastTarget = null;
            lastTargetHealth = 0f;
        }
    }
    private boolean updatePreAttack() {
        Simulation simulatedPlayer = Simulation.simulateLocalPlayer(1);
        BooleanSetting eatUseAttack = settings.getValueByName("Бить и есть");

        if (mc.player.isUsingItem() && (eatUseAttack == null || !eatUseAttack.get())) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;

        if (onlyCrit.get() && !Criticals.hasPreMovementRestrictions(simulatedPlayer)) {
            return Criticals.isPrePlayerInCriticalState(simulatedPlayer) || (smartCrit.get() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    private boolean isAttack() {
        BooleanSetting eatUseAttack = settings.getValueByName("Бить и есть");
        if (mc.player.isUsingItem() && (eatUseAttack == null || !eatUseAttack.get())) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;
        if (onlyCrit.get() && !Criticals.hasMovementRestrictions()) {
            return Criticals.isPlayerInCriticalState() || (smartCrit.get() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    public void updateSprint() {
        if (!hasStopSprint()) return;

        boolean sprint = mc.options.sprintKey.isPressed();
        boolean forward = mc.options.forwardKey.isPressed();

        if (sprintMode.is("Legit")) {
            sprint = false;
            if (mc.player.isSprinting()) {
                forward = false;
                legitBackStop = true;
            }
        }

        if (sprintMode.is("Ordinary")) {
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
            sprint = false;
        }

        mc.options.sprintKey.setPressed(sprint);
        mc.options.forwardKey.setPressed(forward);
    }

    public boolean hasStopSprint() {
        return !sprintMode.is("Без сброса") && !Criticals.hasMovementRestrictions();
    }

    private LivingEntity updateTarget() {
        List<String> selectedNames = targetTypeSetting.getSettings().stream().filter(BooleanSetting::get).map(BooleanSetting::getName).collect(Collectors.toList());
        ValidTarget.EntityFilter filter = new ValidTarget.EntityFilter(selectedNames);
        BooleanSetting attackIgnoreWals = settings.getValueByName("Бить через стены");
        targetSelector.searchTargets(mc.world.getEntities(), distance.getFloatValue() + distanceRotation.getFloatValue(), attackIgnoreWals != null && attackIgnoreWals.get());
        targetSelector.validateTarget(entity -> {
            if (FriendManager.getInstance().isFriend(entity.getName().getString())) {
                return false;
            }
            return filter.isValid(entity);
        });
        return targetSelector.getCurrentTarget();
    }
    public ModeSetting getRotationMode() {
        return rotationMode;
    }
    @EventTarget
    private void setCorrection(EventMoveInput eventMoveInput) {
        if (correction.is("Без корекции")) return;

        if (rotationMode.is("SlothAI")) return;

        if (target == null) return;

        float bodyYaw = Luxury.getInstance().getRotationManager().getBodyRotation().getYaw();

        if (correction.is("Сфокусированная")) {
            Rotate angle = RotateUtils.fromVec3d(target.getBoundingBox().getCenter().subtract(mc.player.getBoundingBox().getCenter()));
            Move.fixMovement(eventMoveInput, bodyYaw, angle.getYaw());
        } else {
            Move.fixMovement(eventMoveInput, bodyYaw, mc.player.getYaw());
        }
    }
    public LivingEntity getTarget() {
        return this.isEnabled() ? target : null;
    }

    public boolean hasTarget() {
        return this.isEnabled() && target != null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastTarget = null;
        lastTargetHealth = 0f;
    }
}