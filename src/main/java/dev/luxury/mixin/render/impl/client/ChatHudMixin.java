package dev.luxury.mixin.render.impl.client;

import dev.luxury.Luxury;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.utils.render.TextRenderUtil;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text onAddMessage(Text message) {
        return processText(message);
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text onAddMessageWithSignature(Text message) {
        return processText(message);
    }

    private Text processText(Text text) {
        if (Luxury.getInstance() == null || text == null) return text;

        String original = text.getString();
        if (original == null || original.isEmpty()) return text;

        TextRenderUtil event = new TextRenderUtil(original);
        EventManager.call(event);

        if (!event.getText().equals(original)) {
            return Text.literal(event.getText());
        }

        return text;
    }
}