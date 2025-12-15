package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventSpawnEntity;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.managers.SyncManager;
import dev.luxury.utils.player.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.item.ItemStack;

@ModuleAnnotation(
        name = "AutoTotem",
        desc = "Берёт в руки тотем при определённом здоровье",
        category = Category.Combat
)
public class AutoTotem extends Module {

    private final ModeListSetting mode = new ModeListSetting(
            "Брать если",
            new BooleanSetting("Кристалл", true),
            new BooleanSetting("Игрок с булавой", true),
            new BooleanSetting("Рядом крипер", false),
            new BooleanSetting("Якорь", false),
            new BooleanSetting("Падение", false)
    );

    private final SliderSetting HPElytra = new SliderSetting("Брать раньше на элитрах", 5, 2, 6, 1);
    private final BooleanSetting back = new BooleanSetting("Возвращать предмет", true);
    private final BooleanSetting noBallSwitch = new BooleanSetting("Не брать если шар", false);
    private final BooleanSetting saveEnchantedtotem = new BooleanSetting("Сохранять чаренные тотемы", true);
    public final SliderSetting hp = new SliderSetting("Здоровье", 4.5f, 2.0f, 20.0f, 0.1f);

    private final SliderSetting crystalDistance = new SliderSetting("Дистанция кристалла", 4, 2, 6, 1);
    private final SliderSetting anchorDistance = new SliderSetting("Дистанция якоря", 4, 2, 6, 1);

    private int item = -1;
    private int returnDelay = 0;
       public static AutoTotem instance;
    public AutoTotem() {
        addSettings(mode, hp, HPElytra, back, noBallSwitch, saveEnchantedtotem,
                crystalDistance, anchorDistance);
        instance = this;
    }

    @EventTarget
    public void onEntitySpawn(EventSpawnEntity event) {
        Entity e = event.getEntity();

        if (isEnabled("Кристалл") && e instanceof EndCrystalEntity) {
            if (mc.player != null && e.distanceTo(mc.player) <= crystalDistance.getFloatValue()) {
                forceTotem();
            }
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        int slot = getTotemSlot();
        ItemStack offhand = mc.player.getOffHandStack();
        boolean hasTotemInHand = offhand.getItem() == Items.TOTEM_OF_UNDYING;

        if (condition()) {
            returnDelay = 40; // 2 секунды задержки (20 тиков = 1 секунда)

            if (slot == -1) return;

            if (saveEnchantedtotem.get() && offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.hasEnchantments()) {
                ItemStack candidate = mc.player.getInventory().getStack(slot);
                if (candidate.getItem() == Items.TOTEM_OF_UNDYING && !candidate.hasEnchantments()) {
                    InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                    item = slot;
                    return;
                }
            }

            if (!hasTotemInHand) {
                InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                if (item == -1) {
                    item = slot;
                }
            }
        } else {
            if (returnDelay > 0) {
                returnDelay--;
                return;
            }

            if (item != -1 && back.get() && hasTotemInHand) {
                InventoryUtil.swapSlotsUniversal(item, 40, false, true);
                item = -1;
            }
        }
    }

    private void forceTotem() {
        int slot = getTotemSlot();
        if (slot == -1) return;

        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
            item = slot;
        }
    }

    private int getTotemSlot() {
        ItemStack offhand = mc.player.getOffHandStack();

        if (saveEnchantedtotem.get()) {
            if (offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.hasEnchantments()) {
                int normalTotem = findTotem(false);
                if (normalTotem != -1) return normalTotem;
                return -1;
            }

            int normalTotem = findTotem(false);
            if (normalTotem != -1) return normalTotem;

            int enchantedTotem = findTotem(true);
            if (enchantedTotem != -1) return enchantedTotem;
            return -1;
        }

        return InventoryUtil.getItemSlot(Items.TOTEM_OF_UNDYING);
    }

    private int findTotem(boolean enchanted) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                boolean hasEnchant = stack.hasEnchantments();
                if (enchanted == hasEnchant) return i;
            }
        }
        return -1;
    }

    private boolean condition() {
        if (mc.player.getHealth() <= hp.getFloatValue()) return true;

        if (!isBall()) {
            if (crystal()) return true;
            if (anchor()) return true;
            if (macePlayer()) return true;
            if (creeper()) return true;
        }

        return checkHPElytra() || checkFall();
    }

    private boolean checkFall() {
        if (!isEnabled("Падение")) return false;
        if (mc.player.isGliding()) return false;
        return mc.player.fallDistance > 10.0f;
    }

    private boolean checkHPElytra() {
        return ((ItemStack) mc.player.getInventory().armor.get(2)).getItem() == Items.ELYTRA &&
                mc.player.getHealth() <= hp.getFloatValue() + HPElytra.getFloatValue();
    }

    private boolean isBall() {
        if (isEnabled("Якорь") && mc.player.fallDistance > 5.0f) return false;
        return noBallSwitch.get() && mc.player.getOffHandStack().getItem() instanceof PlayerHeadItem;
    }

    private boolean anchor() {
        if (!isEnabled("Якорь")) return false;
        return InventoryUtil.TotemUtil.getBlock((float) anchorDistance.getFloatValue(), Blocks.RESPAWN_ANCHOR) != null;
    }

    private boolean creeper() {
        if (!isEnabled("Рядом крипер")) return false;

        for (Entity entity : SyncManager.getEntities()) {
            if (entity instanceof CreeperEntity creeper && mc.player.distanceTo(creeper) < 5.0f) {
                if (creeper.getClientFuseTime(0f) > 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean macePlayer() {
        if (!isEnabled("Игрок с булавой")) return false;

        for (PlayerEntity player : SyncManager.getPlayers()) {
            if (player == mc.player) continue;

            boolean hasMace = player.getMainHandStack().getItem() == Items.MACE;
            double dy = player.getY() - mc.player.getY();
            double yVel = player.getVelocity().y;
            double distance = player.distanceTo(mc.player);
            boolean isAbove = dy > 1.5;
            boolean isInAir = !player.isOnGround() && !player.isTouchingWater() && !player.isClimbing();
            boolean fallingOrInAir = (yVel < -0.1 || yVel > 0.1) && isInAir;

            if (hasMace && isAbove && fallingOrInAir && distance < 24) {
                return true;
            }
        }
        return false;
    }

    private boolean crystal() {
        if (!isEnabled("Кристалл")) return false;

        for (Entity entity : SyncManager.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) < crystalDistance.getFloatValue()) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnabled(String name) {
        for (Object setting : mode.getSettings()) {
            if (setting instanceof BooleanSetting boolSetting) {
                if (boolSetting.getName().equals(name)) {
                    return boolSetting.get();
                }
            }
        }
        return false;
    }

    private void reload() {
        item = -1;
        returnDelay = 0;
    }

    @Override
    public void onEnable() {
        reload();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reload();
        super.onDisable();
    }
}