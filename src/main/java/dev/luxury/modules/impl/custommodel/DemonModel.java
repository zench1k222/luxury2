package dev.luxury.modules.impl.custommodel;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class DemonModel extends CustomPlayerModel {

    public DemonModel(ModelPart root, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {

        if (head != null && sourceHead != null) {
            head.pitch = sourceHead.pitch;
            head.yaw = sourceHead.yaw;
            head.roll = sourceHead.roll;
        }
        
        if (leftArm != null && sourceLeftArm != null) {
            leftArm.pitch = sourceLeftArm.pitch;
            leftArm.yaw = sourceLeftArm.yaw;
            leftArm.roll = sourceLeftArm.roll;
        }
        
        if (rightArm != null && sourceRightArm != null) {
            rightArm.pitch = sourceRightArm.pitch;
            rightArm.yaw = sourceRightArm.yaw;
            rightArm.roll = sourceRightArm.roll;
        }
        
        if (leftLeg != null && sourceLeftLeg != null) {
            leftLeg.pitch = sourceLeftLeg.pitch;
            leftLeg.yaw = sourceLeftLeg.yaw;
            leftLeg.roll = sourceLeftLeg.roll;
        }
        
        if (rightLeg != null && sourceRightLeg != null) {
            rightLeg.pitch = sourceRightLeg.pitch;
            rightLeg.yaw = sourceRightLeg.yaw;
            rightLeg.roll = sourceRightLeg.roll;
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData head = modelPartData.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.3F)), ModelTransform.pivot(0.0F, -6.0F, -1.0F));

        ModelPartData left_horn = head.addChild("left_horn", ModelPartBuilder.create().uv(32, 8).cuboid(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F)).uv(0, 0).cuboid(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)), ModelTransform.of(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F));

        ModelPartData right_horn = head.addChild("right_horn", ModelPartBuilder.create().uv(32, 8).cuboid(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F)).uv(0, 0).cuboid(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)), ModelTransform.of(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F));

        ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F), ModelTransform.of(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F));ModelPartData left_wing = body.addChild("left_wing", ModelPartBuilder.create().uv(40, 12).cuboid(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F), ModelTransform.of(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F));

        ModelPartData right_wing = body.addChild("right_wing", ModelPartBuilder.create().uv(40, 12).cuboid(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F), ModelTransform.of(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F));

        ModelPartData left_arm = modelPartData.addChild("left_arm", ModelPartBuilder.create().uv(24, 16).cuboid(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F), ModelTransform.of(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F));

        ModelPartData right_arm = modelPartData.addChild("right_arm", ModelPartBuilder.create().uv(24, 16).cuboid(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F), ModelTransform.of(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F));

        ModelPartData left_leg = modelPartData.addChild("left_leg", ModelPartBuilder.create().uv(48, 22).cuboid(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(3.0F, 10.0F, 0.0F));

        ModelPartData left_leg1 = left_leg.addChild("left_leg1", ModelPartBuilder.create().uv(34, 34).cuboid(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F), ModelTransform.of(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        ModelPartData bone3 = left_leg1.addChild("bone3", ModelPartBuilder.create(), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F));

        ModelPartData bone7 = bone3.addChild("bone7", ModelPartBuilder.create().uv(16, 34).cuboid(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F).uv(0, 32).cuboid(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(1.9F, 12.0F, 0.25F));

        ModelPartData bone2 = left_leg1.addChild("bone2", ModelPartBuilder.create().uv(26, 0).cuboid(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F).uv(40, 0).cuboid(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F), ModelTransform.of(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        ModelPartData right_leg = modelPartData.addChild("right_leg", ModelPartBuilder.create().uv(48, 22).cuboid(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(-3.0F, 10.0F, 0.0F));

        ModelPartData right_leg3 = right_leg.addChild("right_leg3", ModelPartBuilder.create().uv(34, 34).cuboid(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F), ModelTransform.of(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        ModelPartData bone5 = right_leg3.addChild("bone5", ModelPartBuilder.create(), ModelTransform.of(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F));

        ModelPartData bone6 = bone5.addChild("bone6", ModelPartBuilder.create().uv(16, 34).cuboid(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F).uv(0, 32).cuboid(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(-1.9F, 12.0F, 0.25F));

        ModelPartData bone4 = right_leg3.addChild("bone4", ModelPartBuilder.create().uv(26, 0).cuboid(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F).uv(40, 0).cuboid(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F), ModelTransform.of(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        
        if (root != null) {
            root.render(matrices, vertices, light, overlay);
        }
        
        matrices.pop();
    }
}
