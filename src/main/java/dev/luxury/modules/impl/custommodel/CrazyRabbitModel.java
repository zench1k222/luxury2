package dev.luxury.modules.impl.custommodel;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class CrazyRabbitModel extends CustomPlayerModel {

    private final ModelPart rabbitBone;
    private final ModelPart rabbitHead;
    private final ModelPart rabbitLarm;
    private final ModelPart rabbitRarm;
    private final ModelPart rabbitLleg;
    private final ModelPart rabbitRleg;

    public CrazyRabbitModel(ModelPart root, ModelPart head, ModelPart body, 
                           ModelPart rightArm, ModelPart leftArm, 
                           ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        this.rabbitBone = root.getChild("rabbitBone");
        this.rabbitHead = rabbitBone != null ? rabbitBone.getChild("rabbitHead") : null;
        this.rabbitLarm = rabbitBone != null ? rabbitBone.getChild("rabbitLarm") : null;
        this.rabbitRarm = rabbitBone != null ? rabbitBone.getChild("rabbitRarm") : null;
        this.rabbitLleg = rabbitBone != null ? rabbitBone.getChild("rabbitLleg") : null;
        this.rabbitRleg = rabbitBone != null ? rabbitBone.getChild("rabbitRleg") : null;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData head = modelPartData.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_arm = modelPartData.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData left_arm = modelPartData.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_leg = modelPartData.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData left_leg = modelPartData.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData rabbitBone = modelPartData.addChild("rabbitBone", ModelPartBuilder.create().uv(28, 45).cuboid(-5.0F, -13.0F, -5.0F, 10, 11, 8), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData rabbitHead = rabbitBone.addChild("rabbitHead", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, 0.0F, -4.0F, 6, 1, 6).uv(56, 0).cuboid(-5.0F, -9.0F, -5.0F, 2, 3, 2).uv(56, 0).cuboid(3.0F, -9.0F, -5.0F, 2, 3, 2).uv(0, 45).cuboid(-4.0F, -11.0F, -4.0F, 8, 11, 8).uv(46, 0).cuboid(1.0F, -20.0F, 0.0F, 3, 9, 1).uv(46, 0).cuboid(-4.0F, -20.0F, 0.0F, 3, 9, 1), ModelTransform.pivot(0.0F, -14.0F, -1.0F));

        ModelPartData rabbitLarm = rabbitBone.addChild("rabbitLarm", ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, -2.0F, 2, 8, 4), ModelTransform.of(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F));

        ModelPartData rabbitRarm = rabbitBone.addChild("rabbitRarm", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 2, 8, 4), ModelTransform.of(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F));

        ModelPartData rabbitLleg = rabbitBone.addChild("rabbitLleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4, 2, 4), ModelTransform.pivot(3.0F, -2.0F, -1.0F));

        ModelPartData rabbitRleg = rabbitBone.addChild("rabbitRleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4, 2, 4), ModelTransform.pivot(-3.0F, -2.0F, -1.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {

        if (rabbitHead != null && sourceHead != null) {
            rabbitHead.pitch = sourceHead.pitch;
            rabbitHead.yaw = sourceHead.yaw;
            rabbitHead.roll = sourceHead.roll;
        }
        
        if (rabbitLarm != null && sourceLeftArm != null) {
            rabbitLarm.pitch = sourceLeftArm.pitch;
            rabbitLarm.yaw = sourceLeftArm.yaw;
            rabbitLarm.roll = sourceLeftArm.roll - 0.0873F;
        }
        
        if (rabbitRarm != null && sourceRightArm != null) {
            rabbitRarm.pitch = sourceRightArm.pitch;
            rabbitRarm.yaw = sourceRightArm.yaw;
            rabbitRarm.roll = sourceRightArm.roll + 0.0873F;
        }
        
        if (rabbitRleg != null && sourceRightLeg != null) {
            rabbitRleg.pitch = sourceRightLeg.pitch;
            rabbitRleg.yaw = sourceRightLeg.yaw;
            rabbitRleg.roll = sourceRightLeg.roll;
        }
        
        if (rabbitLleg != null && sourceLeftLeg != null) {
            rabbitLleg.pitch = sourceLeftLeg.pitch;
            rabbitLleg.yaw = sourceLeftLeg.yaw;
            rabbitLleg.roll = sourceLeftLeg.roll;
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.scale(1.25F, 1.25F, 1.25F);
        matrices.translate(0.0F, -0.3F, 0.0F);
        
        if (rabbitBone != null) {
            rabbitBone.render(matrices, vertices, light, overlay);
        }
        matrices.pop();
    }
}
