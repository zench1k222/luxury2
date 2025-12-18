package dev.luxury.modules;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.Arrays;
import java.util.List;

@ModuleAnnotation(
        name = "AutoFarm",
        desc = "АвтоФарм для АресМайна без Baritone",
        category = Category.Misc
)
public class AutoFarm extends Module {

    private final Vec3d FARM_CENTER = new Vec3d(-35, 50, 49);

    private final List<TreeData> TREES = Arrays.asList(
            new TreeData(-30, 42, 46, Blocks.SPRUCE_LOG),
            new TreeData(-16, 37, 41, Blocks.BIRCH_LOG),
            new TreeData(-2,  41, 45, Blocks.CHERRY_LOG),
            new TreeData(12,  41, 45, Blocks.OAK_LOG),
            new TreeData(26,  41, 45, Blocks.MANGROVE_LOG)
    );

    private State state = State.GOING_TO_FARM;
    private int currentTreeIndex = TREES.size() - 1;
    private int currentBlockIndex = 0;
    private int waitTicks = 0;
    private boolean reversing = false;

    private enum State {
        GOING_TO_FARM,
        MINING_TREE,
        WAITING,
        FINISHED
    }

    @Override
    public void onEnable() {
        super.onEnable();
        state = State.GOING_TO_FARM;
        currentTreeIndex = TREES.size() - 1;
        currentBlockIndex = 0;
        waitTicks = 0;
        reversing = false;

        System.out.println("[AutoFarm] Запущен. Деревьев: " + TREES.size());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[AutoFarm] Остановлен");
    }

    @EventTarget
    public void onTick(dev.luxury.events.impl.client.EventTick e) {
        if (mc.player == null || mc.world == null) return;

        switch (state) {
            case GOING_TO_FARM:
                handleGoingToFarm();
                break;

            case MINING_TREE:
                handleMiningTree();
                break;

            case WAITING:
                handleWaiting();
                break;

            case FINISHED:
                System.out.println("[AutoFarm] Все деревья сломаны!");
                toggle();
                break;
        }
    }

    private void handleGoingToFarm() {
        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(FARM_CENTER);

        if (distance < 3.0) {
            System.out.println("[AutoFarm] Прибыли на ферму. Начинаем майнить...");
            state = State.MINING_TREE;
            startMiningCurrentTree();
            return;
        }

        moveToPosition(FARM_CENTER);

        lookAtPosition(FARM_CENTER);
    }

    private void handleMiningTree() {
        if (currentTreeIndex < 0) {
            state = State.FINISHED;
            return;
        }

        TreeData tree = TREES.get(currentTreeIndex);

        int currentZ = reversing ?
                (tree.endZ - currentBlockIndex) :
                (tree.startZ + currentBlockIndex);

        BlockPos targetPos = new BlockPos(tree.x, 51, currentZ);

        if (!isTreeBlock(targetPos, tree.blockType)) {
            advanceToNextBlock();
            return;
        }

        if (breakBlock(targetPos)) {
            waitTicks = 10;
            state = State.WAITING;
            advanceToNextBlock();
        } else {
            moveToPosition(new Vec3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5));
            lookAtPosition(new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5));
        }
    }

    private void advanceToNextBlock() {
        TreeData tree = TREES.get(currentTreeIndex);

        currentBlockIndex++;

        int totalBlocks = Math.abs(tree.endZ - tree.startZ) + 1;
        if (currentBlockIndex >= totalBlocks) {
            currentTreeIndex--;
            currentBlockIndex = 0;

            if (currentTreeIndex >= 0) {
                System.out.println("[AutoFarm] Переходим к дереву #" + (currentTreeIndex + 1));
                startMiningCurrentTree();
            } else {
                state = State.FINISHED;
            }
        }
    }

    private void startMiningCurrentTree() {
        if (currentTreeIndex < 0) return;

        TreeData tree = TREES.get(currentTreeIndex);
        System.out.println("[AutoFarm] Майним " + tree.blockType.getName().getString() +
                " с Z=" + tree.endZ + " до Z=" + tree.startZ);

        reversing = true;
        currentBlockIndex = 0;
        state = State.MINING_TREE;
    }

    private void handleWaiting() {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        state = State.MINING_TREE;
    }

    private boolean isTreeBlock(BlockPos pos, Block expectedBlock) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == expectedBlock;
    }

    private boolean breakBlock(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;

        double distance = mc.player.getPos().distanceTo(new Vec3d(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
        ));

        if (distance > 5.0) {
            return false;
        }

        lookAtPosition(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));

        mc.interactionManager.attackBlock(pos, net.minecraft.util.math.Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);

        return true;
    }

    private void moveToPosition(Vec3d target) {
        if (mc.player == null) return;

        Vec3d direction = target.subtract(mc.player.getPos()).normalize();

        mc.options.forwardKey.setPressed(true);

        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void lookAtPosition(Vec3d target) {
        if (mc.player == null) return;

        Vec3d eyes = mc.player.getCameraPosVec(1.0F);
        Vec3d direction = target.subtract(eyes).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private static class TreeData {
        final int x;
        final int startZ;
        final int endZ;
        final Block blockType;

        TreeData(int x, int startZ, int endZ, Block blockType) {
            this.x = x;
            this.startZ = startZ;
            this.endZ = endZ;
            this.blockType = blockType;
        }
    }
}