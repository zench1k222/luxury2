package dev.luxury.modules.impl.taksa;

import dev.luxury.events.impl.eventapi.events.Event;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.KillAura;
import dev.luxury.utils.animations.infinity.InfinityAnimation;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.math.RotationUtil;
import dev.luxury.utils.math.TimerUtils;
import dev.luxury.utils.world.PlayerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class TaksaBrain {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private Vec3d pos;
    private Vec3d motion = Vec3d.ZERO;
    private float direction = MathUtil.random(0, 360);
    private float yaw, body;
    private int speed = 50;

    private final InfinityAnimation x = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);
    private final InfinityAnimation y = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);
    private final InfinityAnimation z = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);

    private final InfinityAnimation bodyAnim = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);
    private final InfinityAnimation yawAnim = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);
    private final InfinityAnimation pitchAnim = new InfinityAnimation(dev.luxury.utils.animations.Easing.LINEAR);
    
    @Getter
    private boolean lay;
    private final TimerUtils staying = new TimerUtils();
    
    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;
    
    @Setter
    private PlayerEntity entity;
    
    public void onEvent(Event event) {
        if (entity == null) return;
        
        if (event instanceof EventTick) {
            Vec3d playerPos = entity.getPos();
            
            if (pos == null || pos.distanceTo(playerPos) > 10) {
                pos = playerPos;
                x.animate((float) pos.x, 1);
                y.animate((float) pos.y, 1);
                z.animate((float) pos.z, 1);
            }
            
            motion = motion.add(0, -0.2f, 0);
            
            Vec3d newPos = pos.add(motion);
            
            if (PlayerUtil.isBlockSolid(newPos.x, newPos.y, newPos.z)) {
                int blockY = (int) newPos.y;
                double correctedY = blockY + 1 + 0.1;
                newPos = new Vec3d(newPos.x, correctedY, newPos.z);
                motion = new Vec3d(motion.x, 0, motion.z);
            }
            
            motion = new Vec3d(motion.x, 0, motion.z);
            
            KillAura killAura = ModuleManager.getModule(KillAura.class);
            LivingEntity target = killAura != null && killAura.isEnabled() ? killAura.getTarget() : null;
            
            if (target != null && entity == mc.player) {
                if (PlayerUtil.isBlockSolid(newPos.x, newPos.y - 0.1f, newPos.z)) {
                    motion = new Vec3d(motion.x, 0.62f, motion.z);
                }
                
                Box box = new Box(getPos().subtract(0.4, 0, 0.4), getPos().add(0.4, 0.4, 0.4));
                Box targetbox = target.getBoundingBox().expand(-0.1f, 0, -0.1f);
                
                motion = motion.add(target.getPos().subtract(newPos).normalize());
                
                if (box.maxX > targetbox.minX
                 && box.maxY > targetbox.minY
                 && box.maxZ > targetbox.minZ
                 && box.minX < targetbox.maxX
                 && box.minY < targetbox.maxY
                 && box.minZ < targetbox.maxZ) {
                    motion = motion.multiply(-1, 1, -1);
                }
            } else {
                if (newPos.distanceTo(playerPos) > 2) {
                    motion = motion.add(playerPos.subtract(newPos).normalize());
                }
            }
            
            handleRotation();
            
            pos = newPos;
            
            if (pos.distanceTo(playerPos) < 0.1f) {
                direction = MathUtil.random(0, 360);
                double xMot = -Math.sin(Math.toRadians(direction)) * 0.1;
                double zMot = Math.cos(Math.toRadians(direction)) * 0.1;
                motion = motion.add(xMot, 0, zMot);
            }
            
            motion = motion.multiply(0.5);
            
            speed = 150;
            x.animate((float) pos.x, speed);
            y.animate((float) pos.y, speed);
            z.animate((float) pos.z, speed);
            
            limbTick();
            
            if (Math.abs(pos.x - x.getValue()) > 0.1f || Math.abs(pos.z - z.getValue()) > 0.1f) {
                staying.reset();
            }
            
            lay = staying.passed(1000);
        }
    }
    
    private void handleRotation() {
        if (motion.x != 0 || motion.z != 0) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float) Math.toDegrees(angle) - 90;
            yaw %= 360;
            if (yaw < 0) yaw += 360;
        }

        Vec2f rotation = RotationUtil.get(pos, entity.getEyePos());
        
        KillAura killAura = ModuleManager.getModule(KillAura.class);
        LivingEntity target = killAura != null && killAura.isEnabled() ? killAura.getTarget() : null;
        
        if (target != null && entity == mc.player) {
            rotation = RotationUtil.get(pos, target.getPos());
        }
        
        float gradus = lay ? 200 : 150;
        float gradus1 = lay ? 100 : 50;
        if (rotation.x - yaw < -gradus || rotation.x - yaw > gradus) {
            yaw = rotation.x;
        }
        
        float shortestYawPath = (float) (((((yaw - body) % 360) + 540) % 360) - 180);

        if (!lay)
            bodyAnim.animate(body + shortestYawPath, 150);
        yawAnim.animate(MathHelper.clamp(rotation.x - yaw, -gradus1, gradus1), 150);
        pitchAnim.animate(rotation.y, 150);
        
        body = body + shortestYawPath;
    }
    
    public void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double d0 = x.getValue() - pos.x;
        double d1 = 0.0D;
        double d2 = z.getValue() - pos.z;
        float f = MathHelper.sqrt((float) (d0 * d0 + d1 * d1 + d2 * d2)) * 4.0F;

        if (f > 1.0F) {
            f = 1.0F;
        }

        limbSwingAmount += (f - limbSwingAmount) * 0.4F;
        limbSwing += limbSwingAmount;
    }
    
    public float getBody() {
        return bodyAnim.getValue();
    }
    
    public float getYaw() {
        return yawAnim.getValue();
    }
    
    public float getPitch() {
        return pitchAnim.getValue();
    }
    
    public Vec3d getPos() {
        return new Vec3d(x.getValue(), y.getValue(), z.getValue());
    }
}

