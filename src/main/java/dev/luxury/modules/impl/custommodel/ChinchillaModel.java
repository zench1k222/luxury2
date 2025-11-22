package dev.luxury.modules.impl.custommodel;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class ChinchillaModel extends CustomPlayerModel {

    private final ModelPart chinchillaBone;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightRearFoot;
    private final ModelPart leftRearFoot;

    public ChinchillaModel(ModelPart root, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        super(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        this.chinchillaBone = root.getChild("chinchillaBone");
        if (chinchillaBone != null) {
            this.body = chinchillaBone.getChild("body");
            this.head = this.body != null ? this.body.getChild("head") : null;
            this.rightFrontLeg = this.body != null ? this.body.getChild("rightFrontLeg") : null;
            this.leftFrontLeg = this.body != null ? this.body.getChild("leftFrontLeg") : null;
            this.rightRearFoot = this.body != null ? this.body.getChild("rightRearFoot") : null;
            this.leftRearFoot = this.body != null ? this.body.getChild("leftRearFoot") : null;
        } else {
            this.body = null;
            this.head = null;
            this.rightFrontLeg = null;
            this.leftFrontLeg = null;
            this.rightRearFoot = null;
            this.leftRearFoot = null;
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Пустые части игрока
        root.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData chinchillaBone = root.addChild("chinchillaBone", ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        // Body (поворот -7 градусов по X = -0.122173 радиана)
        ModelPartData body = chinchillaBone.addChild("body",
                ModelPartBuilder.create()
                        .uv(14, 14).cuboid(-7.0F, -7.0F, -7.0F, 14, 14, 14), // Main cube
                ModelTransform.of(0.0F, 8.5F, 0.0F, -0.122173F, 0.0F, 0.0F));

        // Body Back (отдельная часть для поворота)
        body.addChild("bodyBack",
                ModelPartBuilder.create()
                        .uv(6, 34).cuboid(-6.0F, -12.0F, -6.0F, 12, 12, 6), // Back cube
                ModelTransform.of(0.0F, 5.0F, -1.2F, 0.305433F, 0.0F, 0.0F)); // 17.5 градусов = 0.305433 радиана

        // Lower Tail
        ModelPartData lowerTail = body.addChild("lowerTail",
                ModelPartBuilder.create()
                        .uv(10, 56).cuboid(-2.0F, -2.0F, -10.0F, 4, 4, 10),
                ModelTransform.of(0.0F, -4.0F, -7.5F, -0.122173F, 0.0F, 0.0F)); // -7 градусов

        // Upper Tail (поворот 52.4213 градусов = 0.914898 радиана)
        lowerTail.addChild("upperTail",
                ModelPartBuilder.create()
                        .uv(36, 54).cuboid(-3.0F, -3.0F, -8.0F, 6, 6, 8),
                ModelTransform.of(0.0F, 0.8F, -8.3F, 0.914898F, 0.0F, 0.0F));

        // Right Rear Foot (поворот 7 по X, -7 по Y)
        body.addChild("rightRearFoot",
                ModelPartBuilder.create()
                        .uv(64, 26).cuboid(-2.0F, -1.0F, -1.0F, 4, 2, 8),
                ModelTransform.of(-6.5F, -7.0F, -6.8F, 0.122173F, -0.122173F, 0.0F));

        // Left Rear Foot (поворот 7 по X, 7 по Y)
        body.addChild("leftRearFoot",
                ModelPartBuilder.create()
                        .uv(64, 8).cuboid(-2.0F, -1.0F, -1.0F, 4, 2, 8),
                ModelTransform.of(6.4F, -7.0F, -6.8F, 0.122173F, 0.122173F, 0.0F));

        // Right Front Leg (поворот -7 по X. Куб со смещением Z: -2 + (-0.2) = -2.2)
        ModelPartData rightFrontLeg = body.addChild("rightFrontLeg",
                ModelPartBuilder.create()
                        .uv(84, 22).cuboid(-2.0F, -8.0F, -2.2F, 4, 8, 4),
                ModelTransform.of(-6.0F, -1.2F, 6.5F, -0.122173F, 0.0F, 0.0F));

        // Right Front Foot (поворот 14 по X, 2 по Y)
        rightFrontLeg.addChild("rightFrontFoot",
                ModelPartBuilder.create()
                        .uv(62, 34).cuboid(-2.0F, -1.0F, 0.0F, 4, 2, 6),
                ModelTransform.of(0.0F, -7.0F, -2.0F, 0.244346F, 0.034907F, 0.0F));

        // Left Front Leg (поворот -7 по X. Куб со смещением Z: -2 + (-0.2) = -2.2)
        ModelPartData leftFrontLeg = body.addChild("leftFrontLeg",
                ModelPartBuilder.create()
                        .uv(84, 4).cuboid(-2.0F, -8.0F, -2.2F, 4, 8, 4),
                ModelTransform.of(6.0F, -1.2F, 6.5F, -0.122173F, 0.0F, 0.0F));

        // Left Front Foot (поворот 14 по X, -2 по Y. Куб со смещением X: -2 + 0.2 = -1.8)
        leftFrontLeg.addChild("leftFrontFoot",
                ModelPartBuilder.create()
                        .uv(62, 16).cuboid(-1.8F, -1.0F, 0.0F, 4, 2, 6),
                ModelTransform.of(-0.2F, -7.0F, -2.0F, 0.244346F, -0.034907F, 0.0F));

        // Head (поворот 6 градусов по X = 0.104720 радиана)
        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create()
                        .uv(64, 50).cuboid(-5.0F, -5.0F, -1.0F, 10, 10, 8),
                ModelTransform.of(0.0F, 3.5F, 6.0F, 0.104720F, 0.0F, 0.0F));

        // Nose (отдельная часть для поворота 21 градус = 0.366519 радиана)
        head.addChild("nose",
                ModelPartBuilder.create()
                        .uv(96, 52).cuboid(-4.0F, -4.0F, -2.0F, 8, 8, 4), // Размеры из JSON
                ModelTransform.of(0.0F, -0.5F, 6.6F, 0.366519F, 0.0F, 0.0F)); // Позиция и поворот из JSON

        // Chin (поворот -7 по X)
        ModelPartData chin = head.addChild("chin",
                ModelPartBuilder.create()
                        .uv(96, 40).cuboid(-4.0F, -6.0F, -4.0F, 8, 6, 4), // Убран scale
                ModelTransform.of(0.0F, 2.5F, 9.9F, -0.122173F, 0.0F, 0.0F));

        // Right Whiskers (Plane. UV (38, 40), Face Z)
        // В MiModel plane имеет 8x6x8, но cuboid.create().uv().cuboid(x, y, z, 8, 6, 0) создает плоскость 8x6 на оси Z.
        chin.addChild("rightWhiskers",
                ModelPartBuilder.create()
                        .uv(38, 40).cuboid(-8.0F, -3.0F, 0.0F, 8, 6, 0),
                ModelTransform.of(-4.0F, -3.0F, -0.5F, 0.0F, -0.122173F, 0.0F)); // -7 по Y

        // Left Whiskers (Plane. UV (38, 30), Face Z)
        chin.addChild("leftWhiskers",
                ModelPartBuilder.create()
                        .uv(38, 30).cuboid(0.0F, -3.0F, 0.0F, 8, 6, 0),
                ModelTransform.of(4.0F, -3.0F, -0.5F, 0.0F, 0.122173F, 0.0F)); // 7 по Y

        // Left Ear (поворот -49 по X, -63 по Y)
        head.addChild("leftEar",
                ModelPartBuilder.create()
                        .uv(102, 6).cuboid(-1.0F, 0.0F, -3.0F, 2, 8, 6),
                ModelTransform.of(2.5F, 3.0F, 2.9F, -0.855211F, -1.099557F, 0.0F));

        // Right Ear (поворот 49 по X, -119 по Y)
        head.addChild("rightEar",
                ModelPartBuilder.create()
                        .uv(102, 22).cuboid(-1.0F, 0.0F, -3.0F, 2, 8, 6),
                ModelTransform.of(-2.5F, 3.0F, 2.9F, 0.855211F, -2.076942F, 0.0F));

        return TexturedModelData.of(modelData, 128, 64);
    }

    @Override
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody, ModelPart sourceRightArm, ModelPart sourceLeftArm, ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {
        if (head != null && sourceHead != null) {
            head.pitch = sourceHead.pitch;
            head.yaw = sourceHead.yaw;
            head.roll = sourceHead.roll;
        }

        if (rightFrontLeg != null && sourceRightArm != null) {
            rightFrontLeg.pitch = sourceRightArm.pitch;
            rightFrontLeg.yaw = sourceRightArm.yaw;
            rightFrontLeg.roll = sourceRightArm.roll;
        }

        if (leftFrontLeg != null && sourceLeftArm != null) {
            leftFrontLeg.pitch = sourceLeftArm.pitch;
            leftFrontLeg.yaw = sourceLeftArm.yaw;
            leftFrontLeg.roll = sourceLeftArm.roll;
        }

        if (rightRearFoot != null && sourceRightLeg != null) {
            rightRearFoot.pitch = sourceRightLeg.pitch;
            rightRearFoot.yaw = sourceRightLeg.yaw;
            rightRearFoot.roll = sourceRightLeg.roll;
        }

        if (leftRearFoot != null && sourceLeftLeg != null) {
            leftRearFoot.pitch = sourceLeftLeg.pitch;
            leftRearFoot.yaw = sourceLeftLeg.yaw;
            leftRearFoot.roll = sourceLeftLeg.roll;
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        matrices.scale(0.65F, 0.65F, 0.65F);
        matrices.translate(0.0F, 0.5F, 0.0F);

        if (chinchillaBone != null) {
            chinchillaBone.render(matrices, vertices, light, overlay);
        }

        matrices.pop();
    }
}