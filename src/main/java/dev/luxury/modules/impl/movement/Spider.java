package dev.luxury.modules.impl.movement;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.Box;

@ModuleAnnotation(
        name = "Spider",
        desc = "Ставит сферу Андромеды или блоки для лазания по стенам",
        category = Category.Movement
)
public class Spider extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Настройки
    private final ModeSetting mode = new ModeSetting("Mode", "Selects spider mode",
            "Andromeda", new String[]{"Andromeda", "Block"});

    private final BooleanSetting flySetting = new BooleanSetting("Fly",
            "Places blocks below you for flying", false);

    private long lastPlaceTime = 0L;

    public Spider() {
        addSettings(mode, flySetting);
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Andromeda")) {
            handleAndromedaMode();
        } else if (mode.is("Block")) {
            handleBlockMode();
        }
    }

    private void handleAndromedaMode() {
        int sphereSlot = findSphereInInventory();
        if (sphereSlot == -1) return;

        ItemStack sphereStack = mc.player.getInventory().getStack(sphereSlot);
        if (!isAndromedaSphere(sphereStack)) return;

        if (mc.player.horizontalCollision) {
            mc.player.getInventory().selectedSlot = sphereSlot;
            Vec3d cameraPos = mc.player.getCameraPosVec(1.0F);
            Vec3d rotationVec = mc.player.getRotationVec(1.0F);
            Vec3d reachVec = cameraPos.add(rotationVec.multiply(5.0D));

            HitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(
                    cameraPos,
                    reachVec,
                    net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) result;
                BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());

                mc.interactionManager.interactBlock(
                        mc.player,
                        Hand.MAIN_HAND,
                        new BlockHitResult(
                                blockHit.getPos(),
                                blockHit.getSide(),
                                placePos,
                                false
                        )
                );
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private void handleBlockMode() {
        if (mc.options.jumpKey.isPressed() && mc.world.getTime() - lastPlaceTime > 2L) {
            // Проверяем наличие блока в руках
            boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
            int slotId = findBlockInHotbar();

            if (offHand || slotId != -1) {
                BlockPos placePos = getBlockPos();
                if (!mc.world.getBlockState(placePos).isReplaceable()) return;

                ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
                Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;

                if (!canPlace(stack, placePos)) return;

                Direction hitDir;
                BlockPos support;
                Vec3d hitVec;

                if (flySetting.get()) {
                    // Fly-режим: ставим блок под игроком
                    support = placePos.down();
                    hitDir = Direction.UP;
                    hitVec = Vec3d.ofCenter(support).add(0, 0.5, 0);
                } else {
                    // Обычный режим: нужна твёрдая стена
                    Vec3d look = mc.player.getRotationVec(1.0F);
                    Direction offsetDir = Direction.getFacing(look.x, 0.0, look.z);
                    support = placePos.offset(offsetDir);

                    if (!mc.world.getBlockState(support).isSolid()) return;

                    hitDir = offsetDir.getOpposite();
                    hitVec = Vec3d.ofCenter(support).add(
                            new Vec3d(hitDir.getVector().getX(),
                                    hitDir.getVector().getY(),
                                    hitDir.getVector().getZ()).multiply(0.5)
                    );
                }

                // Сохраняем текущий слот
                int prevSlot = mc.player.getInventory().selectedSlot;

                // Устанавливаем нужный слот (если не оффхенд)
                if (!offHand) {
                    mc.player.getInventory().selectedSlot = slotId;
                }

                // Взаимодействуем с блоком
                mc.interactionManager.interactBlock(
                        mc.player,
                        hand,
                        new BlockHitResult(hitVec, hitDir, support, false)
                );

                // Отправляем пакет взмаха руки
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));

                // Возвращаем предыдущий слот
                if (!offHand) {
                    mc.player.getInventory().selectedSlot = prevSlot;
                }

                lastPlaceTime = mc.world.getTime();

                // Добавляем импульс вверх для плавности
                // mc.player.setVelocity(new Vec3d(0, 0.3, 0));
            }
        }
    }

    private int findSphereInInventory() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && isAndromedaSphere(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean isAndromedaSphere(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Text name = stack.getName();
        String lower = name.getString().toLowerCase();
        return lower.contains("сфера андромеды") || lower.contains("andromeda sphere");
    }

    private boolean canPlace(ItemStack stack, BlockPos placePos) {
        if (placePos.getY() >= mc.player.getBlockY()) return false;

        if (!(stack.getItem() instanceof BlockItem)) return false;

        BlockItem blockItem = (BlockItem) stack.getItem();
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, placePos);

        if (shape.isEmpty()) return false;

        Box box = shape.getBoundingBox().offset(placePos.getX(), placePos.getY(), placePos.getZ());

        return !box.intersects(mc.player.getBoundingBox());
    }

    private BlockPos getBlockPos() {
        return BlockPos.ofFloored(mc.player.getPos().add(0, -0.001, 0));
    }
}