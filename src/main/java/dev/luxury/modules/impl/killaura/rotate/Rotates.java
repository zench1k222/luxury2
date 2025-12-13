package dev.luxury.modules.impl.killaura.rotate;

import dev.luxury.Luxury;
import dev.luxury.events.impl.client.*;
import dev.luxury.events.impl.eventapi.EventManager;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.eventapi.types.Priority;
import dev.luxury.modules.impl.KillAura;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import net.minecraft.util.math.MathHelper;

import dev.luxury.modules.api.Module;

@Getter
public class Rotates {
    MinecraftClient mc = MinecraftClient.getInstance();
    private Rotate currentRotate = new Rotate(0, 0);
    private Rotate previousRotate = new Rotate(0, 0);
    private final HandlerRequest<TargetRotate> requestHandler = new HandlerRequest<>();
    private final Aim aim = new Aim();
    private TargetRotate previousTargetRotate = new TargetRotate(currentRotate, () -> currentRotate, aim.getInstantSetup());

    private boolean setRotation = true;

    public Rotates() {
        EventManager.register(this);
    }

    @EventTarget
    public void addLocalPlayer(EventSpawnEntity eventSpawnLocalPlayer) {
        if (eventSpawnLocalPlayer.getEntity() instanceof ClientPlayerEntity player) {
            currentRotate = new Rotate(player.getYaw(), player.getPitch());
            previousRotate = new Rotate(player.getYaw(), player.getPitch());
            previousTargetRotate = new TargetRotate(currentRotate, () -> currentRotate, aim.getInstantSetup());
            setRotation = true;
        }
    }

    @EventTarget(Priority.LOW)
    public void update(EventTick event) {
        EventManager.call(new EventRotate());

        TargetRotate targetRotation = requestHandler.getActiveRequestValue();
        if (targetRotation != null) {
            Rotate newRot = targetRotation.rotation().get();
            previousRotate = currentRotate;
            currentRotate = newRot;
            setRotation = false;
            this.previousTargetRotate = targetRotation;
        } else {
            if (setRotation) {
                previousRotate = currentRotate;
                currentRotate = aim.rotate(aim.getInstantSetup(), new Rotate(mc.player.getYaw(), mc.player.getPitch()));
            } else {
                Rotate back = new Rotate(mc.player.getYaw(), mc.player.getPitch());

                if (currentRotate.rotationDeltaTo(back).isInRange(5)) {
                    previousRotate = currentRotate;
                    currentRotate = aim.rotate(aim.getInstantSetup(), back);
                    setRotation = true;
                } else {
                    Rotate newRot = aim.rotate(previousTargetRotate.rotationConfigBack(), back);
                    previousRotate = currentRotate;
                    currentRotate = newRot;
                }
            }
        }

        if (!setRotation) {
            float delta = currentRotate.getYaw() - mc.player.lastYaw;
            {
                Rotate validing = new Rotate(currentRotate.getYaw(), currentRotate.getPitch());
                if (delta > 320)
                    validing = new Rotate(mc.player.lastYaw + 300, currentRotate.getPitch()).normalize(new Rotate(mc.player.lastYaw, mc.player.lastPitch));
                if (delta < -320)
                    validing = new Rotate(mc.player.lastYaw - 300, currentRotate.getPitch()).normalize(new Rotate(mc.player.lastYaw, mc.player.lastPitch));

                currentRotate = validing;
            }
        }

        currentRotate = new Rotate(currentRotate.getYaw(), MathHelper.clamp(currentRotate.getPitch(), -90, 90));

        requestHandler.tick();
    }

    public void setRotation(TargetRotate targetRotation, int priority, Module module) {
        requestHandler.request(new HandlerRequest.Request<>(2, priority, module, targetRotation));
    }

    @EventTarget
    public void direction(EventDirection direction) {
        // Проверяем, используется ли SlothAI
        boolean isSlothAI = false;
        try {
            KillAura killAura = (KillAura) Luxury.getInstance().getModuleManager()
                    .getModules().stream()
                    .filter(m -> m instanceof KillAura && m.isEnabled())
                    .findFirst()
                    .orElse(null);

            if (killAura != null) {
                isSlothAI = killAura.getRotationMode().is("SlothAI");
            }
        } catch (Exception e) {
            // ignore
        }

        // Для SlothAI устанавливаем только pitch (наклон головы), yaw (поворот) остается от игрока
        if (isSlothAI) {
            direction.setYaw(mc.player.getYaw()); // Тело движется свободно
            direction.setPitch(currentRotate.getPitch()); // Только наклон головы от ротации
        } else {
            direction.setYaw(currentRotate.getYaw());
            direction.setPitch(currentRotate.getPitch());
        }
    }

    @EventTarget
    public void packet(PacketEvent eventPacket) {
        switch (eventPacket.getPacket()) {
            case PlayerRotationS2CPacket player -> {
                currentRotate = new Rotate(player.xRot(), player.yRot());
                previousTargetRotate = new TargetRotate(currentRotate, () -> currentRotate, aim.getInstantSetup());
                setRotation = true;
            }

            case PlayerPositionLookS2CPacket player -> {
                currentRotate = new Rotate(player.change().yaw(), player.change().pitch());
                previousTargetRotate = new TargetRotate(currentRotate, () -> currentRotate, aim.getInstantSetup());
                setRotation = true;
            }
            case PlayerInteractItemC2SPacket packetItem -> {
                packetItem.yaw = currentRotate.getYaw();
                packetItem.pitch = currentRotate.getPitch();
            }
            default -> {
            }
        }
    }
}