package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventMouse;
import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.luxury.modules.api.Module.mc;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At(value = "HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        EventManager.call(new EventMouseInput(button, action, mods));

        if (button != GLFW.GLFW_KEY_UNKNOWN && window == mc.getWindow().getHandle()) {
            EventManager.call(new EventMouse(button, action));
        }

    }
}
