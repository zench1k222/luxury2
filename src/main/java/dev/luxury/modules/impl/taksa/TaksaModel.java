package dev.luxury.modules.impl.taksa;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TaksaModel {
    public ModelPart head;
    public ModelPart body;
    public ModelPart neck;
    public ModelPart chest;
    public ModelPart back;
    public ModelPart frontLeftLeg;
    public ModelPart frontRightLeg;
    public ModelPart leftBackLeg;
    public ModelPart rightBackLeg;
    public ModelPart tail;
    public ModelPart leftEar;
    public ModelPart rightEar;

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F).uv(21, 0).cuboid(-1.5F, 0.0F, -7.0F, 3.0F, 3.0F, 3.0F), ModelTransform.pivot(0.0F, 10.5F, -6.8F));

        ModelPartData leftEar = head.addChild("leftEar", ModelPartBuilder.create().uv(32, 4).cuboid(0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F).uv(34, 1).cuboid(0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F), ModelTransform.pivot(3.0F, 3.0F, -2.0F));

        ModelPartData rightEar = head.addChild("rightEar", ModelPartBuilder.create().uv(32, 4).cuboid(-1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F).uv(34, 1).cuboid(-1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F), ModelTransform.pivot(-3.0F, 3.0F, -2.0F));

        ModelPartData neck = root.addChild("neck", ModelPartBuilder.create().uv(15, 7).cuboid(-2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F), ModelTransform.of(0.0F, 10.5F, -5.0F, -0.43633232F, 0.0F, 0.0F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 13.5F, -5.0F));

        ModelPartData chest = body.addChild("chest", ModelPartBuilder.create().uv(32, 13).cuboid(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F), ModelTransform.pivot(0.0F, 0.0F, 3.0F));

        ModelPartData back = body.addChild("back", ModelPartBuilder.create().uv(3, 19).cuboid(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F), ModelTransform.pivot(0.0F, -0.5F, 5.5F));

        ModelPartData frontLeftLeg = root.addChild("frontLeftLeg", ModelPartBuilder.create().uv(42, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), ModelTransform.pivot(1.5F, 16.0F, -3.0F));

        ModelPartData frontRightLeg = root.addChild("frontRightLeg", ModelPartBuilder.create().uv(42, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), ModelTransform.pivot(-1.5F, 16.0F, -3.0F));

        ModelPartData leftBackLeg = root.addChild("leftBackLeg", ModelPartBuilder.create().uv(52, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), ModelTransform.pivot(1.5F, 16.0F, 9.0F));

        ModelPartData rightBackLeg = root.addChild("rightBackLeg", ModelPartBuilder.create().uv(52, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), ModelTransform.pivot(-1.5F, 16.0F, 9.0F));

        ModelPartData tail = root.addChild("tail", ModelPartBuilder.create().uv(2, 12).cuboid(-1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.of(0.0F, 9.0F, 10.0F, (float)Math.PI / 8F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 60, 36);
    }

    public TaksaModel(ModelPart root) {
        this.head = root.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.back = this.body.getChild("back");
        this.frontLeftLeg = root.getChild("frontLeftLeg");
        this.frontRightLeg = root.getChild("frontRightLeg");
        this.leftBackLeg = root.getChild("leftBackLeg");
        this.rightBackLeg = root.getChild("rightBackLeg");
        this.tail = root.getChild("tail");
    }

    public void setRotationAngles(float ageInTicks, TaksaBrain brain) {
        this.head.pitch = brain.getPitch() * ((float)Math.PI / 180F);
        this.head.yaw = brain.getYaw() * ((float)Math.PI / 180F);
        
        this.frontLeftLeg.pitch = (float)Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        this.frontRightLeg.pitch = (float)Math.cos(brain.limbSwing * 0.6662F + (float)Math.PI) * 1.4F * brain.limbSwingAmount;
        this.leftBackLeg.pitch = (float)Math.cos(brain.limbSwing * 0.6662F + (float)Math.PI) * 1.4F * brain.limbSwingAmount;
        this.rightBackLeg.pitch = (float)Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        
        if (brain.isLay()) {
            this.frontLeftLeg.pitch = (float)Math.toRadians(-90.0F);
            this.frontRightLeg.pitch = (float)Math.toRadians(-90.0F);
            this.leftBackLeg.pitch = (float)Math.toRadians(90.0F);
            this.rightBackLeg.pitch = (float)Math.toRadians(90.0F);
            this.frontLeftLeg.yaw = (float)Math.toRadians(-22.0F);
            this.frontRightLeg.yaw = (float)Math.toRadians(22.0F);
            this.leftBackLeg.yaw = (float)Math.toRadians(22.0F);
            this.rightBackLeg.yaw = (float)Math.toRadians(-22.0F);
        } else {
            this.frontLeftLeg.yaw = 0.0F;
            this.frontRightLeg.yaw = 0.0F;
            this.leftBackLeg.yaw = 0.0F;
            this.rightBackLeg.yaw = 0.0F;
        }

        this.tail.pitch = (float)Math.toRadians(brain.isLay() ? 45.0F : 22.0F);
        this.tail.roll = (float)(Math.toRadians(-22.5F) + (double)((float)Math.PI / 8F) + (double)((float)Math.cos(ageInTicks * 0.15F) * 0.3F));
    }

    public void render(MatrixStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, TaksaBrain brain, Identifier texture) {
        matrixStackIn.push();
        matrixStackIn.translate(0.0F, 1.2F - (brain.isLay() ? 0.3F : 0.0F), 0.0F);
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBody()));
        
        VertexConsumer vertexConsumer = bufferIn;
        
        this.head.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.neck.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.body.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.frontLeftLeg.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.frontRightLeg.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.leftBackLeg.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.rightBackLeg.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        this.tail.render(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        
        matrixStackIn.pop();
    }

}

