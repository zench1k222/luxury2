package dev.luxury.mixin.render.impl.client;

import dev.luxury.events.impl.client.SwingDurationEvent;
import dev.luxury.events.impl.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    @Nullable
    public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);

    @Unique
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != client.player) {
            return;
        }

        SwingDurationEvent event = new SwingDurationEvent();
        EventManager.call(event);

        if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(client.player))
                animation *= (6 - (1 + StatusEffectUtil.getHasteAmplifier(client.player)));
            else
                animation *= (hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6);
            cir.setReturnValue((int) animation);
        }
    }
}