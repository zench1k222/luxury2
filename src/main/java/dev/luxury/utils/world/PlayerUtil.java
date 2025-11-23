package dev.luxury.utils.world;

import lombok.experimental.UtilityClass;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

@UtilityClass
public class PlayerUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isBlockSolid(double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getCollisionShape(mc.world, pos);
        return !shape.isEmpty();
    }
}

