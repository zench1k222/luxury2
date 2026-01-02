package dev.luxury.modules.impl.movement;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.utils.MovingUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Random;

@ModuleAnnotation(
        name = "Jesus",
        desc = "Позволяет ходить по воде и лаве",
        category = Category.Movement
)
public class Jesus extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Matrix", new String[]{"Matrix", "Grim", "Vanilla"});
    private boolean isMoving = false;
    private final float melonBallSpeed = 0.47F;

    private int tickDelay = 1;
    private int yawChangeDelay = 0;
    private float storedYaw = 0;
    private int motionPhase = 0;
    private final Random random = new Random();

    public Jesus() {
        addSettings(mode);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Matrix")) {
            handleMatrixMode();
        } else if (mode.is("Grim")) {
            handleGrimMode();
        } else if (mode.is("Vanilla")) {
            handleVanillaMode();
        }
    }

    private void handleMatrixMode() {
        if (!mc.player.isTouchingWater() && !mc.player.isInLava()) return;

        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        ItemStack offHandItem = mc.player.getOffHandStack();

        String itemName = offHandItem.getName().getString();
        float appliedSpeed = melonBallSpeed;

        if (itemName.contains("Ломтик арбуза") && speedEffect != null && speedEffect.getAmplifier() == 2) {
            appliedSpeed *= 1.15F;
        } else if (speedEffect != null) {
            appliedSpeed *= switch (speedEffect.getAmplifier()) {
                case 2 -> 1.15F;
                case 1 -> 1.0F;
                default -> 0.68F;
            };
        } else {
            appliedSpeed *= 0.68F;
        }

        if (slowEffect != null) appliedSpeed *= 0.85F;

        isMoving = mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();

        if (isMoving) {
            double[] speed = MovingUtil.calculateDirection(appliedSpeed);
            mc.player.addVelocity(speed[0], 0, speed[1]);
        } else {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }

        double yMotion = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;
        mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
    }

    private void handleGrimMode() {
        if (mc.player == null || mc.world == null) return;

        World world = mc.world;
        BlockPos below = new BlockPos(
                (int) Math.floor(mc.player.getX()),
                (int) Math.floor(mc.player.getY() - 0.2),
                (int) Math.floor(mc.player.getZ())
        );
        boolean isOnWater = world.getFluidState(below).isIn(net.minecraft.registry.tag.FluidTags.WATER);

        if (!isOnWater || !mc.player.isTouchingWater()) {
            tickDelay = 0;
            motionPhase = 0;
            return;
        }

        Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
        Box box = new Box(playerBox.minX, playerBox.minY - 0.01, playerBox.minZ,
                playerBox.maxX, playerBox.minY, playerBox.maxZ);

        BlockPos boxPos = new BlockPos(
                (int) Math.floor(box.minX),
                (int) Math.floor(box.minY),
                (int) Math.floor(box.minZ)
        );
        if (!world.getBlockState(boxPos).getCollisionShape(world, boxPos).isEmpty()) return;

        mc.player.setOnGround(random.nextBoolean() && random.nextFloat() < 0.7f);

        double baseY = 0.00032;
        double extraY = random.nextDouble() * baseY;
        mc.player.setVelocity(mc.player.getVelocity().x, baseY + extraY, mc.player.getVelocity().z);

        if (tickDelay <= 0) {
            motionPhase = (motionPhase + 1) % 3;
            if (motionPhase != 0) {
                if (yawChangeDelay <= 0) {
                    float yawOffset = (float)((random.nextDouble() - 0.5) * 8.0);
                    storedYaw = mc.player.getYaw() + yawOffset;
                    yawChangeDelay = 4 + random.nextInt(3);
                } else yawChangeDelay--;

                double rad = Math.toRadians(storedYaw);
                double speed = 0.008 + random.nextDouble() * 0.005;

                double motionX = -Math.sin(rad) * speed;
                double motionZ = Math.cos(rad) * speed;

                if (mc.options.forwardKey.isPressed()) mc.player.addVelocity(motionX, 0, motionZ);
            }

            tickDelay = 2 + random.nextInt(2);
        } else tickDelay--;
    }

    private void handleVanillaMode() {
        if (mc.player == null) return;

        boolean inWater = mc.player.isTouchingWater();
        boolean inLava = mc.player.isInLava();

        if (!inWater && !inLava) return;

        double speed = 0.1;
        double yMotion = mc.options.jumpKey.isPressed() ? 0.04 : 0.0;

        if (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) {
            double[] dir = MovingUtil.calculateDirection(speed);
            mc.player.setVelocity(dir[0], yMotion, dir[1]);
        } else {
            mc.player.setVelocity(0, yMotion, 0);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }
    }
}
