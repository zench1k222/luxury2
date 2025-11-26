package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.EventMoveInput;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {


    @Shadow
    @Final
    private GameOptions settings;

    @Unique
    private float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }


    @Inject(method = "tick",at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;playerInput:Lnet/minecraft/util/PlayerInput;",ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    public void injectInputEvent(CallbackInfo ci) {

        EventMoveInput event = new EventMoveInput(this.playerInput,
                getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward()),
                getMovementMultiplier(this.playerInput.left(), this.playerInput.right())
        );
        EventManager.call(event);

        if (event.isCancelled()) return;

        this.movementForward = event.getForward();
        this.movementSideways = event.getStrafe();
        this.playerInput = new PlayerInput(
                this.movementForward > 0,
                this.movementForward < 0,
                this.movementSideways > 0,
                this.movementSideways < 0,
                this.settings.jumpKey.isPressed(),
                this.settings.sneakKey.isPressed(),
                this.settings.sprintKey.isPressed()
        );
        ci.cancel();
    }
}
