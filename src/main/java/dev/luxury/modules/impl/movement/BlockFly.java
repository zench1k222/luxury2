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
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleAnnotation(name = "BlockFly", category = Category.Movement, desc = "Ставит блоки под собой для полёта как Scaffold")
public final class BlockFly extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка", 2, 0, 10, 1);
    private final SliderSetting speedMultiplier = new SliderSetting("Множитель скорости", 0.7, 0.1, 1.0, 0.1);
    private final BooleanSetting autoJump = new BooleanSetting("Авто-прыжок", true);
    private final BooleanSetting safeMode = new BooleanSetting("Безопасный режим", true);
    private final BooleanSetting tower = new BooleanSetting("Башня (вверх)", true);
    private final BooleanSetting forward = new BooleanSetting("Вперёд", true);
    private final BooleanSetting sneaky = new BooleanSetting("Скрытное размещение", false);
    private final BooleanSetting keepBlocks = new BooleanSetting("Не ломать блоки", true);
    private final SliderSetting keepTicks = new SliderSetting("Задержка ломания", 5, 0, 20, 1);

    private int tickCounter = 0;
    private BlockPos lastPlacedBlock = null;
    private final Set<BlockPos> placedBlocks = new HashSet<>();
    private final ConcurrentLinkedQueue<BlockPos> blocksToRemove = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> removalTimers = new ConcurrentLinkedQueue<>();
    private int blockSlot = -1;
    private boolean wasJumping = false;
    private double originalSpeed = 0.0;

    public BlockFly() {
        addSettings(delay, speedMultiplier, autoJump, safeMode, tower, forward, sneaky, keepBlocks, keepTicks);
    }

    @EventTarget
    public void onEvent(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Уменьшаем скорость игрока
        if (originalSpeed == 0.0) {
            originalSpeed = mc.player.getAbilities().getWalkSpeed();
        }
        mc.player.getAbilities().setWalkSpeed((float) (originalSpeed * speedMultiplier.getValue()));

        // Обработка отложенного ломания блоков
        processDelayedBreaking();

        tickCounter++;
        if (tickCounter < delay.getIntValue()) return;
        tickCounter = 0;

        // Находим блок
        if (blockSlot == -1) {
            blockSlot = findBlockSlot();
            if (blockSlot == -1) return;
        }

        // Определяем направление движения
        Vec3d movement = mc.player.getVelocity();
        boolean isMovingForward = Math.abs(movement.x) > 0.1 || Math.abs(movement.z) > 0.1;
        boolean isJumping = mc.player.jumping;

        // Логика размещения блоков
        if (tower.get() && isJumping && !wasJumping) {
            // Режим башни вверх
            placeTowerBlocks();
        } else if (forward.get() && isMovingForward) {
            // Режим движения вперёд (как Scaffold)
            placeForwardBlocks();
        } else {
            // Обычное размещение под ногами
            placeUnderFeet();
        }

        wasJumping = isJumping;

        // Управление поставленными блоками
        managePlacedBlocks();
    }

    private void placeTowerBlocks() {
        BlockPos currentPos = mc.player.getBlockPos();
        BlockPos placePos = currentPos.down();

        // Ставим блок под собой для начала прыжка
        if (mc.world.getBlockState(placePos).isAir()) {
            placeBlock(placePos);
        }

        // Ставим блок на уровень выше для следующего прыжка
        BlockPos abovePos = currentPos.up();
        if (mc.world.getBlockState(abovePos).isAir()) {
            placeBlock(abovePos);
        }

        // Авто-прыжок
        if (autoJump.get()) {
            mc.player.jump();
        }
    }

    private void placeForwardBlocks() {
        Vec3d playerPos = mc.player.getPos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);

        // Предсказываем следующую позицию
        Vec3d nextPos = playerPos.add(lookVec.multiply(0.5));
        BlockPos targetPos = new BlockPos(
                (int) Math.floor(nextPos.x),
                (int) Math.floor(playerPos.y - 1),
                (int) Math.floor(nextPos.z)
        );

        // Ставим блок впереди под собой
        if (mc.world.getBlockState(targetPos).isAir()) {
            placeBlock(targetPos);
        }

        // Также ставим блок прямо под ногами для безопасности
        BlockPos underPos = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(underPos).isAir()) {
            placeBlock(underPos);
        }

        // Медленное движение вперёд
        if (mc.player.isOnGround()) {
            mc.player.setVelocity(mc.player.getVelocity().multiply(0.7, 1.0, 0.7));
        }
    }

    private void placeUnderFeet() {
        BlockPos feetPos = mc.player.getBlockPos();
        BlockPos[] positionsToCheck = {
                feetPos.down(),
                feetPos.down().north(),
                feetPos.down().south(),
                feetPos.down().east(),
                feetPos.down().west(),
                feetPos.down().north().east(),
                feetPos.down().north().west(),
                feetPos.down().south().east(),
                feetPos.down().south().west()
        };

        for (BlockPos pos : positionsToCheck) {
            if (mc.world.getBlockState(pos).isAir()) {
                if (placeBlock(pos)) {
                    break;
                }
            }
        }
    }

    private boolean placeBlock(BlockPos pos) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = blockSlot;

        boolean placed = false;

        // Проверяем все стороны для размещения
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighborPos);

            if (!neighborState.isAir() && neighborState.isSolidBlock(mc.world, neighborPos)) {
                // Для скрытного размещения используем направление вниз
                Direction placeDirection = sneaky.get() ? Direction.DOWN : direction.getOpposite();

                Vec3d hitVec = new Vec3d(
                        neighborPos.getX() + 0.5 + placeDirection.getOffsetX() * 0.5,
                        neighborPos.getY() + 0.5 + placeDirection.getOffsetY() * 0.5,
                        neighborPos.getZ() + 0.5 + placeDirection.getOffsetZ() * 0.5
                );

                BlockHitResult hitResult = new BlockHitResult(
                        hitVec,
                        placeDirection,
                        neighborPos,
                        false
                );

                ActionResult result = mc.interactionManager.interactBlock(
                        mc.player,
                        Hand.MAIN_HAND,
                        hitResult
                );

                if (result.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    lastPlacedBlock = pos;
                    placedBlocks.add(pos);

                    // Если включено "Не ломать блоки", добавляем в очередь для отложенного ломания
                    if (keepBlocks.get() && !safeMode.get()) {
                        scheduleBlockRemoval(pos);
                    }

                    placed = true;
                    break;
                }
            }
        }

        mc.player.getInventory().selectedSlot = previousSlot;
        return placed;
    }

    private void scheduleBlockRemoval(BlockPos pos) {
        blocksToRemove.add(pos);
        removalTimers.add((int) keepTicks.getValue());
    }

    private void processDelayedBreaking() {
        if (blocksToRemove.isEmpty()) return;

        ConcurrentLinkedQueue<BlockPos> tempBlocks = new ConcurrentLinkedQueue<>(blocksToRemove);
        ConcurrentLinkedQueue<Integer> tempTimers = new ConcurrentLinkedQueue<>(removalTimers);

        blocksToRemove.clear();
        removalTimers.clear();

        int index = 0;
        for (BlockPos pos : tempBlocks) {
            int timer = 0;
            for (int i = 0; i < index; i++) {
                tempTimers.poll();
            }
            timer = tempTimers.peek() != null ? tempTimers.peek() : 0;

            if (timer <= 0) {
                // Ломаем блок с задержкой пакета
                if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    // Отправляем пакет начала ломания
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                            pos,
                            Direction.UP
                    ));

                    // Ждём 1 тик
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Отправляем пакет окончания ломания
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                            pos,
                            Direction.UP
                    ));
                }
                placedBlocks.remove(pos);
            } else {
                blocksToRemove.add(pos);
                removalTimers.add(timer - 1);
            }
            index++;
        }
    }

    private void managePlacedBlocks() {
        // Удаляем блоки, которые слишком далеко
        placedBlocks.removeIf(pos -> {
            double distance = mc.player.getPos().distanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
            return distance > 10;
        });

        // В безопасном режиме не удаляем блоки
        if (!safeMode.get() && !keepBlocks.get()) {
            // Удаляем блоки, на которых мы больше не стоим
            if (lastPlacedBlock != null) {
                Vec3d playerPos = mc.player.getPos();
                BlockPos currentUnder = new BlockPos(
                        (int) Math.floor(playerPos.x),
                        (int) Math.floor(playerPos.y - 0.5),
                        (int) Math.floor(playerPos.z)
                );

                if (!currentUnder.equals(lastPlacedBlock) &&
                        !currentUnder.equals(lastPlacedBlock.up())) {

                    if (mc.world.getBlockState(lastPlacedBlock).getBlock() != Blocks.AIR) {
                        scheduleBlockRemoval(lastPlacedBlock);
                    }
                    lastPlacedBlock = null;
                }
            }
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem && !stack.isEmpty()) {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (isValidBlock(block)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isValidBlock(Block block) {
        if (block == Blocks.AIR ||
                block == Blocks.WATER ||
                block == Blocks.LAVA ||
                block == Blocks.FIRE ||
                block == Blocks.SOUL_FIRE ||
                block == Blocks.SAND ||
                block == Blocks.GRAVEL) {
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tickCounter = 0;
        lastPlacedBlock = null;
        placedBlocks.clear();
        blocksToRemove.clear();
        removalTimers.clear();
        blockSlot = -1;
        wasJumping = false;
        originalSpeed = 0.0;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (originalSpeed > 0) {
            mc.player.getAbilities().setWalkSpeed((float) originalSpeed);
        }

        if (!safeMode.get()) {
            for (BlockPos pos : placedBlocks) {
                if (!mc.world.getBlockState(pos).isAir()) {
                    mc.interactionManager.attackBlock(pos, Direction.UP);
                }
            }
        }

        placedBlocks.clear();
        blocksToRemove.clear();
        removalTimers.clear();
        lastPlacedBlock = null;
        blockSlot = -1;
    }
}