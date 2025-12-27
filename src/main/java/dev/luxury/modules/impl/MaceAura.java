package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Random;

@ModuleAnnotation(
        name = "MaceAura",
        desc = "Нормальный удар булавой с высоты",
        category = Category.Combat
)
public class MaceAura extends Module {

    private final SliderSetting minHeight = new SliderSetting("Мин. высота", 30, 20, 60, 1);
    private final SliderSetting maxHeight = new SliderSetting("Макс. высота", 90, 60, 120, 1);
    private final SliderSetting swapDistance = new SliderSetting("Свап дистанция", 10, 6, 15, 0.5f);

    private final BooleanSetting attackPlayers = new BooleanSetting("Игроки", true);
    private final BooleanSetting attackMobs = new BooleanSetting("Мобы", false);

    private Entity target;
    private double targetFlyHeight;
    private boolean goingUp = true;
    private boolean gearSwapped = false;

    public MaceAura(){
        addSettings(minHeight, maxHeight, swapDistance, attackPlayers, attackMobs);
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (target == null || !target.isAlive()) {
            target = findTarget();
            if (target == null) return;
            targetFlyHeight = target.getY() + randomHeight();
            goingUp = true;
            gearSwapped = false;
        }

        ensureElytra();

        if (!mc.player.isGliding()) startGlide();

        if (goingUp) {
            flyUp();
            if (mc.player.getY() >= targetFlyHeight) {
                goingUp = false;
            }
            return;
        }

        flyDown();

        double dist = mc.player.distanceTo(target);

        if (dist <= swapDistance.getValue() && !gearSwapped) {
            swapToChestplate();
            swapToMace();
            gearSwapped = true;
        }

        if (dist <= 3.2 && mc.player.isOnGround()) {
            attack();
            resetCycle();
        }
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (player.isSpectator()) continue;
            if (player.isCreative()) continue;

            double dist = mc.player.squaredDistanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }


    private boolean validTarget(Entity e) {
        if (e instanceof PlayerEntity p) {
            return attackPlayers.get() && !FriendManager.getInstance().isFriend(p.getName().getString());
        }
        return attackMobs.get();
    }

    private void attack() {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void resetCycle() {
        swapToElytra();
        target = null;
        goingUp = true;
        gearSwapped = false;
    }

    /* ====================== FLIGHT ====================== */

    private void flyUp() {
        lookAt(target.getPos().add(0, 30, 0));
        mc.options.forwardKey.setPressed(true);
    }

    private void flyDown() {
        lookAt(target.getPos());
        mc.options.forwardKey.setPressed(true);
        mc.options.sneakKey.setPressed(true);
    }

    private void lookAt(Vec3d pos) {
        Vec3d eyes = mc.player.getCameraPosVec(1);
        Vec3d dir = pos.subtract(eyes).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        float pitch = (float) Math.toDegrees(Math.asin(-dir.y));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private void startGlide() {
        mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
        );
    }

    /* ====================== GEAR ====================== */

    private void ensureElytra() {
        Item chest = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
        if (chest != Items.ELYTRA) {
            int slot = findItem(Items.ELYTRA);
            if (slot != -1) swapChest(slot);
        }
    }

    private void swapToChestplate() {
        for (Item i : new Item[]{
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE
        }) {
            int slot = findItem(i);
            if (slot != -1) {
                swapChest(slot);
                return;
            }
        }
    }

    private void swapToElytra() {
        int slot = findItem(Items.ELYTRA);
        if (slot != -1) swapChest(slot);
    }

    private void swapToMace() {
        int slot = findItem(Items.MACE);
        if (slot >= 0 && slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void swapChest(int invSlot) {
        int chestSlot = 6;
        int slot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, chestSlot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
    }

    private int findItem(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private double randomHeight() {
        return minHeight.getValue() +
                new Random().nextDouble() * (maxHeight.getValue() - minHeight.getValue());
    }

    @Override
    public void onDisable() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        target = null;
    }
}
