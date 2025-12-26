package dev.luxury.modules.impl;

import com.mojang.authlib.GameProfile;
import dev.luxury.events.impl.client.EntityDeathEvent;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ModuleAnnotation(
        name = "KillEffect",
        category = Category.Render
)
public class KillEffect extends Module {
    private final BooleanSetting mobs = new BooleanSetting("На мобов", false);
    private final ModeSetting effectType = new ModeSetting("Тип", "Призрак", new String[]{"Призрак"});
    private final Map<Entity, EntityRenderData> renderEntities = new ConcurrentHashMap<>();

    public KillEffect() {
        addSettings(mobs, effectType);
    }

    private static class EntityRenderData {
        private final long timestamp;
        private final float yaw;
        private final Vec3d startPos;
        private final Entity entity;
        private final GameProfile gameProfile;
        private final EntityPose pose;
        private final OtherClientPlayerEntity fakePlayer;

        public EntityRenderData(long timestamp, float yaw, Vec3d startPos, Entity entity, OtherClientPlayerEntity fakePlayer) {
            this.timestamp = timestamp;
            this.yaw = yaw;
            this.startPos = startPos;
            this.entity = entity;
            this.gameProfile = entity instanceof PlayerEntity ? ((PlayerEntity) entity).getGameProfile() : null;
            this.pose = entity.getPose();
            this.fakePlayer = fakePlayer;
        }

        public long getTimestamp() { return timestamp; }
        public float getYaw() { return yaw; }
        public Vec3d getStartPos() { return startPos; }
        public Entity getEntity() { return entity; }
        public GameProfile getGameProfile() { return gameProfile; }
        public EntityPose getPose() { return pose; }
        public OtherClientPlayerEntity getFakePlayer() { return fakePlayer; }
    }

    @EventTarget
    public void onEntityDeath(EntityDeathEvent event) {
        if (mc.world == null || mc.player == null) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        if (!mobs.isValue() && !(entity instanceof PlayerEntity)) return;
        if (entity == mc.player || renderEntities.containsKey(entity)) return;

        OtherClientPlayerEntity fakePlayer = null;
        if (effectType.is("Призрак") && entity instanceof PlayerEntity) {
            fakePlayer = new OtherClientPlayerEntity(mc.world, ((PlayerEntity) entity).getGameProfile());
            fakePlayer.setPitch(-30.0f);
            fakePlayer.setYaw(entity.getYaw());
            fakePlayer.headYaw = entity.getYaw();
            fakePlayer.bodyYaw = entity.getYaw();
            fakePlayer.setCustomNameVisible(false);
            fakePlayer.setCustomName(Text.literal("Ghost_" + ((PlayerEntity) entity).getGameProfile().getId()));
            mc.world.addEntity(fakePlayer);
        }

        renderEntities.put(entity, new EntityRenderData(System.currentTimeMillis(), entity.getYaw(), entity.getPos(), entity, fakePlayer));
    }

    @EventTarget
    public void onWorldRender(EventRender3D e) {
        if (mc.world == null || mc.player == null) return;
        MatrixStack stack = e.getMatrices();
        float tickDelta = e.getPartialTicks();

        List<Entity> entitiesToRemove = new ArrayList<>();

        renderEntities.forEach((entity, data) -> {
            long elapsed = System.currentTimeMillis() - data.getTimestamp();

            if (elapsed > 3000) {
                entitiesToRemove.add(entity);
                if (data.getFakePlayer() != null) {
                    mc.world.removeEntity(data.getFakePlayer().getId(), Entity.RemovalReason.DISCARDED);
                }
                return;
            }

            float timeProgress = elapsed / 3000.0f;
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

            if (effectType.is("Призрак")) {
                float yOffset = timeProgress * 3.0f;
                int alpha = (int) (255 * (1 - timeProgress));
                Vec3d soulPos = data.getStartPos().add(0, yOffset, 0);

                Entity renderEntity = data.getEntity();
                if (data.getFakePlayer() != null) {
                    renderEntity = data.getFakePlayer();
                    renderEntity.setPos(soulPos.x, soulPos.y, soulPos.z);
                }

                Vec3d relativePos = soulPos.subtract(cameraPos);
                RenderUtil3D.drawEntity(renderEntity, relativePos, data.getYaw(), alpha, stack, tickDelta);
            }
        });

        entitiesToRemove.forEach(renderEntities::remove);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        renderEntities.forEach((entity, data) -> {
            if (data.getFakePlayer() != null && mc.world != null) {
                mc.world.removeEntity(data.getFakePlayer().getId(), Entity.RemovalReason.DISCARDED);
            }
        });
        renderEntities.clear();
    }
}