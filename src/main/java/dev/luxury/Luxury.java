package dev.luxury;

import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.api.ModuleManager;

import dev.luxury.render.feature.CustomModelFeature;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.CommandManager;
import dev.luxury.utils.managers.FriendManager;
import lombok.Getter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.EntityType;
@Getter
public class Luxury implements ModInitializer {

    @Getter
    private static Luxury instance;

    ModuleManager moduleManager = new ModuleManager();
    FontHelper fontHelper = new FontHelper();
    @Getter
    private static dev.luxury.utils.managers.SyncManager Sync;

    @Override
    public void onInitialize() {
        instance = this;
        EventManager.register(this);
        fontHelper.initialize();
        moduleManager.init();
        Sync = dev.luxury.utils.managers.SyncManager.getInstance();

        CommandManager.init(moduleManager);
        FriendManager.getInstance();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, registrationHelper, context) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer && entityType == EntityType.PLAYER) {
                registrationHelper.register(new CustomModelFeature(playerRenderer));
            }
        });
    }
}