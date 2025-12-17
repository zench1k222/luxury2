package dev.luxury;

import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.ClientSounds;
import dev.luxury.modules.impl.killaura.rotate.Aim;
import dev.luxury.modules.impl.killaura.rotate.Rotates;
import dev.luxury.render.feature.CustomModelFeature;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.CommandManager;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.network.ServerHandler;
import lombok.Getter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.client.MinecraftClient;

@Getter
public class Luxury implements ModInitializer {

    @Getter
    private static Luxury instance;
    private Rotates rotationManager;
    ModuleManager moduleManager = new ModuleManager();
    FontHelper fontHelper = new FontHelper();
    Aim aim;
    ServerHandler serverHandler;
    @Getter
    private static dev.luxury.utils.managers.SyncManager Sync;
    FriendManager friendManager = new FriendManager();

    @Override
    public void onInitialize() {
        instance = this;
        EventManager.register(this);
        fontHelper.initialize();
        moduleManager.init();

        Sync = dev.luxury.utils.managers.SyncManager.getInstance();
        rotationManager = new Rotates();
        serverHandler = new ServerHandler();
        CommandManager.init(moduleManager);

        friendManager.getInstance();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, registrationHelper, context) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer && entityType == EntityType.PLAYER) {
                registrationHelper.register(new CustomModelFeature(playerRenderer));
            }
        });
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            dev.luxury.common.way.WayRepository.getInstance().render(drawContext.getMatrices());
        });
    }
}