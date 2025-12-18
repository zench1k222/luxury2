
package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.player.ServerUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@ModuleAnnotation(
        name = "AntiBot",
        desc = "Игнорирует античит-ботов",
        category = Category.Combat
)
public class AntiBot extends Module {



    public final List<PlayerEntity> bots = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();
    public static AntiBot instance;
    public AntiBot(){
        instance = this;
    }
    @EventTarget
    public void onTick(EventTick e) {
        if (mc.world == null || mc.player == null) return;

        if (timer.passed(10000) && !bots.isEmpty()) {
            bots.clear();
            timer.reset();
        }



        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == mc.player) continue;
            //байпасик под рв легенький а nekrasivih лох
            if (armorCheck(player) && !bots.contains(player)) bots.add(player);
        }
    }

    private boolean armorCheck(PlayerEntity entity) {
        return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && isNotColored(entity, 3) && !getArmor(entity, 3).hasEnchantments() || getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && isNotColored(entity, 2) && !getArmor(entity, 2).hasEnchantments() || getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && isNotColored(entity, 1) && !getArmor(entity, 1).hasEnchantments() || getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && isNotColored(entity, 0) && !getArmor(entity, 0).hasEnchantments() || getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !getArmor(entity, 2).hasEnchantments() || getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !getArmor(entity, 1).hasEnchantments());
    }

    private ItemStack getArmor(PlayerEntity entity, int slot) {
        return entity.getInventory().getArmorStack(slot);
    }

    private boolean isNotColored(PlayerEntity entity, int slot) {
        return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
    }
    public boolean isBot(PlayerEntity player) {
        return bots.contains(player);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!bots.isEmpty()) bots.clear();
        if (ServerUtil.isConnected("aresmine")) {
            disable();
            ChatUtil.sendError("На АресМайне нет ботов, нужно выключить АнтиБот");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!bots.isEmpty()) bots.clear();
    }
}