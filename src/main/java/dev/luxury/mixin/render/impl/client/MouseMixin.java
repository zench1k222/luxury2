package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton",at = @At(value ="HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci){
        EventManager.call(new EventMouseInput(button,action,mods));
    }

}
