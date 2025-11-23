package dev.luxury.modules.impl.custommodel;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class SonicModel extends CustomPlayerModel {

    private final ModelPart sonicBone;
    private final ModelPart sonicHead;
    private final ModelPart sonicRleg;
    private final ModelPart sonicLleg;
    private final ModelPart sonicLarm;
    private final ModelPart sonicRarm;

    public SonicModel(ModelPart root, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        this.sonicBone = root.getChild("sonicBone");
        if (sonicBone != null) {
            this.sonicHead = sonicBone.getChild("sonicHead");
            this.sonicRleg = sonicBone.getChild("sonicRleg");
            this.sonicLleg = sonicBone.getChild("sonicLleg");
            this.sonicLarm = sonicBone.getChild("sonicLarm");
            this.sonicRarm = sonicBone.getChild("sonicRarm");
        } else {
            this.sonicHead = null;
            this.sonicRleg = null;
            this.sonicLleg = null;
            this.sonicLarm = null;
            this.sonicRarm = null;
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        float off = 24.0F;
        ModelPartData sonicBone = root.addChild("sonicBone", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-3.0F, -17.0F, -2.0F, 5, 7, 5)
                .uv(0, 22).cuboid(-3.0F, -16.0F, -3.0F, 5, 5, 1)
                .uv(0, 0).cuboid(-3.0F, -16.0F, 3.0F, 5, 5, 1)
                .uv(0, 0).cuboid(0.0F, -15.0F, 3.0F, 1, 1, 3)
                .uv(0, 0).cuboid(-3.0F, -14.0F, 4.0F, 1, 1, 2)
                .uv(0, 0).cuboid(-1.0F, -11.0F, 3.0F, 1, 1, 2)
                .uv(0, 0).cuboid(0.0F, -14.0F, 6.0F, 1, 1, 1)
                .uv(0, 0).cuboid(-3.0F, -13.0F, 6.0F, 1, 1, 1)
                .uv(0, 0).cuboid(-1.0F, -12.0F, 5.0F, 1, 1, 1)
                .uv(0, 0).cuboid(-4.0F, -16.0F, -2.0F, 1, 5, 5)
                .uv(0, 0).cuboid(2.0F, -16.0F, -2.0F, 1, 5, 5),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData sonicRleg = sonicBone.addChild("sonicRleg", ModelPartBuilder.create()
                .uv(53, 54).cuboid(-1.5F, 7.0F, -1.0F, 2, 1, 2)
                .uv(0, 0).cuboid(-1.0F, -1.0F, -0.5F, 1, 8, 1)
                .uv(52, 58).cuboid(-1.5F, 8.0F, -3.0F, 2, 2, 4)
                .uv(52, 58).cuboid(-2.0F, 9.0F, -2.0F, 0, 1, 1),
                ModelTransform.pivot(-3.0F, 14.0F - off, 0.5F));

        ModelPartData sonicLleg = sonicBone.addChild("sonicLleg", ModelPartBuilder.create()
                .uv(53, 54).cuboid(-0.5F, 7.0F, -1.0F, 2, 1, 2)
                .uv(0, 0).cuboid(0.0F, -1.0F, -0.5F, 1, 8, 1)
                .uv(52, 58).cuboid(-0.5F, 8.0F, -3.0F, 2, 2, 4)
                .uv(52, 58).cuboid(1.5F, 9.0F, -2.0F, 0, 1, 1),
                ModelTransform.pivot(2.0F, 14.0F - off, 0.5F));

        ModelPartData sonicLarm = sonicBone.addChild("sonicLarm", ModelPartBuilder.create()
                .uv(0, 60).cuboid(-0.5F, 8.0F, -1.0F, 2, 2, 2)
                .uv(60, 17).cuboid(0.0F, 0.0F, -0.5F, 1, 8, 1),
                ModelTransform.of(3.0F, 9.0F - off, 0.5F, 0.0F, 0.0F, -0.0873F));

        ModelPartData sonicRarm = sonicBone.addChild("sonicRarm", ModelPartBuilder.create()
                .uv(60, 17).cuboid(-1.0F, 0.0F, -0.5F, 1, 8, 1)
                .uv(0, 60).cuboid(-1.5F, 8.0F, -1.0F, 2, 2, 2),
                ModelTransform.of(-4.0F, 9.0F - off, 0.5F, 0.0F, 0.0F, 0.0873F));

        ModelPartData sonicHead = sonicBone.addChild("sonicHead", ModelPartBuilder.create()
                .uv(36, 3).cuboid(-3.5F, -7.0F, -3.0F, 7, 7, 7)
                .uv(60, 0).cuboid(-0.5F, -3.0F, -4.0F, 1, 1, 1)
                .uv(0, 0).cuboid(-3.5F, -4.0F, 6.0F, 3, 3, 3)
                .uv(0, 0).cuboid(-3.5F, -5.0F, 4.0F, 3, 3, 2)
                .uv(0, 0).cuboid(0.5F, -6.0F, 6.0F, 3, 3, 3)
                .uv(0, 0).cuboid(0.5F, -7.0F, 4.0F, 3, 3, 2)
                .uv(0, 0).cuboid(-2.5F, -9.0F, 3.0F, 3, 3, 2)
                .uv(0, 0).cuboid(-2.5F, -10.0F, 5.0F, 3, 3, 3),
                ModelTransform.pivot(-0.5F, 7.0F - off, 0.0F));

        return TexturedModelData.of(modelData, 128, 64);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {
        if (sonicHead != null && sourceHead != null) {
            sonicHead.pitch = sourceHead.pitch;
            sonicHead.yaw = sourceHead.yaw;
            sonicHead.roll = sourceHead.roll;
        }

        if (sonicLarm != null && sourceLeftArm != null) {
            sonicLarm.pitch = sourceLeftArm.pitch;
            sonicLarm.yaw = sourceLeftArm.yaw;
            sonicLarm.roll = sourceLeftArm.roll - 0.0873F;
        }

        if (sonicRarm != null && sourceRightArm != null) {
            sonicRarm.pitch = sourceRightArm.pitch;
            sonicRarm.yaw = sourceRightArm.yaw;
            sonicRarm.roll = sourceRightArm.roll + 0.0873F;
        }

        if (sonicRleg != null && sourceRightLeg != null) {
            sonicRleg.pitch = sourceRightLeg.pitch;
            sonicRleg.yaw = sourceRightLeg.yaw;
            sonicRleg.roll = sourceRightLeg.roll;
        }

        if (sonicLleg != null && sourceLeftLeg != null) {
            sonicLleg.pitch = sourceLeftLeg.pitch;
            sonicLleg.yaw = sourceLeftLeg.yaw;
            sonicLleg.roll = sourceLeftLeg.roll;
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.scale(1.3F, 1.3F, 1.3F);
        matrices.translate(0.0F, -0.35F, 0.0F);

        if (sonicBone != null) {
            sonicBone.render(matrices, vertices, light, overlay);
        }

        matrices.pop();
    }
}