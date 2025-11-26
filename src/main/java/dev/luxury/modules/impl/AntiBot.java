package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.math.TimerUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Игнорирует ботов от античита",
        category = Category.Combat
)
public final class AntiBot extends Module {
    public static final AntiBot INSTANCE = new AntiBot();

    public AntiBot() {
    }

    private final List<PlayerEntity> bots = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (timer.finished(10000) && !bots.isEmpty()) {
            bots.clear();
            timer.reset();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == mc.player) continue;
            //drugduck tech.
            if (armorCheck(player) && !bots.contains(player)) bots.add(player);
        }
    }

    private boolean armorCheck(PlayerEntity entity) {
        return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && isNotColored(entity, 3) && !getArmor(entity, 3).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && isNotColored(entity, 2) && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && isNotColored(entity, 1) && !getArmor(entity, 1).hasEnchantments()
                || getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && isNotColored(entity, 0) && !getArmor(entity, 0).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !getArmor(entity, 1).hasEnchantments());
    }

    private ItemStack getArmor(PlayerEntity entity, int slot) {
        return entity.getInventory().getArmorStack(slot);
    }

    private boolean isNotColored(PlayerEntity entity, int slot) {
        return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!bots.isEmpty()) bots.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!bots.isEmpty()) bots.clear();
    }

    public boolean isBot(PlayerEntity player) {
        return this.bots.contains(player);
    }
}