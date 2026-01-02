package dev.luxury.modules.impl.other;

import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.player.ServerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
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
    private int currentZ = 0;
    private int waitTicks = 0;
    private int cycleCount = 0;
    private boolean restarting = false;

    private enum State {
        GOING_TO_FARM,
        APPROACHING_TREE,
        MINING_BLOCK,
        WAITING,
        CHECKING_NEXT,
        FINISHED_CYCLE
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!ServerUtil.isConnected("aresmine")) {
            ChatUtil.sendError("Работает только на АресМайне");
            disable();
        }
        startNewCycle();
        System.out.println("[AutoFarm] Запущен. Цикл #" + (cycleCount + 1));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopMovement();
        System.out.println("[AutoFarm] Остановлен");
    }

    @EventTarget
    public void onTick(dev.luxury.events.impl.client.EventTick e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (state != State.GOING_TO_FARM && state != State.APPROACHING_TREE) {
            stopMovement();
        }

        switch (state) {
            case GOING_TO_FARM:
                handleGoingToFarm();
                break;
            case APPROACHING_TREE:
                handleApproachingTree();
                break;
            case MINING_BLOCK:
                handleMiningBlock();
                break;
            case WAITING:
                handleWaiting();
                break;
            case CHECKING_NEXT:
                handleCheckingNext();
                break;
            case FINISHED_CYCLE:
                handleFinishedCycle();
                break;
        }
    }

    private void startNewCycle() {
        state = State.GOING_TO_FARM;
        currentTreeIndex = TREES.size() - 1;
        currentZ = 0;
        waitTicks = 0;
        restarting = false;
        cycleCount++;
    }

    private void handleGoingToFarm() {
        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(FARM_CENTER);

        if (distance < 2.0) {
            System.out.println("[AutoFarm] Прибыли на ферму");
            state = State.APPROACHING_TREE;
            prepareCurrentTree();
            return;
        }

        moveToPosition(FARM_CENTER, 0.2);
        lookAtPosition(FARM_CENTER);
    }

    private void prepareCurrentTree() {
        if (currentTreeIndex < 0) {
            state = State.FINISHED_CYCLE;
            return;
        }

        TreeData tree = TREES.get(currentTreeIndex);
        currentZ = tree.endZ;

        System.out.println("[AutoFarm] Готовим дерево: " + tree.blockType.getName().getString() +
                " Z=" + tree.endZ + "→" + tree.startZ);

        state = State.APPROACHING_TREE;
    }

    private void handleApproachingTree() {
        TreeData tree = TREES.get(currentTreeIndex);
        BlockPos targetPos = new BlockPos(tree.x, 51, currentZ);

        if (!isTreeBlock(targetPos, tree.blockType)) {
            System.out.println("[AutoFarm] Блок уже сломан: " + targetPos);
            advanceToNextBlock();
            return;
        }

        Vec3d blockCenter = new Vec3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        double distance = mc.player.getPos().distanceTo(blockCenter);

        if (distance > 3.0) {
            moveToPosition(blockCenter, 0.15);
            lookAtPosition(new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5));
        } else {
            stopMovement();
            state = State.MINING_BLOCK;
        }
    }

    private void handleMiningBlock() {
        TreeData tree = TREES.get(currentTreeIndex);
        BlockPos targetPos = new BlockPos(tree.x, 51, currentZ);

        lookAtPosition(new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5));

        if (isLookingAtBlock(targetPos)) {
            mc.interactionManager.attackBlock(targetPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        System.out.println("[AutoFarm] Ломаем блок: " + targetPos);

        waitTicks = 5;
        state = State.WAITING;
    }

    private void handleWaiting() {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        state = State.CHECKING_NEXT;
    }

    private void handleCheckingNext() {
        advanceToNextBlock();
        if (state == State.APPROACHING_TREE || state == State.FINISHED_CYCLE) return;
        state = State.APPROACHING_TREE;
    }

    private void advanceToNextBlock() {
        TreeData tree = TREES.get(currentTreeIndex);
        currentZ--;

        if (currentZ < tree.startZ) {
            currentTreeIndex--;
            if (currentTreeIndex >= 0) {
                System.out.println("[AutoFarm] Переходим к следующему дереву");
                prepareCurrentTree();
            } else {
                state = State.FINISHED_CYCLE;
            }
        } else {
            System.out.println("[AutoFarm] Следующий блок Z=" + currentZ);
        }
    }

    private void handleFinishedCycle() {
        System.out.println("[AutoFarm] Цикл #" + cycleCount + " завершён");

        if (!restarting) {
            restarting = true;
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    if (isEnabled()) {
                        startNewCycle();
                        System.out.println("[AutoFarm] Перезапуск цикла #" + cycleCount);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private boolean isTreeBlock(BlockPos pos, Block expectedBlock) {
        return mc.world.getBlockState(pos).getBlock() == expectedBlock;
    }

    private void moveToPosition(Vec3d target, double speed) {
        Vec3d direction = target.subtract(mc.player.getPos()).normalize();
        mc.player.setVelocity(direction.x * speed, mc.player.getVelocity().y, direction.z * speed);
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        mc.player.setYaw(yaw);
    }

    private void stopMovement() {
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }

    private void lookAtPosition(Vec3d target) {
        Vec3d eyes = mc.player.getCameraPosVec(1.0F);
        Vec3d direction = target.subtract(eyes).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private boolean isLookingAtBlock(BlockPos pos) {
        HitResult hit = mc.crosshairTarget;
        return hit != null &&
                hit.getType() == HitResult.Type.BLOCK &&
                ((BlockHitResult)hit).getBlockPos().equals(pos);
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