package dev.luxury.modules.impl.misc;

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
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Overwrite;

@ModuleAnnotation(name = "AirBlock", category = Category.Misc, desc = "Позволяет ставить блоки в воздухе без опоры")
public final class AirBlock extends Module {

    private final SliderSetting range = new SliderSetting("Дистанция", 4.5, 1, 6, 0.1);
    private final SliderSetting delay = new SliderSetting("Задержка", 0, 0, 20, 1);
    private final BooleanSetting swing = new BooleanSetting("Анимация руки", true);
    private final BooleanSetting checkBlock = new BooleanSetting("Проверять блок", true);
    private final BooleanSetting onlyWhileMoving = new BooleanSetting("Только при движении", false);
    private final SliderSetting angle = new SliderSetting("Угол установки", 90, 0, 180, 1);

    private int tickCounter = 0;
    private int blockSlot = -1;
    private BlockPos lastPlaced = null;
    private int lastPlaceTick = 0;

    public AirBlock() {
        addSettings(range, delay, swing, checkBlock, onlyWhileMoving, angle);
    }

    @EventTarget
    public void onEvent(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Проверка на движение
        if (onlyWhileMoving.get() &&
                mc.player.input.movementForward == 0 &&
                mc.player.input.movementSideways == 0) {
            return;
        }

        tickCounter++;
        if (tickCounter < delay.getIntValue()) return;
        tickCounter = 0;

        // Находим блок в инвентаре
        if (blockSlot == -1) {
            blockSlot = findBestBlockSlot();
            if (blockSlot == -1) return;
        }

        // Проверяем наличие блоков
        if (!hasBlocks(blockSlot)) {
            blockSlot = findBestBlockSlot();
            if (blockSlot == -1) return;
        }

        // Получаем позицию для установки
        BlockPos targetPos = getTargetPosition();
        if (targetPos == null) return;

        // Проверяем можно ли ставить
        if (!canPlaceAt(targetPos)) return;

        // Устанавливаем блок
        placeBlockInAir(targetPos);
    }

    private BlockPos getTargetPosition() {
        // Получаем позицию взгляда
        Vec3d startPos = mc.player.getCameraPosVec(1.0f);
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        // Максимальная дистанция
        double maxDistance = range.getValue();

        // Рассчитываем конечную точку
        Vec3d endPos = startPos.add(lookVec.multiply(maxDistance));

        // Находим ближайшую позицию блока на линии взгляда
        return findBlockPositionOnRay(startPos, endPos);
    }

    private BlockPos findBlockPositionOnRay(Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        Vec3d direction = end.subtract(start).normalize();

        // Проверяем точки на луче с шагом 0.1
        for (double d = 0.1; d <= distance; d += 0.1) {
            Vec3d point = start.add(direction.multiply(d));
            BlockPos blockPos = new BlockPos(
                    (int) Math.floor(point.x),
                    (int) Math.floor(point.y),
                    (int) Math.floor(point.z)
            );

            // Проверяем, что блок воздушный
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.isAir()) {
                // Проверяем угол установки
                if (isValidAngle(blockPos)) {
                    return blockPos;
                }
            }
        }

        return null;
    }

    private boolean isValidAngle(BlockPos pos) {
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d direction = blockCenter.subtract(playerPos).normalize();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        // Рассчитываем угол между взглядом и направлением на блок
        double dot = lookVec.dotProduct(direction);
        double lookLength = lookVec.length();
        double dirLength = direction.length();

        double angleRad = Math.acos(dot / (lookLength * dirLength));
        double angleDeg = Math.toDegrees(angleRad);

        // Проверяем угол
        return angleDeg <= angle.getValue();
    }

    private boolean canPlaceAt(BlockPos pos) {
        if (pos == null) return false;

        // Проверяем расстояние
        double distance = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        if (distance > range.getValue()) return false;

        // Проверяем что блок воздушный
        if (!mc.world.getBlockState(pos).isAir()) return false;

        // Проверка на частую установку
        if (lastPlaced != null && lastPlaced.equals(pos)) {
            if (mc.player.age - lastPlaceTick < 2) {
                return false;
            }
        }

        return true;
    }

    private void placeBlockInAir(BlockPos pos) {
        // Создаем фиктивный BlockHitResult
        // Используем DOWN как направление для установки сверху
        Vec3d hitVec = Vec3d.ofCenter(pos).subtract(0, 0.5, 0);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                Direction.DOWN,
                pos.down(), // Фиктивный соседний блок
                false
        );

        // Сохраняем текущий слот
        int originalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = blockSlot;

        boolean wasSneaking = mc.player.input.playerInput.sneak();

        try {
            // Включаем приседание для лучшей совместимости
            mc.player.input.playerInput = new net.minecraft.util.PlayerInput(
                    mc.player.input.playerInput.forward(),
                    mc.player.input.playerInput.backward(),
                    mc.player.input.playerInput.left(),
                    mc.player.input.playerInput.right(),
                    mc.player.input.playerInput.jump(),
                    true, // sneak
                    mc.player.input.playerInput.sprint()
            );

            // Отправляем пакет взаимодействия с блоком
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    Hand.MAIN_HAND,
                    hitResult,
                    0
            ));

            // Также вызываем стандартное взаимодействие
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
            // Восстанавливаем состояние
            mc.player.input.playerInput = new net.minecraft.util.PlayerInput(
                    mc.player.input.playerInput.forward(),
                    mc.player.input.playerInput.backward(),
                    mc.player.input.playerInput.left(),
                    mc.player.input.playerInput.right(),
                    mc.player.input.playerInput.jump(),
                    wasSneaking,
                    mc.player.input.playerInput.sprint()
            );

            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }

    private int findBestBlockSlot() {
        int bestSlot = -1;
        int bestCount = 0;

        // Сначала проверяем выбранный слот
        int currentSlot = mc.player.getInventory().selectedSlot;
        ItemStack currentStack = mc.player.getInventory().getStack(currentSlot);
        if (isValidBlockStack(currentStack)) {
            return currentSlot;
        }

        // Проверяем хотбар
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

        return bestSlot;
    }

    private boolean isValidBlockStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) stack.getItem()).getBlock();

        if (checkBlock.get()) {
            return isValidBlock(block);
        }

        return true;
    }

    private boolean isValidBlock(Block block) {
        if (block == null) return false;

        // Исключаем блоки, которые не имеют физической формы или вызывают проблемы
        return block != Blocks.AIR &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.FIRE &&
                block != Blocks.SOUL_FIRE &&
                block != Blocks.LIGHT &&
                block != Blocks.STRUCTURE_VOID &&
                block != Blocks.COMMAND_BLOCK &&
                block != Blocks.CHAIN_COMMAND_BLOCK &&
                block != Blocks.REPEATING_COMMAND_BLOCK;
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
        lastPlaceTick = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
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