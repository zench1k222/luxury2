package dev.luxury.mixin.render.impl.client;

import dev.luxury.Luxury;

import dev.luxury.events.impl.client.EventKeyInput;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At(value = "HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        EventManager.call(new EventKeyInput(key, action,modifiers));
    }
}
