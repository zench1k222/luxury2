package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "Spider",
        desc = "Ставит сферу Андромеды туда, куда смотришь, при касании стены",
        category = Category.Movement
)
public class Spider extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

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

    private int findSphereInInventory() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && isAndromedaSphere(stack)) {
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
}
