package dev.luxury.mixin.render.impl.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.utils.math.ProjectionUtil;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    private static MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    void render3dHook(RenderTickCounter renderTickCounter, CallbackInfo ci) {
        if (nullCheck()) {

            return;
        }

        RenderUtil3D.hookEvent3d();
    }
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        matrixStack.translate(mc.getEntityRenderDispatcher().camera.getPos().negate());

       ProjectionUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
       ProjectionUtil.setLastWorldSpaceMatrix(matrixStack.peek());

    }
}