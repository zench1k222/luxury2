package dev.luxury.utils.player;

import dev.luxury.utils.client.Network;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

import java.util.List;
import java.util.Locale;

public class ServerUtil  {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final List<KeyBinding> moveKeys = List.of(
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey,
            mc.options.jumpKey
    );

    public static boolean canMove = true;
    private static final SimpleScript script = new SimpleScript();
    private static final SimpleScript postScript = new SimpleScript();

    private static class SimpleScript {
        private Runnable[] tasks = new Runnable[10];
        private int[] delays = new int[10];
        private int currentStep = -1;
        private int timer = 0;
        private boolean finished = true;

        public SimpleScript cleanup() {
            tasks = new Runnable[10];
            delays = new int[10];
            currentStep = -1;
            timer = 0;
            finished = false;
            return this;
        }

        public SimpleScript addTickStep(int delay, Runnable task) {
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i] == null) {
                    tasks[i] = task;
                    delays[i] = delay;
                    break;
                }
            }
            return this;
        }

        public void update() {
            if (finished) return;

            if (currentStep == -1) {
                currentStep = 0;
                timer = 0;
            }

            if (currentStep < tasks.length && tasks[currentStep] != null) {
                if (timer >= delays[currentStep]) {
                    tasks[currentStep].run();
                    currentStep++;
                    timer = 0;
                } else {
                    timer++;
                }
            } else {
                finished = true;
            }
        }

        public boolean isFinished() {
            return finished;
        }
    }

    public static void tick() {
        script.update();
    }

    public static void postMotion() {
        postScript.update();
    }

    public static void addTask(Runnable task) {
        if (script.isFinished() && hasPlayerMovement()) {
            switch (Network.server) {
                case "FunTime" -> {
                    script.cleanup()
                            .addTickStep(0, () -> {
                                disableMoveKeys();
                                rotateToCamera();
                            })
                            .addTickStep(1, () -> {
                                task.run();
                                enableMoveKeys();
                            });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player != null && mc.player.isOnGround()) {
                        script.cleanup()
                                .addTickStep(0, ServerUtil::disableMoveKeys)
                                .addTickStep(2, ServerUtil::rotateToCamera)
                                .addTickStep(3, task::run)
                                .addTickStep(4, ServerUtil::enableMoveKeys);
                        return;
                    }
                }
                case "SpookyTime", "CopyTime" -> {
                    script.cleanup()
                            .addTickStep(0, () -> {
                                disableMoveKeys();
                                rotateToCamera();
                            })
                            .addTickStep(1, task::run)
                            .addTickStep(2, ServerUtil::enableMoveKeys);
                    return;
                }
            }
        }
        script.addTickStep(0, ServerUtil::rotateToCamera);
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            closeScreen(true);
        });
    }

    private static void rotateToCamera() {
        if (mc.player != null && mc.cameraEntity != null) {
            mc.player.setYaw(mc.cameraEntity.getYaw());
            mc.player.setPitch(mc.cameraEntity.getPitch());
        }
    }

    public static void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public static void enableMoveKeys() {
        closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    private static void unPressMoveKeys() {
        moveKeys.forEach(key -> key.setPressed(false));
    }

    private static void updateMoveKeys() {
        if (mc.getWindow() != null) {
            moveKeys.forEach(key -> {
                boolean pressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                        key.getDefaultKey().getCode());
                key.setPressed(pressed);
            });
        }
    }

    private static void closeScreen(boolean skip) {
        if (skip && mc.currentScreen != null) {
            mc.player.closeScreen();
        }
    }

    private static boolean hasPlayerMovement() {
        return mc.player != null && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
    }

    public static float getHealth(LivingEntity target) {
        if (mc.getCurrentServerEntry() == null) {
            return target.getHealth();
        }

        String serverAddress = mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
        boolean isLocal = mc.isConnectedToLocalServer();

        if (isLocal || serverAddress.isEmpty()) {
            return target.getHealth();
        }

        if (target instanceof MobEntity) {
            return target.getHealth();
        }

        if (serverAddress.contains("reallyworld") || serverAddress.contains("playrw") ||
                serverAddress.contains("saturn-x") || serverAddress.contains("skytime") ||
                serverAddress.contains("space-times") || serverAddress.contains("funtime")) {
            Scoreboard scoreboard = target.getWorld().getScoreboard();
            ScoreboardObjective scoreObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

            if (scoreObjective != null) {
                try {
                    int hp = scoreboard.getOrCreateScore(ScoreHolder.fromName(target.getNameForScoreboard()), scoreObjective).getScore();
                    if (hp >= 0 && hp <= target.getMaxHealth()) {
                        return (float) hp;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return target.getHealth();
    }

    public static boolean isConnected(String ip) {
        if (mc.getCurrentServerEntry() == null) return false;
        String serverAddress = mc.getCurrentServerEntry().address;
        return serverAddress != null && serverAddress.contains(ip);
    }
}