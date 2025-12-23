package dev.luxury.modules.impl;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

@ModuleAnnotation(
        name = "ClientSounds",
        desc = "Воспроизведение звуков клиента",
        category = Category.Render
)
public class ClientSounds extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static ClientSounds instance;

    public final BooleanSetting toggleSound = new BooleanSetting("Звук вкл/выкл модуля", true);
    public final BooleanSetting killSound = new BooleanSetting("Звук убийства", true);
    public final BooleanSetting armorAlertSound = new BooleanSetting("Звук поломки брони", true);
    public final BooleanSetting clientStartSound = new BooleanSetting("Звук запуска клиента", true);

    private static SoundEvent ENABLE_SOUND;
    private static SoundEvent DISABLE_SOUND;
    private static SoundEvent KILL_SOUND;
    private static SoundEvent ARMOR_ALERT_SOUND;
    private static SoundEvent START_SOUND;

    public ClientSounds() {
        instance = this;
        registerSounds();
        addSettings(toggleSound, killSound, armorAlertSound, clientStartSound);
    }

    public static ClientSounds getInstance() {
        return instance;
    }

    private void registerSounds() {
        ENABLE_SOUND = registerSound("luxury", "enable");
        DISABLE_SOUND = registerSound("luxury", "disable");
        KILL_SOUND = registerSound("luxury", "kill");
        ARMOR_ALERT_SOUND = registerSound("luxury", "armor-alert");
        START_SOUND = registerSound("luxury", "start");
    }

    private static SoundEvent registerSound(String namespace, String path) {
        Identifier id = Identifier.of(namespace, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public void playClientStartSound() {
        if (clientStartSound.get()) {
            playSound(START_SOUND);
        }
    }

    public void playEnableSound() {
        if (toggleSound.get()) {
            playSound(ENABLE_SOUND);
        }
    }

    public void playDisableSound() {
        if (toggleSound.get()) {
            playSound(DISABLE_SOUND);
        }
    }

    public void playKillSound() {
        if (killSound.get()) {
            playSound(KILL_SOUND);
        }
    }

    public void playArmorAlertSound() {
        if (armorAlertSound.get()) {
            playSound(ARMOR_ALERT_SOUND);
        }
    }

    private void playSound(SoundEvent sound) {
        if (mc == null || sound == null) return;

        if (!mc.isOnThread()) {
            mc.execute(() -> playSound(sound));
            return;
        }

        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(
                    PositionedSoundInstance.master(
                            sound,
                            1f
                    )
            );
        }
    }

    public void stopAllSounds() {
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stopAll();
        }
    }
}