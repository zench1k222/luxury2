package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.events.impl.render.TextFactoryEvent;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = {TextVisitFactory.class})
    public class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z" }, index = 0)
    private static String adjustText(String text) {
        TextFactoryEvent event = new TextFactoryEvent(text);
        EventManager.call(event);
        return event.getText();
    }
}
