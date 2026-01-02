package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeSetting;
import net.minecraft.client.option.KeyBinding;

import java.util.Random;

@ModuleAnnotation(
        name = "AntiAFK",
        desc = "Предотвращает AFK кик",
        category = Category.Misc
)
public class AntiAFK extends Module {

    private final Random random = new Random();
    private int tickCounter = 0;
    private boolean jumping = false;
    private int jumpTicks = 0;
    private boolean rotating = false;
    private float targetYaw = 0;
    private float targetPitch = 0;

    private final ModeSetting actionDelay = new ModeSetting("Задержка", "Средняя",
            new String[]{"Медленно", "Средняя", "Быстро"});

    private final BooleanSetting rotate = new BooleanSetting("Поворачиваться", true);
    private final BooleanSetting jump = new BooleanSetting("Прыгать", true);
    private final BooleanSetting sendMessage = new BooleanSetting("Писать в чат", true);

    public AntiAFK() {
        addSettings(actionDelay, rotate, jump, sendMessage);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        tickCounter++;

        int delay = getDelayTicks();

        if (tickCounter % delay == 0) {
            performAction();
        }

        // Прыжок
        if (jumping) {
            jumpTicks++;
            if (jumpTicks >= 5) {
                KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), false);
                jumping = false;
                jumpTicks = 0;
            }
        }

        if (rotating) {
            rotateCamera();
        }
    }

    private void performAction() {
        int action = random.nextInt(100);

        if (sendMessage.get() && action < 25) {
            sendMessage();
        }

        if (jump.get() && action < 40) {
            jump();
        }

        if (rotate.get()) {
            startRotation();
        }
    }

    private void sendMessage() {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage("/qqallle");
        }
    }

    private void jump() {
        if (mc.player != null && mc.player.isOnGround() && !jumping) {
            KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), true);
            jumping = true;
        }
    }

    private void startRotation() {
        targetYaw = random.nextFloat() * 360.0f;
        targetPitch = random.nextFloat() * 60.0f - 30.0f;
        rotating = true;
    }

    private void rotateCamera() {
        if (mc.player != null) {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();

            float yawDiff = targetYaw - currentYaw;
            float pitchDiff = targetPitch - currentPitch;

            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;

            float step = 10.0f;

            float newYaw = currentYaw;
            float newPitch = currentPitch;

            if (Math.abs(yawDiff) > step) {
                newYaw += (yawDiff > 0 ? step : -step);
            } else {
                newYaw = targetYaw;
            }

            if (Math.abs(pitchDiff) > step) {
                newPitch += (pitchDiff > 0 ? step : -step);
            } else {
                newPitch = targetPitch;
            }

            mc.player.setYaw(newYaw);
            mc.player.setPitch(newPitch);

            if (Math.abs(newYaw - targetYaw) < 1 && Math.abs(newPitch - targetPitch) < 1) {
                rotating = false;
            }
        }
    }

    private int getDelayTicks() {
        switch (actionDelay.getValue()) {
            case "Медленно": return 6400;
            case "Быстро": return 2400;
            default: return 1200;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tickCounter = 0;
        jumping = false;
        jumpTicks = 0;
        rotating = false;
        targetYaw = 0;
        targetPitch = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (mc.options != null) {
            KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), false);
        }
    }
}