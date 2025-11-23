package dev.luxury.modules.impl.custommodel;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class FreddyBearModel extends CustomPlayerModel {

    private final ModelPart fredbody;
    private final ModelPart fredhead;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;

    public FreddyBearModel(ModelPart root, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        this.fredbody = root.getChild("fredbody");
        if (fredbody != null) {
            this.fredhead = fredbody.getChild("fredhead");
            this.armLeft = fredbody.getChild("armLeft");
            this.armRight = fredbody.getChild("armRight");
            this.legLeft = fredbody.getChild("legLeft");
            this.legRight = fredbody.getChild("legRight");
        } else {
            this.fredhead = null;
            this.armLeft = null;
            this.armRight = null;
            this.legLeft = null;
            this.legRight = null;
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData body_root = modelPartData.addChild("body_root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("torso", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        body_root.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData fredbody = modelPartData.addChild("fredbody", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -14.0F, -1.0F, 2, 24, 2), ModelTransform.pivot(0.0F, -9.0F, 0.0F));

        ModelPartData torso = fredbody.addChild("torso", ModelPartBuilder.create().uv(8, 0).cuboid(-6.0F, -9.0F, -4.0F, 12, 18, 8), ModelTransform.of(0.0F, 0.0F, 0.0F, (float)Math.PI / 180, 0.0F, 0.0F));

        ModelPartData crotch = fredbody.addChild("crotch", ModelPartBuilder.create().uv(56, 0).cuboid(-5.5F, 0.0F, -3.5F, 11, 3, 7), ModelTransform.pivot(0.0F, 9.5F, 0.0F));

        ModelPartData fredhead = fredbody.addChild("fredhead", ModelPartBuilder.create().uv(39, 22).cuboid(-5.5F, -8.0F, -4.5F, 11, 8, 9), ModelTransform.pivot(0.0F, -13.0F, -0.5F));

        ModelPartData jaw = fredhead.addChild("jaw", ModelPartBuilder.create().uv(49, 65).cuboid(-5.0F, 0.0F, -4.5F, 10, 3, 9), ModelTransform.of(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));

        ModelPartData frednose = fredhead.addChild("frednose", ModelPartBuilder.create().uv(17, 67).cuboid(-4.0F, -2.0F, -3.0F, 8, 4, 3), ModelTransform.pivot(0.0F, -2.0F, -4.5F));

        ModelPartData earRight = fredhead.addChild("earRight", ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -3.0F, -0.5F, 2, 3, 1), ModelTransform.of(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, -1.0471976F));

        ModelPartData earRightpad = earRight.addChild("earRightpad", ModelPartBuilder.create().uv(85, 0).cuboid(-2.0F, -5.0F, -1.0F, 4, 4, 2), ModelTransform.pivot(0.0F, -1.0F, 0.0F));

        ModelPartData earLeft = fredhead.addChild("earLeft", ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, -3.0F, -0.5F, 2, 3, 1), ModelTransform.of(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, 1.0471976F));

        ModelPartData earRightpad_1 = earLeft.addChild("earRightpad_1", ModelPartBuilder.create().uv(40, 39).cuboid(-2.0F, -5.0F, -1.0F, 4, 4, 2), ModelTransform.pivot(0.0F, -1.0F, 0.0F));

        ModelPartData hat = fredhead.addChild("hat", ModelPartBuilder.create().uv(70, 24).cuboid(-3.0F, -0.5F, -3.0F, 6, 1, 6), ModelTransform.of(0.0F, -8.4F, 0.0F, (float)(-Math.PI) / 180, 0.0F, 0.0F));

        ModelPartData hat2 = hat.addChild("hat2", ModelPartBuilder.create().uv(78, 61).cuboid(-2.0F, -4.0F, -2.0F, 4, 4, 4), ModelTransform.of(0.0F, 0.1F, 0.0F, (float)(-Math.PI) / 180, 0.0F, 0.0F));

        ModelPartData armRight = fredbody.addChild("armRight", ModelPartBuilder.create().uv(48, 0).cuboid(-1.0F, 0.0F, -1.0F, 2, 10, 2), ModelTransform.of(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.2617994F));

        ModelPartData armRightpad = armRight.addChild("armRightpad", ModelPartBuilder.create().uv(70, 10).cuboid(-2.5F, 0.0F, -2.5F, 5, 9, 5), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData armRight2 = armRight.addChild("armRight2", ModelPartBuilder.create().uv(90, 20).cuboid(-1.0F, 0.0F, -1.0F, 2, 8, 2), ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));

        ModelPartData armRightpad2 = armRight2.addChild("armRightpad2", ModelPartBuilder.create().uv(0, 26).cuboid(-2.5F, 0.0F, -2.5F, 5, 7, 5), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData handRight = armRight2.addChild("handRight", ModelPartBuilder.create().uv(20, 26).cuboid(-2.0F, 0.0F, -2.5F, 4, 4, 5), ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F));

        ModelPartData armLeft = fredbody.addChild("armLeft", ModelPartBuilder.create().uv(62, 10).cuboid(-1.0F, 0.0F, -1.0F, 2, 10, 2), ModelTransform.of(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2617994F));

        ModelPartData armLeftpad = armLeft.addChild("armLeftpad", ModelPartBuilder.create().uv(38, 54).cuboid(-2.5F, 0.0F, -2.5F, 5, 9, 5), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData armLeft2 = armLeft.addChild("armLeft2", ModelPartBuilder.create().uv(90, 48).cuboid(-1.0F, 0.0F, -1.0F, 2, 8, 2), ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));

        ModelPartData armLeftpad2 = armLeft2.addChild("armLeftpad2", ModelPartBuilder.create().uv(0, 58).cuboid(-2.5F, 0.0F, -2.5F, 5, 7, 5), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData handLeft = armLeft2.addChild("handLeft", ModelPartBuilder.create().uv(58, 56).cuboid(-1.0F, 0.0F, -2.5F, 4, 4, 5), ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F));

        ModelPartData legRight = fredbody.addChild("legRight", ModelPartBuilder.create().uv(90, 8).cuboid(-1.0F, 0.0F, -1.0F, 2, 10, 2), ModelTransform.pivot(-3.3F, 12.5F, 0.0F));

        ModelPartData legRightpad = legRight.addChild("legRightpad", ModelPartBuilder.create().uv(73, 33).cuboid(-3.0F, 0.0F, -3.0F, 6, 9, 6), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData legRight2 = legRight.addChild("legRight2", ModelPartBuilder.create().uv(20, 35).cuboid(-1.0F, 0.0F, -1.0F, 2, 8, 2), ModelTransform.of(0.0F, 9.6F, 0.0F, (float)Math.PI / 90, 0.0F, 0.0F));

        ModelPartData legRightpad2 = legRight2.addChild("legRightpad2", ModelPartBuilder.create().uv(0, 39).cuboid(-2.5F, 0.0F, -3.0F, 5, 7, 6), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData footRight = legRight2.addChild("footRight", ModelPartBuilder.create().uv(22, 39).cuboid(-2.5F, 0.0F, -6.0F, 5, 3, 8), ModelTransform.of(0.0F, 8.0F, 0.0F, (float)(-Math.PI) / 90, 0.0F, 0.0F));

        ModelPartData legLeft = fredbody.addChild("legLeft", ModelPartBuilder.create().uv(54, 10).cuboid(-1.0F, 0.0F, -1.0F, 2, 10, 2), ModelTransform.pivot(3.3F, 12.5F, 0.0F));

        ModelPartData legLeftpad = legLeft.addChild("legLeftpad", ModelPartBuilder.create().uv(48, 39).cuboid(-3.0F, 0.0F, -3.0F, 6, 9, 6), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData legLeft2 = legLeft.addChild("legLeft2", ModelPartBuilder.create().uv(72, 48).cuboid(-1.0F, 0.0F, -1.0F, 2, 8, 2), ModelTransform.of(0.0F, 9.6F, 0.0F, (float)Math.PI / 90, 0.0F, 0.0F));

        ModelPartData legLeftpad2 = legLeft2.addChild("legLeftpad2", ModelPartBuilder.create().uv(16, 50).cuboid(-2.5F, 0.0F, -3.0F, 5, 7, 6), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData footLeft = legLeft2.addChild("footLeft", ModelPartBuilder.create().uv(72, 50).cuboid(-2.5F, 0.0F, -6.0F, 5, 3, 8), ModelTransform.of(0.0F, 8.0F, 0.0F, (float)(-Math.PI) / 90, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 100, 80);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {
        if (fredhead != null && sourceHead != null) {
            fredhead.pitch = sourceHead.pitch;
            fredhead.yaw = sourceHead.yaw;
            fredhead.roll = sourceHead.roll;
        }
        
        if (armLeft != null && sourceLeftArm != null) {
            armLeft.pitch = sourceLeftArm.pitch;
            armLeft.yaw = sourceLeftArm.yaw;
            armLeft.roll = sourceLeftArm.roll;
        }
        
        if (armRight != null && sourceRightArm != null) {
            armRight.pitch = sourceRightArm.pitch;
            armRight.yaw = sourceRightArm.yaw;
            armRight.roll = sourceRightArm.roll;
        }
        
        if (legRight != null && sourceRightLeg != null) {
            legRight.pitch = sourceRightLeg.pitch;
            legRight.yaw = sourceRightLeg.yaw;
            legRight.roll = sourceRightLeg.roll;
        }
        
        if (legLeft != null && sourceLeftLeg != null) {
            legLeft.pitch = sourceLeftLeg.pitch;
            legLeft.yaw = sourceLeftLeg.yaw;
            legLeft.roll = sourceLeftLeg.roll;
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.scale(0.75F, 0.65F, 0.75F);
        matrices.translate(0.0F, 0.85F, 0.0F);
        
        if (fredbody != null) {
            fredbody.render(matrices, vertices, light, overlay);
        }
        
        matrices.pop();
    }
}
