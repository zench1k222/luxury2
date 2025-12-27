package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.*;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
        name = "MaceAura",
        desc = "Булавный удар с высоты",
        category = Category.Combat
)
public class MaceAura extends Module {
    // Основные настройки
    private final SliderSetting attackRange = new SliderSetting("Дистанция атаки", 10.0f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting minHeight = new SliderSetting("Мин. высота", 30.0f, 10.0f, 100.0f, 1.0f);
    private final SliderSetting maxHeight = new SliderSetting("Макс. высота", 90.0f, 20.0f, 150.0f, 1.0f);
    private final SliderSetting swapDistance = new SliderSetting("Дистанция обмена", 10.0f, 5.0f, 20.0f, 0.5f);

    // Цели
    private final ModeListSetting targetTypeSetting = new ModeListSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Друзья", false));

    // Элитры
    private final BooleanSetting autoElytra = new BooleanSetting("Авто-элитры", true);
    private final BooleanSetting autoFirework = new BooleanSetting("Авто-фейерверк", "Автоматически использует фейерверки", true);
    private final BooleanSetting keepElytra = new BooleanSetting("Сохранять элитру", "Оставаться в элитре после атаки", false);

    // Щиты
    private final BooleanSetting shieldBreak = new BooleanSetting("Ломать щит", true);
    private final BooleanSetting shieldPush = new BooleanSetting("Отжимать щит", "Атаковать через щит", true);

    // Другое
    private final BooleanSetting checkObstacles = new BooleanSetting("Проверять препятствия", true);
    private final SliderSetting attackDelay = new SliderSetting("Задержка атаки", 200f, 0f, 1000f, 50f);
    private final BooleanSetting rotateToTarget = new BooleanSetting("Поворачиваться", true);
    private final BooleanSetting debugInfo = new BooleanSetting("Отладка", false);

    // Состояние
    private Entity target = null;
    private State currentState = State.IDLE;
    private long lastAttackTime = 0;
    private int shieldBreakCooldown = 0;
    private int elytraCooldown = 0;
    private boolean wasGliding = false;
    private boolean hasMaceEquipped = false;
    private boolean hasChestplateEquipped = false;
    private Vec3d targetPosition = null;
    private double targetHeight = 0;

    private enum State {
        IDLE,
        FINDING_TARGET,
        FLYING_UP,
        FLYING_DOWN,
        SWAPPING_GEAR,
        ATTACKING,
        RETURNING
    }

    public MaceAura() {
        addSettings(
                attackRange, minHeight, maxHeight, swapDistance,
                targetTypeSetting,
                autoElytra, autoFirework, keepElytra,
                shieldBreak, shieldPush,
                checkObstacles, attackDelay, rotateToTarget, debugInfo
        );
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        updateCooldowns();
        handleShieldBreak();
        handleStateMachine();
        debugInfo();
    }

    private void updateCooldowns() {
        if (shieldBreakCooldown > 0) shieldBreakCooldown--;
        if (elytraCooldown > 0) elytraCooldown--;
    }

    private void handleStateMachine() {
        switch (currentState) {
            case IDLE:
                handleIdleState();
                break;
            case FINDING_TARGET:
                handleFindingTargetState();
                break;
            case FLYING_UP:
                handleFlyingUpState();
                break;
            case FLYING_DOWN:
                handleFlyingDownState();
                break;
            case SWAPPING_GEAR:
                handleSwappingGearState();
                break;
            case ATTACKING:
                handleAttackingState();
                break;
            case RETURNING:
                handleReturningState();
                break;
        }
    }

    private void handleIdleState() {
        if (mc.player.isGliding()) {
            // Если уже летим, переходим к поиску цели
            currentState = State.FINDING_TARGET;
            return;
        }

        // Проверяем наличие элитры
        if (autoElytra.get() && !mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
            int elytraSlot = findItemSlot(Items.ELYTRA);
            if (elytraSlot != -1) {
                swapToChestSlot(elytraSlot);
                elytraCooldown = 5;
            }
        }

        // Начинаем поиск цели
        currentState = State.FINDING_TARGET;
    }

    private void handleFindingTargetState() {
        target = findTarget();

        if (target == null) {
            // Если нет цели, остаемся в режиме полета
            if (mc.player.isGliding()) {
                currentState = State.IDLE;
            }
            return;
        }

        // Определяем высоту полета
        Random random = new Random();
        targetHeight = minHeight.getValue() + random.nextDouble() * (maxHeight.getValue() - minHeight.getValue());
        targetPosition = target.getPos();

        // Взлетаем на нужную высоту
        if (mc.player.getY() < target.getY() + targetHeight) {
            currentState = State.FLYING_UP;
        } else {
            currentState = State.FLYING_DOWN;
        }
    }

    private void handleFlyingUpState() {
        if (!mc.player.isGliding() && autoElytra.get()) {
            startGliding();
        }

        if (autoFirework.get() && mc.player.isGliding()) {
            useFirework();
        }

        // Проверяем достигли ли нужной высоты
        if (mc.player.getY() >= target.getY() + targetHeight - 5) {
            currentState = State.FLYING_DOWN;
        } else {
            // Летим вверх
            controlFlight(true);
        }

        // Обновляем позицию цели
        if (target != null) {
            targetPosition = target.getPos();
        }
    }

    private void handleFlyingDownState() {
        if (target == null) {
            currentState = State.FINDING_TARGET;
            return;
        }

        double distanceToTarget = mc.player.getPos().distanceTo(target.getPos());

        // Проверяем дистанцию для обмена снаряжения
        if (distanceToTarget <= swapDistance.getValue() && !hasChestplateEquipped) {
            currentState = State.SWAPPING_GEAR;
            return;
        }

        // Летим вниз к цели
        controlFlight(false);

        // Проверяем возможность атаки
        if (distanceToTarget <= attackRange.getValue() && mc.player.isOnGround()) {
            currentState = State.ATTACKING;
        }
    }

    private void handleSwappingGearState() {
        // Снимаем элитру, надеваем нагрудник
        if (!hasChestplateEquipped) {
            swapToChestplate();
            hasChestplateEquipped = true;
            hasMaceEquipped = false;
        }

        // Берем булаву
        if (!hasMaceEquipped) {
            swapToMace();
            hasMaceEquipped = true;
        }

        currentState = State.ATTACKING;
    }

    private void handleAttackingState() {
        if (target == null || !target.isAlive()) {
            resetState();
            return;
        }

        // Проверяем задержку между атаками
        if (System.currentTimeMillis() - lastAttackTime < (long) attackDelay.getValue()) {
            return;
        }

        // Поворачиваемся к цели
        if (rotateToTarget.get()) {
            rotateToTarget(target);
        }

        // Атакуем булавой
        attackWithMace();
        lastAttackTime = System.currentTimeMillis();

        // Проверяем, нужно ли ломать щит
        if (target instanceof LivingEntity livingTarget &&
                livingTarget.isUsingItem() &&
                livingTarget.getActiveItem().isOf(Items.SHIELD)) {

            if (shieldBreak.get()) {
                shieldBreak(livingTarget);
            }
        }

        // Переходим в следующее состояние
        if (keepElytra.get()) {
            currentState = State.RETURNING;
        } else {
            resetState();
        }
    }

    private void handleReturningState() {
        // Возвращаем элитру
        if (hasChestplateEquipped) {
            swapToElytra();
            hasChestplateEquipped = false;
            hasMaceEquipped = false;
        }

        // Снова взлетаем
        currentState = State.FLYING_UP;
    }

    private void resetState() {
        target = null;
        currentState = State.IDLE;
        targetPosition = null;
        hasChestplateEquipped = false;
        hasMaceEquipped = false;

        // Возвращаем элитру если нужно
        if (!mc.player.isGliding() && keepElytra.get()) {
            int elytraSlot = findItemSlot(Items.ELYTRA);
            if (elytraSlot != -1) {
                swapToChestSlot(elytraSlot);
            }
        }
    }

    // Методы для работы со щитами (из KillAura)
    private void handleShieldBreak() {
        if (!shieldBreak.get() && !shieldPush.get()) return;
        if (shieldBreakCooldown > 0) return;

        if (target instanceof LivingEntity livingTarget &&
                livingTarget.isUsingItem() &&
                livingTarget.getActiveItem().isOf(Items.SHIELD)) {

            if (shieldBreak.get()) {
                shieldBreak(livingTarget);
            }
        }
    }

    private void shieldBreak(LivingEntity target) {
        if (!target.isUsingItem() || !target.getActiveItem().isOf(Items.SHIELD)) return;

        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }

        if (axeSlot == -1) {
            if (shieldPush.get() && mc.interactionManager != null && target.isAlive()) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
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
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (originalSlot != axeSlot) {
            mc.player.getInventory().selectedSlot = originalSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(originalSlot));
        }

        shieldBreakCooldown = 3;
    }

