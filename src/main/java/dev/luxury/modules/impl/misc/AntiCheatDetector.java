package dev.luxury.modules.impl.misc;

import dev.luxury.events.impl.client.PacketEvent;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@ModuleAnnotation(
        name = "ACDetector",
        category = Category.Misc,
        desc = "Advanced anti-cheat detector (silent & packet based)"
)
public class AntiCheatDetector extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private int suspicion = 0;
    private int totalDetections = 0;

    private long lastSetback = 0;
    private int setbackCount = 0;
    private long lastKeepAlive = 0;
    private int keepAliveFreq = 0;
    private final Queue<Long> recentSetbacks = new LinkedList<>();
    private final Queue<Long> recentVelocities = new LinkedList<>();
    private Vec3d lastPos = Vec3d.ZERO;
    private boolean hasMovedSinceSetback = false;
    private int ticksSinceLastFlag = 0;
    private double lastVelLength = 0;
    private int consecutiveSmallVels = 0;
    private int teleportCount = 0;
    private long lastTeleport = 0;
    private final Queue<Long> gameModeChanges = new LinkedList<>();
    private final Queue<Long> worldBorderChanges = new LinkedList<>();
    private boolean lastOnGround = false;
    private int groundSpoofDetects = 0;
    private double lastFallDistance = 0;
    private int packetQueueSize = 0;
    private long lastTransaction = 0;
    private int transactionSpam = 0;
    private final Map<String, Long> attributeChanges = new HashMap<>();
    private final Queue<Long> blockPlacements = new LinkedList<>();
    private final Queue<Long> blockBreaks = new LinkedList<>();
    private final Queue<Float> rotationChanges = new LinkedList<>();
    private Vec3d lastLookVec = Vec3d.ZERO;
    private int constantRotationTicks = 0;
    private final Queue<Long> hitTimings = new LinkedList<>();
    private long lastSwing = 0;
    private int perfectCpsCount = 0;
    private double lastReachDistance = 0;
    private int liquidInteractions = 0;
    private boolean wasInLiquid = false;
    private final Queue<Double> clickDelays = new LinkedList<>();
    private int sameYawTicks = 0;
    private float lastYaw = 0;
    private int invalidPacketOrder = 0;
    private boolean sentMovement = false;

    private enum AntiCheat {
        GRIM, MATRIX, VULCAN, NCP, VERUS, POLAR, INTAVE, SPARTAN, UNKNOWN
    }

    private final EnumMap<AntiCheat, Integer> acScore = new EnumMap<>(AntiCheat.class);

    public AntiCheatDetector() {
        for (AntiCheat ac : AntiCheat.values()) {
            acScore.put(ac, 0);
        }
    }

    private void warn(int weight, String type, String reason, AntiCheat ac) {
        suspicion += weight;
        totalDetections++;
        acScore.put(ac, acScore.get(ac) + weight);

        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[AC] ").formatted(Formatting.DARK_RED, Formatting.BOLD).append(Text.literal(type + " » ").formatted(Formatting.RED)).append(Text.literal(reason).formatted(Formatting.WHITE)).append(Text.literal(" [" + ac.name() + "]").formatted(Formatting.GOLD)).append(Text.literal(" #" + totalDetections).formatted(Formatting.GRAY)), false);
        }

        suspicion = Math.min(suspicion, 100);
        ticksSinceLastFlag = 0;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.player == null) return;

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket p) {
            long now = System.currentTimeMillis();

            if (hasMovedSinceSetback) {
                recentSetbacks.add(now);
                if (recentSetbacks.size() > 5) recentSetbacks.poll();

                setbackCount++;
                lastSetback = now;
                hasMovedSinceSetback = false;

                if (setbackCount > 2 && now - lastSetback < 5000) {
                    warn(5, "Setback", "Multiple position corrections", AntiCheat.GRIM);
                }

                if (recentSetbacks.size() >= 3) {
                    long span = now - recentSetbacks.peek();
                    if (span < 2000) {
                        warn(7, "Setback", "Rapid setback pattern (GRIM)", AntiCheat.GRIM);
                    }
                }
            }
        }

        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket p) {
            if (p.getEntityId() == mc.player.getId()) {
                long now = System.currentTimeMillis();
                recentVelocities.add(now);
                if (recentVelocities.size() > 10) recentVelocities.poll();

                Vec3d vel = new Vec3d(p.getVelocityX() / 8000.0, p.getVelocityY() / 8000.0, p.getVelocityZ() / 8000.0);

                double len = vel.length();

                if (len < 0.1 && len > 0.001) {
                    consecutiveSmallVels++;
                    if (consecutiveSmallVels >= 3) {
                        warn(6, "Velocity", "Micro-velocity spam (VULCAN)", AntiCheat.VULCAN);
                        consecutiveSmallVels = 0;
                    }
                } else {
                    consecutiveSmallVels = 0;
                }

                if (vel.y > 0 && vel.y < 0.085 && vel.horizontalLength() > 0.2) {
                    warn(5, "Velocity", "Clamped Y knockback (MATRIX)", AntiCheat.MATRIX);
                }

                if (recentVelocities.size() >= 5) {
                    long span = now - recentVelocities.peek();
                    if (span < 500) {
                        warn(4, "Velocity", "Velocity packet spam", AntiCheat.VULCAN);
                    }
                }

                lastVelLength = len;
            }
        }

        if (e.getPacket() instanceof ExplosionS2CPacket p) {
            Optional<Vec3d> kb = p.playerKnockback();
            if (kb.isPresent() && kb.get().lengthSquared() < 0.005) {
                warn(4, "Explosion", "Nullified explosion KB (POLAR)", AntiCheat.POLAR);
            }
        }

        if (e.getPacket() instanceof KeepAliveS2CPacket) {
            long now = System.currentTimeMillis();
            if (lastKeepAlive > 0) {
                long delta = now - lastKeepAlive;
                if (delta < 500) {
                    keepAliveFreq++;
                    if (keepAliveFreq >= 4) {
                        warn(3, "Network", "KeepAlive spam pattern (NCP)", AntiCheat.NCP);
                        keepAliveFreq = 0;
                    }
                } else {
                    keepAliveFreq = 0;
                }
            }
            lastKeepAlive = now;
        }

        if (e.getPacket() instanceof OverlayMessageS2CPacket p) {
            Text text = p.text();
            if (text != null) {
                String msg = text.getString().toLowerCase();
                if (msg.contains("illegal") || msg.contains("violation") || msg.contains("check") || msg.contains("alert")) {
                    warn(8, "Overlay", "AC alert message (VULCAN)", AntiCheat.VULCAN);
                }
            }
        }

        if (e.getPacket() instanceof TitleS2CPacket p) {
            warn(5, "Title", "Title packet flag (MATRIX)", AntiCheat.MATRIX);
        }

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            long now = System.currentTimeMillis();
            teleportCount++;

            if (now - lastTeleport < 100) {
                warn(6, "Teleport", "Rapid teleport spam (INTAVE)", AntiCheat.INTAVE);
            }
            lastTeleport = now;

            if (now - lastTeleport > 5000) {
                teleportCount = 0;
            }
        }

        if (e.getPacket() instanceof GameStateChangeS2CPacket p) {
            long now = System.currentTimeMillis();
            gameModeChanges.add(now);
            if (gameModeChanges.size() > 3) gameModeChanges.poll();

            if (gameModeChanges.size() >= 3) {
                long span = now - gameModeChanges.peek();
                if (span < 3000) {
                    warn(5, "GameMode", "Rapid gamemode changes (GRIM)", AntiCheat.GRIM);
                }
            }
        }

        if (e.getPacket() instanceof WorldBorderInitializeS2CPacket || e.getPacket() instanceof WorldBorderSizeChangedS2CPacket) {
            long now = System.currentTimeMillis();
            worldBorderChanges.add(now);
            if (worldBorderChanges.size() > 5) worldBorderChanges.poll();

            if (worldBorderChanges.size() >= 3) {
                long span = now - worldBorderChanges.peek();
                if (span < 2000) {
                    warn(6, "WorldBorder", "Border manipulation (POLAR)", AntiCheat.POLAR);
                }
            }
        }

        if (e.getPacket() instanceof CommonPingS2CPacket) {
            long now = System.currentTimeMillis();
            if (now - lastTransaction < 50) {
                transactionSpam++;
                if (transactionSpam >= 5) {
                    warn(7, "Transaction", "Transaction packet spam (GRIM)", AntiCheat.GRIM);
                    transactionSpam = 0;
                }
            } else {
                transactionSpam = 0;
            }
            lastTransaction = now;
        }

        if (e.getPacket() instanceof EntityAttributesS2CPacket p) {
            if (p.getEntityId() == mc.player.getId()) {
                long now = System.currentTimeMillis();
                String key = "speed_change";

                if (attributeChanges.containsKey(key)) {
                    long last = attributeChanges.get(key);
                    if (now - last < 500) {
                        warn(5, "Attributes", "Rapid attribute changes (MATRIX)", AntiCheat.MATRIX);
                    }
                }
                attributeChanges.put(key, now);
            }
        }

        if (e.getPacket() instanceof DamageTiltS2CPacket) {
            if (mc.player.getHealth() == mc.player.getMaxHealth()) {
                warn(6, "Damage", "Damage tilt without HP loss (VERUS)", AntiCheat.VERUS);
            }
        }

        if (e.getPacket() instanceof BundleS2CPacket p) {
            try {
                var packets = p.getPackets();
                if (packets != null) {
                    int size = 0;
                    for (var packet : packets) {
                        size++;
                    }
                    if (size > 10) {
                        warn(4, "Bundle", "Large packet bundle flush (NCP)", AntiCheat.NCP);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (e.getPacket() instanceof BlockUpdateS2CPacket) {
            long now = System.currentTimeMillis();
            blockPlacements.add(now);
            if (blockPlacements.size() > 8) blockPlacements.poll();

            if (blockPlacements.size() >= 5) {
                long span = now - blockPlacements.peek();
                if (span < 400) {
                    warn(7, "Scaffold", "Rapid block placement (MATRIX)", AntiCheat.MATRIX);
                }
            }
        }

        if (e.getPacket() instanceof BlockBreakingProgressS2CPacket p) {
            long now = System.currentTimeMillis();
            blockBreaks.add(now);
            if (blockBreaks.size() > 5) blockBreaks.poll();

            if (blockBreaks.size() >= 4) {
                long span = now - blockBreaks.peek();
                if (span < 300) {
                    warn(6, "FastBreak", "Rapid block breaking (GRIM)", AntiCheat.GRIM);
                }
            }
        }

        if (e.getPacket() instanceof GameMessageS2CPacket p) {
            String msg = p.content().getString();
            if (msg.contains("spam") || msg.contains("slow down") || msg.contains("flood")) {
                warn(5, "Chat", "Chat spam warning (VULCAN)", AntiCheat.VULCAN);
            }
        }

        if (e.getPacket() instanceof ParticleS2CPacket p) {
            if (mc.player.getPos().distanceTo(new Vec3d(p.getX(), p.getY(), p.getZ())) < 2.0) {
                warn(4, "Particle", "Suspicious particle near player (SPARTAN)", AntiCheat.SPARTAN);
            }
        }

        if (e.getPacket() instanceof PlayerAbilitiesS2CPacket p) {
            warn(5, "Abilities", "Abilities packet received (INTAVE)", AntiCheat.INTAVE);
        }

        if (e.getPacket() instanceof HealthUpdateS2CPacket p) {
            if (p.getHealth() < mc.player.getHealth() - 0.5f) {
                if (!mc.player.isOnGround() && mc.player.fallDistance > 3) {
                    warn(6, "Health", "Delayed damage packet (POLAR)", AntiCheat.POLAR);
                }
            }
        }

        if (e.getPacket() instanceof EntityStatusEffectS2CPacket p) {
            if (p.getEntityId() == mc.player.getId()) {
                warn(3, "Effect", "Effect modification detected (VERUS)", AntiCheat.VERUS);
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null) return;

        ticksSinceLastFlag++;

        Vec3d currentPos = mc.player.getPos();
        double moveDist = currentPos.distanceTo(lastPos);
        if (moveDist > 0.01) {
            hasMovedSinceSetback = true;
        }
        lastPos = currentPos;

        if (System.currentTimeMillis() - lastSetback > 10000) {
            setbackCount = Math.max(0, setbackCount - 1);
        }

        if (ticksSinceLastFlag > 200) {
            recentSetbacks.clear();
            recentVelocities.clear();
        }

        if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
            double posDelta = Math.abs(mc.player.getX() - mc.player.prevX) +
                    Math.abs(mc.player.getZ() - mc.player.prevZ);

            if (posDelta < 0.0001 && !mc.player.isSneaking()) {
                warn(6, "Desync", "Movement input without position change (GRIM)", AntiCheat.GRIM);
            }
        }

        float yawDiff = Math.abs(mc.player.getYaw() - mc.player.prevYaw);
        float pitchDiff = Math.abs(mc.player.getPitch() - mc.player.prevPitch);

        if (yawDiff > 45 && ticksSinceLastFlag > 10) {
            warn(5, "Rotation", "Rubber-band yaw correction (MATRIX)", AntiCheat.MATRIX);
        }

        if (yawDiff > 15 && yawDiff % 10.0f < 0.1f) {
            warn(4, "Rotation", "Discrete rotation snap (POLAR)", AntiCheat.POLAR);
        }

        if (!mc.player.isOnGround() && mc.player.getVelocity().horizontalLength() < 0.1
                && (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0)) {
            warn(5, "Movement", "Forced air slowdown (VERUS)", AntiCheat.VERUS);
        }

        if (mc.player.handSwinging) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown < 0.5f && ticksSinceLastFlag > 5) {
                warn(3, "Combat", "Cooldown violation (NCP)", AntiCheat.NCP);
            }
        }

        Vec3d currentLook = mc.player.getRotationVec(1.0f);
        if (mc.player.handSwinging) {
            double lookDiff = currentLook.distanceTo(lastLookVec);

            if (lookDiff < 0.001 && lastLookVec.length() > 0) {
                constantRotationTicks++;
                if (constantRotationTicks > 5) {
                    warn(8, "KillAura", "Locked rotation pattern (GRIM)", AntiCheat.GRIM);
                    constantRotationTicks = 0;
                }
            } else {
                constantRotationTicks = 0;
            }
        }
        lastLookVec = currentLook;

        float rotYawDiff = Math.abs(mc.player.getYaw() - mc.player.prevYaw);
        rotationChanges.add(rotYawDiff);
        if (rotationChanges.size() > 10) rotationChanges.poll();

        if (rotationChanges.size() >= 8) {
            double avg = rotationChanges.stream().mapToDouble(d -> d).average().orElse(0);
            if (avg > 15 && rotYawDiff > 40) {
                warn(7, "Aimbot", "Rotation snapback pattern (MATRIX)", AntiCheat.MATRIX);
            }
        }

        if (mc.player.handSwinging) {
            long now = System.currentTimeMillis();
            if (lastSwing > 0) {
                double delay = now - lastSwing;
                clickDelays.add(delay);
                if (clickDelays.size() > 20) clickDelays.poll();

                if (clickDelays.size() >= 15) {
                    double avgDelay = clickDelays.stream().mapToDouble(d -> d).average().orElse(0);
                    double variance = clickDelays.stream().mapToDouble(d -> Math.pow(d - avgDelay, 2)).average().orElse(0);
                    double stdDev = Math.sqrt(variance);

                    if (stdDev < 8 && avgDelay < 100 && avgDelay > 30) {
                        warn(9, "AutoClick", "Consistent click pattern (VULCAN)", AntiCheat.VULCAN);
                        clickDelays.clear();
                    }

                    if (Math.abs(avgDelay - 50.0) < 5) {
                        perfectCpsCount++;
                        if (perfectCpsCount >= 3) {
                            warn(10, "AutoClick", "Perfect CPS timing (VULCAN)", AntiCheat.VULCAN);
                            perfectCpsCount = 0;
                        }
                    }
                }
            }
            lastSwing = now;
        }

        if (mc.crosshairTarget != null && mc.player.handSwinging) {
            double reachDist = mc.crosshairTarget.getPos().distanceTo(mc.player.getEyePos());
            if (reachDist > 3.1 && reachDist < 6.0) {
                warn(8, "Reach", "Extended reach distance", AntiCheat.GRIM);
            }
            lastReachDistance = reachDist;
        }

        boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
        if (inLiquid) {
            double hSpeed = mc.player.getVelocity().horizontalLength();
            if (hSpeed > 0.15 && !mc.player.isSprinting()) {
                liquidInteractions++;
                if (liquidInteractions > 10) {
                    warn(6, "Liquid", "Fast liquid movement (INTAVE)", AntiCheat.INTAVE);
                    liquidInteractions = 0;
                }
            }
        } else {
            liquidInteractions = 0;
        }
        wasInLiquid = inLiquid;

        if (Math.abs(mc.player.getYaw() - lastYaw) < 0.1f) {
            sameYawTicks++;
            if (sameYawTicks > 30 && mc.player.getVelocity().y > 0.2) {
                warn(7, "Tower", "Locked yaw while ascending (SPARTAN)", AntiCheat.SPARTAN);
                sameYawTicks = 0;
            }
        } else {
            sameYawTicks = 0;
        }
        lastYaw = mc.player.getYaw();

        if (mc.player.isTouchingWater() && !mc.player.isSwimming() && mc.player.isOnGround()) {
            double yVel = mc.player.getVelocity().y;
            if (Math.abs(yVel) < 0.01) {
                warn(8, "Jesus", "Walking on water surface (POLAR)", AntiCheat.POLAR);
            }
        }

        if (mc.player.isOnGround() && lastOnGround) {
            double yDiff = mc.player.getY() - mc.player.prevY;
            if (yDiff > 0.6 && yDiff < 1.2 && !mc.player.jumping) {
                warn(7, "Step", "High step height (VERUS)", AntiCheat.VERUS);
            }
        }

        if (mc.world != null && !mc.player.isSpectator()) {
            var box = mc.player.getBoundingBox();
            boolean inBlock = !mc.world.isSpaceEmpty(mc.player, box.contract(0.01));
            if (inBlock && mc.player.getVelocity().horizontalLength() > 0.1) {
                warn(9, "Phase", "Moving inside blocks (MATRIX)", AntiCheat.MATRIX);
            }
        }

        if (mc.player.isUsingItem() && !mc.player.isSneaking()) {
            double hSpeed = mc.player.getVelocity().horizontalLength();
            if (hSpeed > 0.2) {
                warn(6, "NoSlow", "Fast movement while using item (GRIM)", AntiCheat.GRIM);
            }
        }

        if (mc.player.horizontalCollision && !mc.player.isOnGround()) {
            double yVel = mc.player.getVelocity().y;
            if (yVel > 0.15 && !mc.player.isClimbing()) {
                warn(8, "Spider", "Wall climbing detected (INTAVE)", AntiCheat.INTAVE);
            }
        }
    }

    public AntiCheat getDetectedAntiCheat() {
        return acScore.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(AntiCheat.UNKNOWN);
    }

    public String getACReport() {
        AntiCheat detected = getDetectedAntiCheat();
        int confidence = acScore.get(detected) * 100 / Math.max(1, suspicion);
        return detected.name() + " (" + confidence + "% confidence, " + totalDetections + " flags)";
    }
}