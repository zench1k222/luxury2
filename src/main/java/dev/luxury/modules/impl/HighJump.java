package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "HighJump",
        desc = "",
        category = Category.Movement
)
public class HighJump extends Module {
    MinecraftClient mc = MinecraftClient.getInstance();
    @EventTarget
    public void onTick(EventTick e) {
        if (mc.world == null || mc.player == null) return;

        ChunkPos playerChunkPos = new ChunkPos(mc.player.getBlockPos());

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                WorldChunk chunk = mc.world.getChunk(playerChunkPos.x + x, playerChunkPos.z + z);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
                        BlockPos tilePos = blockEntity.getPos();

                        double dx = mc.player.getX() - (tilePos.getX() + 0.5f);
                        double dz = mc.player.getZ() - (tilePos.getZ() + 0.5f);
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        double yDiff = Math.abs(mc.player.getY() - (tilePos.getY() + 0.5f));

                        if (distance <= 1 && yDiff <= (mc.player.getVelocity().y > 1 ? 30 : 2) && mc.player.fallDistance == 0) {
                            float progress = shulker.getAnimationProgress(1.0f);

                            if (progress > 0.0f && progress < 1.0f) {
                                mc.player.setVelocity(mc.player.getVelocity().x, 2.33f, mc.player.getVelocity().z);
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("dwa");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