    // Вспомогательные методы
    private Entity findTarget() {
        List<Entity> entities = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            if (!isValidTargetType(entity)) continue;

            double distance = mc.player.squaredDistanceTo(entity);
            double maxDistance = attackRange.getValue() * 2;
            if (distance > maxDistance * maxDistance) continue;

            if (checkObstacles.get() && hasObstacleBetween(entity)) continue;

            if (entity instanceof PlayerEntity player) {
                BooleanSetting friendsSetting = targetTypeSetting.getValueByName("Друзья");
                if (friendsSetting != null && !friendsSetting.get() &&
                        FriendManager.getInstance().isFriend(player.getName().getString())) {
                    continue;
                }
            }

            entities.add(entity);
        }

        if (entities.isEmpty()) return null;

        return entities.stream()
                .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
                .orElse(null);
    }

    private boolean isValidTargetType(Entity entity) {
        BooleanSetting players = targetTypeSetting.getValueByName("Игроки");
        BooleanSetting mobs = targetTypeSetting.getValueByName("Мобы");
        BooleanSetting animals = targetTypeSetting.getValueByName("Животные");

        if (entity instanceof PlayerEntity) {
            return players != null && players.get();
        } else if (entity instanceof net.minecraft.entity.mob.HostileEntity) {
            return mobs != null && mobs.get();
        } else if (entity instanceof net.minecraft.entity.passive.AnimalEntity) {
            return animals != null && animals.get();
        }
        return false;
    }

    private boolean hasObstacleBetween(Entity entity) {
        Vec3d playerEyes = mc.player.getCameraPosVec(1.0f);
        Vec3d targetEyes = entity.getCameraPosVec(1.0f);

        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                playerEyes,
                targetEyes,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() != net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private void startGliding() {
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        if (mc.player.getVelocity().y < 0) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    mc.player,
                    ClientCommandC2SPacket.Mode.START_FALL_FLYING
            ));
        }
    }

    private void useFirework() {
        int fireworkSlot = findItemSlot(Items.FIREWORK_ROCKET);
        if (fireworkSlot == -1) return;

        int currentSlot = mc.player.getInventory().selectedSlot;

        if (currentSlot != fireworkSlot) {
            mc.player.getInventory().selectedSlot = fireworkSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(fireworkSlot));
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        if (currentSlot != fireworkSlot) {
            mc.player.getInventory().selectedSlot = currentSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(currentSlot));
        }
    }

    private void controlFlight(boolean goUp) {
        if (!mc.player.isGliding()) return;

        if (goUp) {
            mc.options.forwardKey.setPressed(true);
            mc.options.sneakKey.setPressed(false);
        } else {
            mc.options.forwardKey.setPressed(true);
            mc.options.sneakKey.setPressed(true);
        }

        if (targetPosition != null && rotateToTarget.get()) {
            Vec3d direction = targetPosition.subtract(mc.player.getPos()).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            float targetPitch = (float) Math.toDegrees(Math.asin(-direction.y));

            mc.player.setYaw(targetYaw);
            mc.player.setPitch(targetPitch);
        }
    }

    private void swapToChestplate() {
        int chestplateSlot = findChestplateSlot();
        if (chestplateSlot != -1) {
            swapToChestSlot(chestplateSlot);
        }
    }

    private void swapToMace() {
        int maceSlot = findItemSlot(Items.MACE);
        if (maceSlot != -1 && maceSlot < 9) {
            mc.player.getInventory().selectedSlot = maceSlot;
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(maceSlot));
        }
    }

    private void swapToElytra() {
        int elytraSlot = findItemSlot(Items.ELYTRA);
        if (elytraSlot != -1) {
            swapToChestSlot(elytraSlot);
        }
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findChestplateSlot() {
        Item[] chestplates = {
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
        };

        for (Item chestplate : chestplates) {
            int slot = findItemSlot(chestplate);
            if (slot != -1) return slot;
        }

        return -1;
    }

    private void swapToChestSlot(int slot) {
        int chestSlot = 6;

        if (slot < 0 || slot >= mc.player.getInventory().size()) return;

        int fromSlot = slot < 9 ? slot + 36 : slot;

        if (mc.interactionManager != null) {
            mc.interactionManager.clickSlot(0, fromSlot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, chestSlot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, fromSlot, 0, SlotActionType.SWAP, mc.player);
        }
    }

    private void rotateToTarget(Entity target) {
        Vec3d eyesPos = mc.player.getCameraPosVec(1.0f);
        Vec3d targetPos = target.getBoundingBox().getCenter();
        Vec3d direction = targetPos.subtract(eyesPos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void attackWithMace() {
        if (mc.interactionManager != null && target != null && target.isAlive()) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void debugInfo() {
        if (!debugInfo.get()) return;

        String stateStr = "Состояние: " + currentState;
        String targetStr = "Цель: " + (target != null ? target.getName().getString() : "Нет");
        String heightStr = String.format("Высота: %.1f", mc.player.getY());

        if (mc.player.age % 20 == 0) {
            System.out.println(stateStr + " | " + targetStr + " | " + heightStr);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();

        mc.options.forwardKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }
}