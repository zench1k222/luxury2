package dev.luxury.mixin.render.impl.client;


import dev.luxury.events.impl.client.EventCamera;
import dev.luxury.events.impl.client.EventCameraPosition;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.NoPush;
import dev.luxury.modules.impl.killaura.rotate.Rotate;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Vec3d pos;

    @Shadow @Final
    private BlockPos.Mutable blockPos;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void moveBy(float f, float g, float h);

    @Shadow
    protected abstract float clipToSpace(float f);

    @Shadow
    private float yaw;

    @Shadow
    private float pitch;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        NoPush noPush = ModuleManager.getModule(NoPush.class);

        if (noPush != null && noPush.isEnabled() && noPush.mods.getValueByName("Блоки").get()) {
            if (focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
                float cameraPitch = inverseView ? -pitch : pitch;
                float cameraYaw = yaw - (inverseView ? 180 : 0);
                float distance = 4.0F;
                setRotation(cameraYaw, cameraPitch);
                moveBy(-distance, 0.0F, 0.0F);
                ci.cancel();
            }
        }
    }
    @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void posHook(Vec3d pos, CallbackInfo ci) {
        EventCameraPosition event = new EventCameraPosition(pos);
        EventManager.call(event);
        this.pos = pos = event.getPos();
        blockPos.set(pos.x,pos.y,pos.z);
        ci.cancel();
    }
}