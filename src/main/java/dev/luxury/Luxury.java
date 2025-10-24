package dev.luxury;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import dev.luxury.events.impl.client.EventKeyInput;
import dev.luxury.modules.api.ModuleManager;

import lombok.Getter;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
@Getter
public class Luxury implements ModInitializer {

    @Getter
    private static Luxury instance;
    @Getter
    public static EventBus eventBus = new EventBus();
    ModuleManager moduleManager = new ModuleManager();


    @Override
    public void onInitialize() {
        instance = this;
        moduleManager.init();
        eventBus.register(this);
    }


    @Subscribe
    public void onKey(EventKeyInput e) {
        if (e.getAction() != GLFW.GLFW_PRESS) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null || client.world == null) return;

        moduleManager.onKey(e.getKey());
    }
}
