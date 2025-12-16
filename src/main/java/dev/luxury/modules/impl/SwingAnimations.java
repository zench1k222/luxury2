package dev.luxury.modules.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.HandAnimationEvent;
import dev.luxury.events.impl.client.HandOffsetEvent;
import dev.luxury.events.impl.client.SwingDurationEvent;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ModuleAnnotation(
        name = "SwingAnimations",
        desc = "",
        category =  Category.Render
)
public class SwingAnimations extends Module {

    ModeSetting swingType = new ModeSetting("Тип","Плавная",new String[]{"Плавная","Топчек","Сила","Свайпич","Вниз"});

    public BooleanSetting onAura = new BooleanSetting("Только с KillAura", false);


SliderSetting mainHandX = new SliderSetting("MainHandX",0f,-30,30,0.1f);

    SliderSetting mainHandY = new SliderSetting("MainHandY",0f,-30,30,0.1f);

    SliderSetting mainHandZ = new SliderSetting("MainHandZ",0f,-30,30,0.1f);

    SliderSetting offHandX = new SliderSetting("offHandX",0f,-30,30,0.1f);
    SliderSetting offHandY = new SliderSetting("offHandY",0f,-30,30,0.1f);
    SliderSetting offHandZ = new SliderSetting("offHandZ",0f,-30,30,0.1f);
    boolean swingGroupEnabled = true;
    boolean offsetGroupEnabled = true;
MatrixStack stack = new MatrixStack();
    float swingSpeed =2F;
public SwingAnimations(){
    addSettings(swingType,onAura,mainHandX,mainHandY,mainHandZ,offHandX,offHandY,offHandZ);
}

    @EventTarget
    public void onSwingDuration(SwingDurationEvent e) {

        if (onAura.get() && !KillAura.state) return;

        e.setAnimation(swingSpeed);
        e.setCancelled(true);
    }

    @EventTarget
    public void onHandAnimation(HandAnimationEvent e) {

        if (onAura.get() && !KillAura.state) return;

        if (e.getHand() == Hand.MAIN_HAND && swingGroupEnabled) {
            MatrixStack matrix = e.getMatrices();
            float swingProgress = e.getSwingProgress();
            int i = mc.player.getMainArm() == Arm.RIGHT ? 1 : -1;

            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);

            switch (swingType.get()) {
                case "Свайпич" -> {
                    matrix.translate(0.56F * i, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                }
                case "Вниз" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5));
                    matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                }
                case "Плавная" -> {
                    matrix.translate(i * 0.56F, -0.42F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F)));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                    matrix.translate(0, -0.1, 0);
                }
                case "Сила" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.translate((-sinSmooth * sinSmooth * sin1) * i, 0, 0);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60));
                }
                case "Топчек" -> {
                    matrix.translate(i * 0.56F, -0.32F, -0.72F);
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                    matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                    matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                }
            }

            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onHandOffset(HandOffsetEvent e) {

        if (onAura.get() && !KillAura.state) return;

        Hand hand = e.getHand();
        if (hand == Hand.MAIN_HAND && e.getStack().getItem() instanceof CrossbowItem) return;

        if (!offsetGroupEnabled) return;

        MatrixStack matrix = e.getMatrices();

        if (hand == Hand.MAIN_HAND) {
            matrix.translate(
                    mainHandX.getValue(),
                    mainHandY.getValue(),
                    mainHandZ.getValue()
            );
        } else {
            matrix.translate(
                    offHandX.getValue(),
                    offHandY.getValue(),
                    offHandZ.getValue()
            );
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
