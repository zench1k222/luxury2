package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@ModuleAnnotation(
        name = "TpLoot",
        desc = "Автоматическая телепортация к предметам",
        category = Category.Movement
)
public class TpLoot extends Module {

    private static final double SEARCH_RADIUS_XZ = 100.0;
    private static final double SEARCH_RADIUS_Y = 100.0;

    private final Set<Item> targetItems = new HashSet<>(Arrays.asList(
            Items.NETHERITE_SWORD,
            Items.PLAYER_HEAD,
            Items.TOTEM_OF_UNDYING,
            Items.ELYTRA
    ));

    private Vec3d initialPosition;
    private ItemEntity lastTargetedItem;
    private boolean hubCommandSent;
    private boolean flyDisabled;
    private boolean spawnCommandSent;
    private int tickCounter;

    public TpLoot() {
        this.initialPosition = null;
        this.lastTargetedItem = null;
        this.hubCommandSent = false;
        this.flyDisabled = false;
        this.spawnCommandSent = false;
        this.tickCounter = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.lastTargetedItem = null;
        this.hubCommandSent = false;
        this.flyDisabled = false;
        this.spawnCommandSent = false;
        this.tickCounter = 0;
        this.initialPosition = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.initialPosition = null;
        this.lastTargetedItem = null;
        this.hubCommandSent = false;
        this.flyDisabled = false;
        this.spawnCommandSent = false;
        this.tickCounter = 0;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null || mc.player == null) return;

        tickCounter++;

        if (tickCounter % 3 != 0) return;

        if (this.spawnCommandSent) {
            return;
        }

        if (this.lastTargetedItem != null && !this.lastTargetedItem.isAlive()) {
            if (!this.flyDisabled) {
                mc.player.networkHandler.sendChatMessage("/fly");
                this.flyDisabled = true;
            }
            if (!this.spawnCommandSent) {
                mc.player.networkHandler.sendChatMessage("/spawn");
                this.spawnCommandSent = true;
                this.disable();
            }
            return;
        }

        findNearestTargetItem().ifPresent(item -> {
            if (this.initialPosition == null) {
                this.initialPosition = mc.player.getPos();
            }

            if (this.lastTargetedItem != item) {
                boolean previousItemLooted = this.lastTargetedItem != null && !this.lastTargetedItem.isAlive();

                Vec3d itemPos = item.getPos();
                this.teleport(itemPos.x, itemPos.y, itemPos.z);
                this.lastTargetedItem = item;
                this.hubCommandSent = false;

                if (!previousItemLooted) {
                    this.flyDisabled = false;
                    this.spawnCommandSent = false;
                }
            }
        });
    }

    private void teleport(double x, double y, double z) {
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true,true));
            mc.player.setPosition(x, y, z);
            mc.player.setVelocity(0, 0, 0);
        }
    }

    private Optional<ItemEntity> findNearestTargetItem() {
        if (mc.player == null || mc.world == null) {
            return Optional.empty();
        }

        Vec3d playerPos = mc.player.getPos();
        double radiusSq = SEARCH_RADIUS_XZ * SEARCH_RADIUS_XZ;

        Box searchBox = mc.player.getBoundingBox()
                .expand(SEARCH_RADIUS_XZ, SEARCH_RADIUS_Y, SEARCH_RADIUS_XZ);

        return mc.world.getEntitiesByClass(ItemEntity.class, searchBox, item -> {
                    if (!item.isAlive() || !this.targetItems.contains(item.getStack().getItem())) {
                        return false;
                    }
                    Vec3d pos = item.getPos();
                    double dx = pos.x - playerPos.x;
                    double dz = pos.z - playerPos.z;
                    double dy = pos.y - playerPos.y;
                    return dx * dx + dz * dz <= radiusSq && Math.abs(dy) <= SEARCH_RADIUS_Y;
                })
                .stream()
                .min(Comparator.comparingDouble(item -> {
                    Vec3d pos = item.getPos();
                    double dx = pos.x - playerPos.x;
                    double dz = pos.z - playerPos.z;
                    double dy = pos.y - playerPos.y;
                    return dx * dx + dy * dy + dz * dz;
                }));
    }
}