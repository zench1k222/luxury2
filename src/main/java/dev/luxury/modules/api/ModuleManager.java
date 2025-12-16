package dev.luxury.modules.api;

import dev.luxury.events.impl.client.EventKeyInput;
import dev.luxury.events.impl.client.EventMouseInput;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.ElytraHelper;
import dev.luxury.modules.impl.NoDelay;
import dev.luxury.modules.impl.*;
import dev.luxury.modules.impl.hud.api.HUD;
import dev.luxury.modules.impl.taksa.DogPet;
import dev.luxury.ui.Clickgui;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

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
                new HUD(),
                new DiscordRPC(),
                new Blink(),
                new dev.luxury.modules.impl.elytraaura.ElytraAura(),
                new KillAura(),
                new TargetEsp(),
                new Speed(),
                new HighJump(),
                new Spider(),
                new NoClip(),
                new Clickgui(),
                new AntiBot(),
                new ESP(),
                new SwingAnimations(),
                new CustomModels(),
                new DogPet(),
                new Projectiles(),
                new NoDelay(),
                new AutoSprint(),
                new ClickPearl(),
                new AutoTotem(),
                new ClientSounds(),
                new ArmorAlert(),
                new ElytraHelper(),
                new StaffKillAnyDesk(),
                new Arrows(),
                new FireFly(),
                new JumpCircle(),
                new HitBubles(),
                new PacketCriticals(),
                new NoWeb(),
                new Jesus(),
                new AutoDuel(),
                new TpLoot(),
                new AutoPotion(),
                new NoPush(),
                new AutoAccept(),
                new SeeInvisible()

        );
        DiscordRPC.getInstance().enable();
        ClientSounds.getInstance().enable();
    }

    public static List<Module> getModules() {
        return modules;
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

    @EventTarget
    public void onKey(EventKeyInput e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (e.getAction() == 1)
            for (Module module : modules)
                if (module.getKey() == e.getKey() && !module.isMouse())
                    module.toggle();
    }

    @EventTarget
    private void onMouse(EventMouseInput e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (e.getAction() == 1)
            for (Module module : modules)
                if (module.isMouse() && module.getKey() == e.getButton()) {
                    module.toggle();
                }
    }
}