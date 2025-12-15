package dev.luxury.utils.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PlayerIntersectionUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.interactionManager != null) {
            mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
        }
    }

    public static void interactItem(Hand hand) {
        interactItem(hand, getCameraAngle());
    }

    public static void interactItem(Hand hand, float[] angle) {
        sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle[0], angle[1]));
    }

    public static void interactEntity(Entity entity) {
        if (mc.player != null && mc.player.networkHandler != null) {
            Vec3d center = entity.getBoundingBox().getCenter();
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity,
                    false, Hand.MAIN_HAND, center));
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity,
                    false, Hand.MAIN_HAND));
        }
    }

    public static void startFallFlying() {
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
        }
    }

    public static void sendPacketWithoutEvent(Packet<?> packet) {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
            mc.getNetworkHandler().getConnection().send(packet);
        }
    }

    public static void grimSuperBypass(double y, float[] angle) {
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY() + y, mc.player.getZ(),
                    angle[0], angle[1], mc.player.isOnGround(), mc.player.horizontalCollision));
        }
    }

    public static String getHealthString(LivingEntity entity) {
        return getHealthString(getHealth(entity));
    }

    public static String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
    }

    public static float getHealth(LivingEntity entity) {
        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        if (entity instanceof PlayerEntity player) {
            switch (ServerUtil.server) {
                case "FunTime", "ReallyWorld" -> {
                    ScoreboardObjective scoreBoard = player.getScoreboard()
                            .getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                    if (scoreBoard != null) {
                        MutableText text2 = ReadableScoreboardScore.getFormattedScore(
                                player.getScoreboard().getScore(player, scoreBoard),
                                scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                        try {
                            hp = Float.parseFloat(removeFormatting(text2.getString()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return MathHelper.clamp(hp, 0, entity.getMaxHealth());
    }

    private static String removeFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public static void jump() {
        if (mc.player != null && mc.player.isSprinting()) {
            float g = mc.player.getYaw() * ((float)Math.PI / 180F);
            mc.player.addVelocity(-MathHelper.sin(g) * 0.2F, 0.0F, MathHelper.cos(g) * 0.2F);
        }
    }

    public static List<BlockPos> getCube(BlockPos center, float radius) {
        return getCube(center, radius, radius, true);
    }

    public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        return getCube(center, radiusXZ, radiusY, true);
    }

    public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int posY = down ? centerY - (int) radiusY : centerY;

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = posY; y <= centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int y = start.getY(); y <= end.getY(); y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public static InputUtil.Type getKeyType(int key) {
        return key < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public static Stream<Entity> streamEntities() {
        return mc.world != null ? StreamSupport.stream(mc.world.getEntities().spliterator(), false) : Stream.empty();
    }

    public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
        return mc.world != null && mc.player != null &&
                mc.world.isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
    }

    public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
        return mc.player != null && mc.player.getActiveStatusEffects().containsKey(statusEffect);
    }

    public static boolean isPlayerInBlock(Block block) {
        return mc.player != null && isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), block);
    }

    public static boolean isBoxInBlock(Box box, Block block) {
        return isBox(box, pos -> mc.world != null && mc.world.getBlockState(pos).getBlock().equals(block));
    }

    public static boolean isBoxInBlocks(Box box, List<Block> blocks) {
        return isBox(box, pos -> mc.world != null && blocks.contains(mc.world.getBlockState(pos).getBlock()));
    }

    public static boolean isBox(Box box, Predicate<BlockPos> pos) {
        return BlockPos.stream(box).anyMatch(pos);
    }

    public static boolean isKey(KeyBinding key) {
        return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
    }

    public static boolean isKey(InputUtil.Type type, int keyCode) {
        if (keyCode != -1 && mc.getWindow() != null) {
            switch (type) {
                case KEYSYM: return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
                case MOUSE: return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
            }
        }
        return false;
    }

    public static boolean isAir(BlockPos blockPos) {
        return mc.world != null && isAir(mc.world.getBlockState(blockPos));
    }

    public static boolean isAir(BlockState state) {
        return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
    }

    public static boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }

    public static boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    private static float[] getCameraAngle() {
        return new float[] { mc.player != null ? mc.player.getYaw() : 0,
                mc.player != null ? mc.player.getPitch() : 0 };
    }
}