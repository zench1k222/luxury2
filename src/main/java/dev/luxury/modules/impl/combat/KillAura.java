package dev.luxury.modules.impl.combat;

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
import dev.luxury.modules.impl.other.killaura.*;
import dev.luxury.modules.impl.other.killaura.rotate.*;
import dev.luxury.modules.impl.render.ClientSounds;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.modules.impl.other.killaura.Criticals;
import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;

import java.util.List;
import java.util.stream.Collectors;

@ModuleAnnotation(
        name = "KillAura",
        desc = "",
        category = Category.Combat
)
public class KillAura extends Module {
    private final ModeSetting rotationMode = new ModeSetting("Ротация", "HvH", new String[]{"HvH", "SlothAI", "TriggerBot","SpookyTime","FunTime"});
    private final ModeSetting sprintMode = new ModeSetting("Бег", "Ordinary", new String[]{"HvH", "Ordinary", "Legit", "Без сброса"});
    private final ModeSetting correction = new ModeSetting("Коррекция Движения", "Свободная", new String[]{"Сфокусированная", "Свободная", "Без корекции"});

    public final SliderSetting distance = new SliderSetting("Расстония", "Дистанция атаки", 3.0, 0.5, 6.0, 0.1);
    public static final ModeSetting attackMethod = new ModeSetting("Метод аттаки", "New", new String[]{"New", "Old"});
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
    private final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Бьет критами если зажата кнопка прыжка", true);
    private final BooleanSetting grimDistance = new BooleanSetting("Grim дистанция", "Автоматически подстраивает дистанцию под Grim античит", true);
    private final BooleanSetting removeInterpolation = new BooleanSetting("Убрать интерполяцию у игроков", "Использует серверные позиции для точных попаданий", true);
 public static KillAura instance;
    public KillAura() {
        addSettings(rotationMode, sprintMode, correction, distance, attackMethod, distanceRotation, settings, targetTypeSetting, onlyCrit, smartCrit, grimDistance, removeInterpolation);
        instance = this;
    }

    Aim aim = new Aim();

    private final ValidTarget targetSelector = new ValidTarget();
    private final ValidPoint validPoint = new ValidPoint();
    private LivingEntity target = null;
    private LivingEntity lastTarget = null;
    private float lastTargetHealth = 0f;
    public static boolean state = false;
    private boolean legitBackStop = false;
    @Getter
    private boolean preAttack = false;
    @Getter
    private boolean isCanAttack = false;
    private int shieldBreakCooldown = 0;

