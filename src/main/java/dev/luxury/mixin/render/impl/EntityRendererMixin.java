// Файл: dev/luxury/mixin/render/impl/EntityRendererMixin.java
package dev.luxury.mixin.render.impl;

import dev.luxury.Luxury;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.impl.ESP;
import dev.luxury.utils.render.TextRenderUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, Text text, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers, int light,
                                      CallbackInfo ci) {

        ESP esp = ESP.getInstance();

        if (esp != null && esp.isEnabled() && esp.shouldHideVanillaNameTag(state)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Text onRenderLabel(Text text) {
        if (Luxury.getInstance() == null) return text;

        String original = text.getString();
        TextRenderUtil event = new TextRenderUtil(original);
        EventManager.call(event);

        if (!event.getText().equals(original)) {
            return Text.of(event.getText());
        }

        return text;
    }
}