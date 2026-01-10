package dev.luxury.modules.impl.movement;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Overwrite;

@ModuleAnnotation(name = "Scaffold", category = Category.Movement, desc = "Ставит блоки под ногами при движении")
public final class BlockFly extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка", 0, 0, 10, 1);
    private final BooleanSetting tower = new BooleanSetting("Башня", false);
    private final BooleanSetting safe = new BooleanSetting("Безопасный режим", true);
    private final BooleanSetting airPlace = new BooleanSetting("Воздушная установка", true);
    private final BooleanSetting keepY = new BooleanSetting("Сохранять высоту", true);
    private final SliderSetting keepYLevel = new SliderSetting("Уровень высоты", -1, -5, 5, 1);
    private final BooleanSetting swing = new BooleanSetting("Анимация руки", true);
    private final BooleanSetting checkBlock = new BooleanSetting("Проверять блоки", true);

    private int tickCounter = 0;
    private int blockSlot = -1;
    private BlockPos lastPlaced = null;
    private int lastPlaceTick = 0;

    public BlockFly() {
        addSettings(delay, tower, safe, airPlace, keepY, keepYLevel, swing, checkBlock);
    }

    @EventTarget
    public void onEvent(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        tickCounter++;
        if (tickCounter < delay.getIntValue()) return;
        tickCounter = 0;

        // Находим блок
        if (blockSlot == -1) {
            blockSlot = findBestBlockSlot();
            if (blockSlot == -1) return;
        }

        // Проверяем наличие блоков
        if (!hasBlocks(blockSlot)) {
            blockSlot = findBestBlockSlot();
            if (blockSlot == -1) return;
        }

        // Определяем позицию для установки
        BlockPos targetPos = getTargetPosition();
        if (targetPos == null) return;

        // Проверяем можно ли ставить
        if (!canPlaceAt(targetPos)) return;

        // Устанавливаем блок
        placeBlock(targetPos);

        // Режим башни
        if (tower.get() && mc.player.input.playerInput.jump()) {
            handleTower();
        }
    }

    private BlockPos getTargetPosition() {
        Vec3d playerPos = mc.player.getPos();
        BlockPos playerBlockPos = mc.player.getBlockPos();

        // Сохраняем высоту
        if (keepY.get()) {
            int targetY = playerBlockPos.getY() + keepYLevel.getIntValue();
            return new BlockPos(playerBlockPos.getX(), targetY, playerBlockPos.getZ());
        }

        // Базовая позиция под ногами
        BlockPos basePos = new BlockPos(
                (int) Math.floor(playerPos.x),
                (int) Math.floor(playerPos.y - 0.2),
                (int) Math.floor(playerPos.z)
        );

        // Если игрок движется
        if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
            // Вектор взгляда
            Vec3d lookVec = mc.player.getRotationVec(1.0f);
            Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z).normalize();

            // Предсказанная позиция
            float speed = mc.player.input.movementForward > 0 ? 1.0f : -1.0f;
            Vec3d predictedPos = playerPos.add(horizontalLook.multiply(speed * 0.5));

            BlockPos predictedBlockPos = new BlockPos(
                    (int) Math.floor(predictedPos.x),
                    (int) Math.floor(predictedPos.y - 0.2),
                    (int) Math.floor(predictedPos.z)
            );

            // Проверяем сначала предсказанную позицию
            if (isValidPlacement(predictedBlockPos)) {
                return predictedBlockPos;
            }
        }

        // Проверяем блоки вокруг
        BlockPos[] checkOrder = {
                basePos,
                basePos.north(),
                basePos.south(),
                basePos.east(),
                basePos.west(),
                basePos.north().east(),
                basePos.north().west(),
                basePos.south().east(),
                basePos.south().west()
        };

        for (BlockPos pos : checkOrder) {
            if (isValidPlacement(pos)) {
                return pos;
            }
        }

        return null;
    }

    private boolean isValidPlacement(BlockPos pos) {
        if (pos == null) return false;

        // Проверяем расстояние
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
        if (distance > 4.5) return false;

        // Проверяем что блок воздушный
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isAir()) return false;

        // Проверка на воздушную установку
        if (airPlace.get()) return true;

        // Ищем соседний блок для установки
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighbor);
            if (!neighborState.isAir() && neighborState.isSolidBlock(mc.world, neighbor)) {
                return true;
            }
        }

        return false;
    }

    private boolean canPlaceAt(BlockPos pos) {
        if (pos == null) return false;

        // Не ставим слишком часто на одно место
        if (lastPlaced != null && lastPlaced.equals(pos)) {
            if (mc.player.age - lastPlaceTick < 3) {
                return false;
            }
        }

        // Проверяем что блок еще воздушный
        return mc.world.getBlockState(pos).isAir();
    }

    private void placeBlock(BlockPos pos) {
        // Находим сторону для установки
        Direction placeSide = findPlaceSide(pos);
        if (placeSide == null && !airPlace.get()) {
            return;
        }

        // Если не нашли сторону и включена воздушная установка
        if (placeSide == null) {
            placeSide = Direction.UP;
        }

        BlockPos neighborPos = pos.offset(placeSide.getOpposite());

        // Сохраняем текущий слот
        int originalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = blockSlot;

        try {
            // Вычисляем точку попадания
            Vec3d hitVec = Vec3d.ofCenter(neighborPos)
                    .add(placeSide.getOffsetX() * 0.5,
                            placeSide.getOffsetY() * 0.5,
                            placeSide.getOffsetZ() * 0.5);

            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    placeSide,
                    neighborPos,
                    false
            );

            // Устанавливаем блок
            ActionResult result = mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    hitResult
            );

            if (result.isAccepted()) {
                if (swing.get()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                lastPlaced = pos;
                lastPlaceTick = mc.player.age;
            }
        } finally {
            // Возвращаем слот
            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }

    private Direction findPlaceSide(BlockPos pos) {
        // Приоритетные направления (сверху надежнее)
        Direction[] priorityOrder = {
                Direction.DOWN,    // Установка сверху - самый надежный
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                Direction.UP       // Установка снизу
        };

        for (Direction dir : priorityOrder) {
            BlockPos neighbor = pos.offset(dir.getOpposite());
            BlockState neighborState = mc.world.getBlockState(neighbor);

            // Проверка на валидность блока
            if (checkBlock.get() && !isValidBlock(neighborState.getBlock())) {
                continue;
            }

            if (!neighborState.isAir() && neighborState.isSolidBlock(mc.world, neighbor)) {
                return dir;
            }
        }

        return null;
    }

    private boolean isValidBlock(Block block) {
        if (block == null) return false;

        // Исключаем неподходящие блоки
        return block != Blocks.AIR &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.FIRE &&
                block != Blocks.SOUL_FIRE;
    }

    private void handleTower() {
        if (!mc.player.isOnGround()) return;

        BlockPos belowPos = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(belowPos).isAir() && canPlaceAt(belowPos)) {
            placeBlock(belowPos);
        }

        // Автоматический прыжок
        mc.player.jump();
    }

    private int findBestBlockSlot() {
        int bestSlot = -1;
        int bestCount = 0;

        // Сначала проверяем хотбар
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValidBlockStack(stack)) {
                int count = stack.getCount();
                if (count > bestCount) {
                    bestCount = count;
                    bestSlot = i;
                }
            }
        }

        // Если не нашли в хотбаре, ищем во всем инвентаре
        if (bestSlot == -1) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isValidBlockStack(stack)) {
                    bestSlot = i;
                    break;
                }
            }
        }

        return bestSlot;
    }

    private boolean isValidBlockStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) stack.getItem()).getBlock();
        return isValidBlock(block);
    }

    private boolean hasBlocks(int slot) {
        if (slot < 0 || slot >= 36) return false;
        ItemStack stack = mc.player.getInventory().getStack(slot);
        return isValidBlockStack(stack) && !stack.isEmpty();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tickCounter = 0;
        blockSlot = -1;
        lastPlaced = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Сбрасываем все
        blockSlot = -1;
        lastPlaced = null;
    }

    @Overwrite
    public String getSuffix() {
        if (blockSlot != -1 && mc.player != null) {
            ItemStack stack = mc.player.getInventory().getStack(blockSlot);
            if (!stack.isEmpty()) {
                return stack.getName().getString();
            }
        }
        return "";
    }
}