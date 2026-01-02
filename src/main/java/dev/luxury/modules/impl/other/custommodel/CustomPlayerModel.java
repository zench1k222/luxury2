package dev.luxury.modules.impl.other.custommodel;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public abstract class CustomPlayerModel {
    
    protected final ModelPart root;
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;
    
    public CustomPlayerModel(ModelPart root, ModelPart head, ModelPart body, 
                            ModelPart rightArm, ModelPart leftArm, 
                            ModelPart rightLeg, ModelPart leftLeg) {
        this.root = root;
        this.head = head;
        this.body = body;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
        this.rightLeg = rightLeg;
        this.leftLeg = leftLeg;
    }
    
    public abstract void render(MatrixStack matrices, VertexConsumer vertices, 
                               int light, int overlay);
    
    public void copyRotations(ModelPart sourceHead, ModelPart sourceBody,
                             ModelPart sourceRightArm, ModelPart sourceLeftArm,
                             ModelPart sourceRightLeg, ModelPart sourceLeftLeg) {
        if (this.head != null && sourceHead != null) this.head.copyTransform(sourceHead);
        if (this.body != null && sourceBody != null) this.body.copyTransform(sourceBody);
        if (this.rightArm != null && sourceRightArm != null) this.rightArm.copyTransform(sourceRightArm);
        if (this.leftArm != null && sourceLeftArm != null) this.leftArm.copyTransform(sourceLeftArm);
        if (this.rightLeg != null && sourceRightLeg != null) this.rightLeg.copyTransform(sourceRightLeg);
        if (this.leftLeg != null && sourceLeftLeg != null) this.leftLeg.copyTransform(sourceLeftLeg);
    }
}

