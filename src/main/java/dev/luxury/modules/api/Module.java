package dev.luxury.modules.api;

import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.api.settings.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Module {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    private String name;
    private  Category category;
    private boolean enabled;
    private boolean pressed;
    private List<Setting> settings = new ArrayList<>();
    private int key;
    private boolean mouse;
    public Module() {

    }

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }


    public void enable() {
        if (!enabled) {
            enabled = true;
            EventManager.register(this);
            onEnable();
        }
    }

    public void disable() {
        if (enabled) {
            enabled = false;
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

    public void addSettings(Setting... settings) {
        for (Setting setting : settings) {
            this.settings.add(setting);
        }
    }

    public Setting getSetting(String name) {
        for (Setting setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public <T extends Setting> T getSetting(Class<T> clazz, String name) {
        Setting setting = getSetting(name);
        if (setting != null && clazz.isInstance(setting)) {
            return clazz.cast(setting);
        }
        return null;
    }


    @Override
    public String toString() {
        if (key == -1) return "None";
        if (mouse) return "M" + (key + 1);

        String str = "";
        try {
            for (Field field : GLFW.class.getDeclaredFields()) {
                if (field.getName().startsWith("GLFW_KEY_")) {
                    int a = (int) field.get(null);
                    if (a == key) {
                        String nb = field.getName().substring("GLFW_KEY_".length());
                        str = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        return str.toUpperCase();
    }

    public boolean isEmpty() {
        return key < 0;
    }
}
