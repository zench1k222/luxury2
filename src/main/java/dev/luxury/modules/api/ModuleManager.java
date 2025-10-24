package dev.luxury.modules.api;

import dev.luxury.Luxury;

import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.modules.impl.*;
import dev.luxury.ui.Clickgui;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ModuleManager {
    private static final List<Module> modules = new CopyOnWriteArrayList<>();


    public void init() {
        EventManager.register(this);
        registerAll(
                new Test(),
                new HUD(),
                new DiscordRPC(),
                new Blink(),
                new KillAura(),
                new TargetEsp(),
                new Speed(),
                new HighJump(),
                new Spider(),
                new NoClip(),
                new Clickgui(),
                new AntiBot()

        );
        DiscordRPC.getInstance().enable();
        Luxury.getInstance().getEventBus().register(this);
    }

    private void registerAll(Module... mods) {
        for (Module module : mods) {
            initModuleFromAnnotation(module);
        }

        Arrays.sort(mods, Comparator.comparing(Module::getName));
        modules.addAll(List.of(mods));
    }


    private void initModuleFromAnnotation(Module module) {
        if (module.getClass().isAnnotationPresent(ModuleAnnotation.class)) {
            ModuleAnnotation annotation = module.getClass().getAnnotation(ModuleAnnotation.class);

            module.setName(annotation.name());
            module.setCategory(annotation.category());
            module.setKey(annotation.key());
        }
    }

    public List<Module> getSorted() {
        return modules.stream()
                .sorted(Comparator.comparing(Module::getName))
                .toList();
    }
    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static boolean isEnabled(Class<? extends Module> clazz) {
        Module m = getModule(clazz);
        return m != null && m.isEnabled();
    }

    public void onKey(int key) {
        for (Module module : modules) {
            if (module.getKey() == key) {
                module.toggle();
            }
        }
    }
}