package dev.luxury.utils.player;

import dev.luxury.utils.managers.SyncManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryUtil {
    public static final  MinecraftClient mc = MinecraftClient.getInstance();
    public static void moveItemTest(int from, int to, boolean air) {
        if (from == to)
            return;
        testPick(from, 0);
        testPick(to, 0);
        if (air)
            testPick(from, 0);
    }
    public static void testPick(int slot, int button) {
        mc.interactionManager.clickSlot(0, slot, button, SlotActionType.PICKUP, mc.player);
    }

    public static void moveToOffhand(Item item) {
        if (mc.player == null || mc.interactionManager == null) return;
        var inventory = mc.player.getInventory();
        if (inventory.offHand.get(0).getItem() == item) return;
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).getItem() == item) {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i + 36, 40, SlotActionType.SWAP, mc.player);
                return;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (inventory.getStack(i).getItem() == item) {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 40, SlotActionType.SWAP, mc.player);
                return;
            }
        }
    }

    public static int getItem(Class<?> itemClass, boolean hotBarOnly) {
        var inventory = mc.player.getInventory();
        int startSlot = hotBarOnly ? 0 : 9;
        int endSlot = hotBarOnly ? 9 : inventory.size();

        for (int i = startSlot; i < endSlot; i++) {
            if (i >= inventory.size()) continue;
            ItemStack itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty() && itemClass.isInstance(itemStack.getItem())) {
                return i;
            }
        }

        return -1;
    }
    public static int getPearls() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem().asItem() instanceof EnderPearlItem) {
                return i;
            }
        }
        return -1;
    }
    public static void swapSlotsUniversal(int slot1, int slot2, boolean cursor, boolean conversion) {
        if (cursor) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, mc.player);
        } else {
            if (slot1 < 9 && conversion) {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1 + 36, slot2, SlotActionType.SWAP, mc.player);
            } else {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, slot2, SlotActionType.SWAP, mc.player);
            }
        }

    }
    public static void swapSlots(int slot1, int slot2) {
        if (slot1 < 9 && slot2 < 9) {
            mc.interactionManager.clickSlot(0, slot1 + 36, slot2, SlotActionType.SWAP, mc.player);
        } else {
            if (slot1 < 9) {
                mc.interactionManager.clickSlot(0, slot2, slot1, SlotActionType.SWAP, mc.player);
            } else if (slot2 < 9) {
                mc.interactionManager.clickSlot(0, slot1, slot2, SlotActionType.SWAP, mc.player);
            }
        }
    }

    public static void pickupItem(int slot, int button) {
        mc.interactionManager.clickSlot(0, slot, button, SlotActionType.PICKUP, mc.player);
    }
    public static int getItemSlot(Item input) {
        for (ItemStack stack : SyncManager.getItems()) {
            if (stack.getItem() == input) {
                return -2;
            }

        }
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() == input) {
                slot = i;
                break;
            }
        }
        if (slot < 9 && slot != -1) {
            slot = slot + 36;
        }
        return slot;
    }
    public static int getHotBarSlot(Item input) {
        for(int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == input) {
                return i;
            }
        }

        return -1;
    }
    public static boolean doesHotbarHaveItem(Item item) {
        for(int i = 0; i < 9; ++i) {
            mc.player.getInventory().getStack(i);
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return true;
            }
        }

        return false;
    }


    public static void windowClick(int conteinerId, int slot, int mouse, SlotActionType type, PlayerEntity player) {
        mc.interactionManager.clickSlot(conteinerId, slot, mouse, type, player);
    }
    public static void startFly() {
        mc.player.startGliding();
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    public static class TotemUtil {
        public static BlockPos getBlock(float distance, Block block) {
            return getSphere(getPlayerPosLocal(), distance, 6, false, true, 0).stream().filter(position -> mc.world.getBlockState(position).getBlock() == block).min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos))).orElse(null);
        }
        public static BlockPos getBlock(float distance) {
            return getSphere(getPlayerPosLocal(), distance, 6, false, true, 0).stream().filter(position -> mc.world.getBlockState(position).getBlock() != Blocks.AIR).min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos))).orElse(null);
        }
        public static List<BlockPos> getSphere(final BlockPos blockPos, final float n, final int n2, final boolean b, final boolean b2, final int n3) {
            final ArrayList<BlockPos> list = new ArrayList<BlockPos>();
            final int x = blockPos.getX();
            final int y = blockPos.getY();
            final int z = blockPos.getZ();
            for (int n4 = x - (int) n; n4 <= x + n; ++n4) {
                for (int n5 = z - (int) n; n5 <= z + n; ++n5) {
                    for (int n6 = b2 ? (y - (int) n) : y; n6 < (b2 ? (y + n) : ((float) (y + n2))); ++n6) {
                        final double n7 = (x - n4) * (x - n4) + (z - n5) * (z - n5) + (b2 ? ((y - n6) * (y - n6)) : 0);
                        if (n7 < n * n && (!b || n7 >= (n - 1.0f) * (n - 1.0f))) {
                            list.add(new BlockPos(n4, n6 + n3, n5));
                        }
                    }
                }
            }
            return list;
        }

        public static BlockPos getPlayerPosLocal() {
            if (mc.player == null) {
                return (BlockPos) BlockPos.ZERO;
            }
            return new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
        }

        public static double getDistanceOfEntityToBlock(final Entity entity, final BlockPos blockPos) {
            return getDistance(entity.getX(), entity.getY(), entity.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
        public static double getDistance(final double n, final double n2, final double n3, final double n4, final double n5, final double n6) {
            final double n7 = n - n4;
            final double n8 = n2 - n5;
            final double n9 = n3 - n6;
            return MathHelper.sqrt((float) (n7 * n7 + n8 * n8 + n9 * n9));
        }
    }


}
