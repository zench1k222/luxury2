package dev.luxury.modules.api;

import dev.luxury.Luxury;

import dev.luxury.events.impl.eventapi.EventManager;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;


@Getter
@Setter
public class Module {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    private String name;
    private  Category category;
    private boolean enabled;
    private int key = -1;
    private boolean pressed;


    public Module() {

    }

    public Module(String name, Category category, int key) {
        this.name = name;
        this.category = category;
        this.key = key;

    }


    public void enable() {
        if (!enabled) {
            enabled = true;
            Luxury.getInstance().getEventBus().register(this);
            EventManager.register(this);
            onEnable();
        }
    }

    public void disable() {
        if (enabled) {
            enabled = false;
            Luxury.getInstance().getEventBus().unregister(this);
            EventManager.unregister(this);
            onDisable();
        }
    }


    public void toggle() {
        if (enabled) disable();
        else enable();
    }

    public void onEnable() {}
    public void onDisable() {}



}
