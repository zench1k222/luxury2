package dev.luxury.mixin.render.impl.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.utils.math.ProjectionUtil;
import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    private static MinecraftClient mc = MinecraftClient.getInstance();



    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        matrixStack.translate(mc.getEntityRenderDispatcher().camera.getPos().negate());

        ProjectionUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
        ProjectionUtil.setLastWorldSpaceMatrix(matrixStack.peek());
        RenderUtil3D.hookEvent3d();
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    private void render3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
        MatrixStack matrixStack = new MatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        RenderSystem.setShaderFog(Fog.DUMMY);
        //ебаная хуйня но она нада
        RenderUtil.render3D.setTranslation(matrixStack);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.getModelViewMatrix();
    }
}