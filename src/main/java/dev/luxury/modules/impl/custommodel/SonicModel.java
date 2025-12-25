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

    private final ModelPart bbMain;
    private final ModelPart sonicHead;
    private final ModelPart rleg;
    private final ModelPart lleg;
    private final ModelPart larm;
    private final ModelPart rarm;

    public SonicModel(ModelPart root, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);

        this.bbMain = root.getChild("bbMain");
        this.sonicHead = root.getChild("sonicHead");
        this.rleg = root.getChild("rleg");
        this.lleg = root.getChild("lleg");
        this.larm = root.getChild("larm");
        this.rarm = root.getChild("rarm");

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

        modelPartData.addChild("bbMain",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-3.0F, -17.0F, -2.0F, 5.0F, 7.0F, 5.0F)
                        .uv(0, 22)
                        .cuboid(-3.0F, -16.0F, -3.0F, 5.0F, 5.0F, 1.0F)
                        .uv(0, 0)
                        .cuboid(-3.0F, -16.0F, 3.0F, 5.0F, 5.0F, 1.0F)
                        .uv(0, 0)
                        .cuboid(0.0F, -15.0F, 3.0F, 1.0F, 1.0F, 3.0F)
                        .uv(0, 0)
                        .cuboid(-3.0F, -14.0F, 4.0F, 1.0F, 1.0F, 2.0F)
                        .uv(0, 0)
                        .cuboid(-1.0F, -11.0F, 3.0F, 1.0F, 1.0F, 2.0F)
                        .uv(0, 0)
                        .cuboid(0.0F, -14.0F, 6.0F, 1.0F, 1.0F, 1.0F)
                        .uv(0, 0)
                        .cuboid(-3.0F, -13.0F, 6.0F, 1.0F, 1.0F, 1.0F)
                        .uv(0, 0)
                        .cuboid(-1.0F, -12.0F, 5.0F, 1.0F, 1.0F, 1.0F)
                        .uv(0, 0)
                        .cuboid(-4.0F, -16.0F, -2.0F, 1.0F, 5.0F, 5.0F)
                        .uv(0, 0)
                        .cuboid(2.0F, -16.0F, -2.0F, 1.0F, 5.0F, 5.0F),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData sonicHeadData = modelPartData.addChild("sonicHead",
                ModelPartBuilder.create()
                        .uv(36, 3)
                        .cuboid(-3.5F, -7.0F, -3.0F, 7.0F, 7.0F, 7.0F)
                        .uv(60, 0)
                        .cuboid(-0.5F, -3.0F, -4.0F, 1.0F, 1.0F, 1.0F),
                ModelTransform.pivot(-0.5F, 7.0F, 0.0F));

        sonicHeadData.addChild("spike1",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-3.5F, -4.0F, 6.0F, 3.0F, 3.0F, 3.0F)
                        .uv(0, 0)
                        .cuboid(-3.5F, -5.0F, 4.0F, 3.0F, 3.0F, 2.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        sonicHeadData.addChild("spike2",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(0.5F, -6.0F, 6.0F, 3.0F, 3.0F, 3.0F)
                        .uv(0, 0)
                        .cuboid(0.5F, -7.0F, 4.0F, 3.0F, 3.0F, 2.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        sonicHeadData.addChild("spike3",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-2.5F, -9.0F, 3.0F, 3.0F, 3.0F, 2.0F)
                        .uv(0, 0)
                        .cuboid(-2.5F, -10.0F, 5.0F, 3.0F, 3.0F, 3.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        modelPartData.addChild("rleg",
                ModelPartBuilder.create()
                        .uv(53, 54)
                        .cuboid(-1.5F, 7.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .uv(0, 0)
                        .cuboid(-1.0F, -1.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .uv(52, 58)
                        .cuboid(-1.5F, 8.0F, -3.0F, 2.0F, 2.0F, 4.0F)
                        .uv(52, 58)
                        .cuboid(-2.0F, 9.0F, -2.0F, 0.0F, 1.0F, 1.0F),
                ModelTransform.pivot(-3.0F, 14.0F, 0.5F));

        modelPartData.addChild("lleg",
                ModelPartBuilder.create()
                        .uv(53, 54)
                        .cuboid(-0.5F, 7.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .uv(0, 0)
                        .cuboid(0.0F, -1.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .uv(52, 58)
                        .cuboid(-0.5F, 8.0F, -3.0F, 2.0F, 2.0F, 4.0F)
                        .uv(52, 58)
                        .cuboid(1.5F, 9.0F, -2.0F, 0.0F, 1.0F, 1.0F),
                ModelTransform.pivot(2.0F, 14.0F, 0.5F));

        modelPartData.addChild("larm",
                ModelPartBuilder.create()
                        .uv(0, 60)
                        .cuboid(-0.5F, 8.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                        .uv(60, 17)
                        .cuboid(0.0F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                ModelTransform.of(3.0F, 9.0F, 0.5F, 0.0F, 0.0F, -0.0873F));

        modelPartData.addChild("rarm",
                ModelPartBuilder.create()
                        .uv(60, 17)
                        .cuboid(-1.0F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .uv(0, 60)
                        .cuboid(-1.5F, 8.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                ModelTransform.of(-4.0F, 9.0F, 0.5F, 0.0F, 0.0F, 0.0873F));


        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {
        if (sourceHead == null || sourceRightArm == null || sourceLeftArm == null ||
                sourceRightLeg == null || sourceLeftLeg == null) {
            return;
        }

        this.sonicHead.pitch = sourceHead.pitch;
        this.sonicHead.yaw = sourceHead.yaw;
        this.sonicHead.roll = sourceHead.roll;

        this.larm.pitch = sourceLeftArm.pitch;
        this.larm.yaw = sourceLeftArm.yaw;
        this.larm.roll = sourceLeftArm.roll - 0.0873F;

        this.rarm.pitch = sourceRightArm.pitch;
        this.rarm.yaw = sourceRightArm.yaw;
        this.rarm.roll = sourceRightArm.roll + 0.0873F;

        this.rleg.pitch = sourceRightLeg.pitch;
        this.rleg.yaw = sourceRightLeg.yaw;
        this.rleg.roll = sourceRightLeg.roll;

        this.lleg.pitch = sourceLeftLeg.pitch;
        this.lleg.yaw = sourceLeftLeg.yaw;
        this.lleg.roll = sourceLeftLeg.roll;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.scale(1.25F, 1.25F, 1.25F);
        matrices.translate(0.0F, -0.3F, 0.0F);

        if (bbMain != null) bbMain.render(matrices, vertices, light, overlay);
        if (sonicHead != null) sonicHead.render(matrices, vertices, light, overlay);
        if (rleg != null) rleg.render(matrices, vertices, light, overlay);
        if (lleg != null) lleg.render(matrices, vertices, light, overlay);
        if (larm != null) larm.render(matrices, vertices, light, overlay);
        if (rarm != null) rarm.render(matrices, vertices, light, overlay);

        matrices.pop();
    }
}