    @Native
    @EventTarget
    public void eventRotate(EventRotate e) {
        if (legitBackStop) {
            legitBackStop = false;
            mc.options.forwardKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode()));
        }

        target = updateTarget();

        if (grimDistance.get()) {
            GrimDistance.updateTargetHistory(target);
        }

        checkTargetKilled();

        preAttack = false;
        isCanAttack = false;
        if (target == null) return;

        if (target.isUsingItem() && target.getActiveItem().isOf(Items.SHIELD)) {
            BooleanSetting breakShieldSetting = settings.getValueByName("Ломать щит");
            if (breakShieldSetting != null && breakShieldSetting.get()) {
                breakShield();
            }
        }

        BooleanSetting attackIgnoreWals = settings.getValueByName("Бить через стены");

        float attackDistance = distance.getFloatValue();
        float rotationRange = distance.getFloatValue() + distanceRotation.getFloatValue();

        if (grimDistance.get()) {
            attackDistance = GrimDistance.calculateSafeReach(target, distance.getFloatValue());
        }

        Pair<Vec3d, Box> point = validPoint.computeVector(target, rotationRange, Luxury.getInstance().getRotationManager().getCurrentRotate(), new Vec3d(0, 0, 0), attackIgnoreWals != null && attackIgnoreWals.get());

        Vec3d eyes = Simulation.simulateLocalPlayer(1).pos.add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Rotate angle = RotateUtils.fromVec3d(point.getLeft().subtract(eyes));

        Box box = point.getRight();
        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        boolean canHit = RayTrace.rayTrace(Luxury.getInstance().getRotationManager().getCurrentRotate().toVector(), attackDistance, box);

        if (grimDistance.get()) {
            canHit = canHit && GrimDistance.canSafelyAttack(target, distance.getFloatValue());
        }
        if (canHit && isCanAttack && (!Luxury.getInstance().getServerHandler().isServerSprint() || mc.player.isGliding() || Criticals.hasMovementRestrictions() || sprintMode.is("HvH") || sprintMode.is("Без сброса"))) {

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

        if (rotationMode.is("SpookyTime")) {
            Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getSpookytimeSetup(), angle), aim.getSpookytimeSetup()), 3, this);
        }

        if (rotationMode.is("FunTime")) {
            Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(angle, () -> aim.rotate(aim.getFuntimeSetup(), angle), aim.getFuntimeSetup()), 3, this);
        }

        if (preAttack || isCanAttack) {
            updateSprint();
        }

        if (target != null) {
            lastTarget = target;
            lastTargetHealth = target.getHealth();
        }
    }

    @Native
    private void breakShield() {
        if (target == null) return;

        if (!target.isUsingItem() || !target.getActiveItem().isOf(Items.SHIELD)) return;

        BooleanSetting breakShieldSetting = settings.getValueByName("Ломать щит");
        if (breakShieldSetting == null || !breakShieldSetting.get()) return;

        if (shieldBreakCooldown > 0) {
            shieldBreakCooldown--;
            return;
        }

        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }

        if (axeSlot == -1) {
            BooleanSetting shieldPushSetting = settings.getValueByName("Отжимать щит");
            if (shieldPushSetting != null && shieldPushSetting.get() && mc.interactionManager != null && target.isAlive()) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                shieldBreakCooldown = 10;
            }
            return;
        }

        int originalSlot = mc.player.getInventory().selectedSlot;

        if (originalSlot != axeSlot) {
            mc.player.getInventory().selectedSlot = axeSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(axeSlot));
        }

        if (mc.interactionManager != null && target.isAlive()) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }

        if (originalSlot != axeSlot) {
            mc.player.getInventory().selectedSlot = originalSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(originalSlot));
        }

        shieldBreakCooldown = 3;

        String targetName = target.getName().getString();
    }

    @Native
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

    @Native
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

    @Native
    private boolean isAttack() {
        BooleanSetting eatUseAttack = settings.getValueByName("Бить и есть");
        if (mc.player.isUsingItem() && (eatUseAttack == null || !eatUseAttack.get())) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;
        if (onlyCrit.get() && !Criticals.hasMovementRestrictions()) {
            return Criticals.isPlayerInCriticalState() || (smartCrit.get() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    @Native
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

    @Native
    public boolean hasStopSprint() {
        return !sprintMode.is("Без сброса") && !Criticals.hasMovementRestrictions();
    }

    @Native
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

    @Native
    public ModeSetting getRotationMode() {
        return rotationMode;
    }

    @Native
    @EventTarget
    private void setCorrection(EventMoveInput eventMoveInput) {
        if (correction.is("Без корекции")) return;

        if (rotationMode.is("SlothAI")) return;

        if (target == null) return;

        if (correction.is("Сфокусированная")) {
            Rotate angle = RotateUtils.fromVec3d(target.getBoundingBox().getCenter().subtract(mc.player.getBoundingBox().getCenter()));
            Move.fixMovement(eventMoveInput, Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw(), angle.getYaw());
        } else {
            Move.fixMovement(eventMoveInput, Luxury.getInstance().getRotationManager().getCurrentRotate().getYaw(), mc.player.getYaw());
        }
    }

    @Native
    public LivingEntity getTarget() {
        return this.isEnabled() ? target : null;
    }

    @Native
    public boolean hasTarget() {
        return this.isEnabled() && target != null;
    }

    @Native
    public boolean shouldRemoveInterpolation() {
        return removeInterpolation.get();
    }

    @Native
    @Override
    public void onEnable() {
        super.onEnable();
        state = true;
        shieldBreakCooldown = 0;
    }

    @Native
    @Override
    public void onDisable() {
        super.onDisable();
        state = false;
        lastTarget = null;
        lastTargetHealth = 0f;
        shieldBreakCooldown = 0;
        GrimDistance.reset();
    }
}