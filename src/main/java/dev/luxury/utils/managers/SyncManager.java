package dev.luxury.utils.managers;

import com.google.common.collect.ImmutableList;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.events.impl.eventapi.EventTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.Collections;
import java.util.List;

public class SyncManager {
    private static SyncManager instance;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private volatile List<Entity> cachedEntities = Collections.emptyList();
    private volatile List<AbstractClientPlayerEntity> cachedPlayers = Collections.emptyList();

    private SyncManager() {
        EventManager.register(this);
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public List<Entity> getEntities() {
        return cachedEntities;
    }

    public List<AbstractClientPlayerEntity> getPlayers() {
        return cachedPlayers;
    }

    @EventTarget
    public void onEvent(EventTick event) {
        if (mc.world == null || mc.player == null) {
            cachedEntities = Collections.emptyList();
            cachedPlayers = Collections.emptyList();
            return;
        }
        
        cachedEntities = ImmutableList.copyOf(mc.world.getEntities());
        cachedPlayers = ImmutableList.copyOf(mc.world.getPlayers());
    }
}

