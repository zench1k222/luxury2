package dev.luxury.modules.impl;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.MovingUtil;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;

@ModuleAnnotation(
        name = "NoWeb",
        category = Category.Movement
)
public class NoWeb extends Module {

    private final SliderSetting speedS = new SliderSetting("Скорость", 0.1, 0.1, 0.6, 0.1);

    public NoWeb() {
        addSettings(speedS);
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        BlockPos pos = mc.player.getBlockPos();
        if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {

            double[] speed = MovingUtil.calculateDirection(speedS.getFloatValue());
            mc.player.addVelocity(speed[0], 0, speed[1]);

            double motionY = mc.options.jumpKey.isPressed() ? 1.2 : mc.options.sneakKey.isPressed() ? -2 : 0;

            mc.player.setVelocity(mc.player.getVelocity().x, motionY, mc.player.getVelocity().z);
        }
    }
    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
