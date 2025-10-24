package dev.luxury.mixin.render.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.render.RenderUtil3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
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
}