package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import dev.luxury.modules.impl.killaura.rotate.TargetRotate;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;


@ModuleAnnotation(
        name = "AutoPotion",
        desc = "Автоматически кидает бафы под себя",
        category = Category.Combat
)
public class AutoPotion extends Module {

    private final BooleanSetting autoOff = new BooleanSetting("Авто отключение", false);

    private final ModeListSetting potions = new ModeListSetting("Бросать",
            new BooleanSetting("Силу", true),
            new BooleanSetting("Скорость", true),
            new BooleanSetting("Огнестойкость", true)
    );

    private boolean throwing;
    private long lastThrowTime;
    private final long throwDelay = 500;
    private final float throwPitch = 90f;

    public AutoPotion() {
        addSettings(potions, autoOff);
    }

    private enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "Силу"),
        SPEED(StatusEffects.SPEED, "Скорость"),
        FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "Огнестойкость");

        final RegistryEntry<StatusEffect> effect;
        final String settingName;

        PotionType(RegistryEntry<StatusEffect> effect, String settingName) {
            this.effect = effect;
            this.settingName = settingName;
        }

        boolean enabled(AutoPotion m) {
            BooleanSetting s = m.potions.getValueByName(settingName);
            return s != null && s.get();
        }
    }

    @EventTarget
    public void onMotion(EventTick e) {
        if (!shouldThrow()) return;

        Rotate down = new Rotate(mc.player.getYaw(), throwPitch);

        Luxury.getInstance().getRotationManager().setRotation(new TargetRotate(down, () -> down, Luxury.getInstance().getRotationManager().getAim().getInstantSetup()), 9, this);

        throwing = true;
    }

    @EventTarget
    public void onUpdate(EventTick e) {
        if (!throwing) return;

        throwPotion(PotionType.STRENGTH);
        throwPotion(PotionType.SPEED);
        throwPotion(PotionType.FIRE_RESISTANCE);

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));

        lastThrowTime = System.currentTimeMillis();
        throwing = false;

        if (autoOff.get()) toggle();
    }

    private boolean shouldThrow() {
        if (mc.player == null || mc.world == null) return false;
        if (!mc.player.isOnGround()) return false;
        if (mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.AIR)
            return false;
        if (System.currentTimeMillis() - lastThrowTime < throwDelay)
            return false;

        return canBuff();
    }

    private boolean canBuff() {
        for (PotionType t : PotionType.values()) {
            if (canBuff(t)) return true;
        }
        return false;
    }

    private boolean canBuff(PotionType t) {
        if (!t.enabled(this)) return false;
        if (mc.player.hasStatusEffect(t.effect)) return false;
        return findPotionSlot(t) != -1;
    }

    private int findPotionSlot(PotionType t) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.SPLASH_POTION)) continue;

            PotionContentsComponent comp =
                    stack.getComponents().get(DataComponentTypes.POTION_CONTENTS);
            if (comp == null) continue;

            for (StatusEffectInstance e : comp.getEffects()) {
                if (e.getEffectType() == t.effect) return i;
            }
        }
        return -1;
    }

    private void throwPotion(PotionType t) {
        if (!canBuff(t)) return;

        int slot = findPotionSlot(t);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
    }

    private void sendSequencedPacket(SequencedPacketCreator creator) {
        try (PendingUpdateManager mgr = mc.world.getPendingUpdateManager().incrementSequence()) {
            mc.player.networkHandler.sendPacket(creator.predict(mgr.getSequence()));
        } catch (Exception e) {
            mc.player.networkHandler.sendPacket(creator.predict(0));
        }
    }

    @Override
    public void onDisable() {
        throwing = false;
    }
}
