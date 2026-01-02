package dev.luxury.modules.impl.render;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.math.MathUtil;
import dev.luxury.utils.math.ProjectionUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil3D;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ModuleAnnotation(
        name = "Projectiles",
        desc = "Отображает траекторию и время полета снарядов",
        category = Category.Render
)
public class Projectiles extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private final ModeListSetting projectiles = new ModeListSetting("Снаряды",
            new BooleanSetting("Эндер Пёрл", true),
            new BooleanSetting("Стрела", true),
            new BooleanSetting("Трезубец", true));

    private final List<Point> points = new ArrayList<>();

    public Projectiles() {
        addSettings(projectiles);
    }

    @EventTarget
    public void onEvent(EventRender2D event) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;
        
        DrawContext context = event.getDrawContext();
        MatrixStack matrices = event.getMatrixStack();
        
        for (Point point : points) {
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(point.pos);
            int ticks = point.ticks;
            
            if (vec3d.z < 0 || vec3d.z > 1) continue;
            if (!ProjectionUtil.canSee(new Box(point.pos.subtract(0.1, 0.1, 0.1), point.pos.add(0.1, 0.1, 0.1)))) continue;
            
            FontDraw font = FontHelper.monsterrat[13];
            if (font == null) continue;
            
            double time = ticks * 50 / 1000.0;
            String text = String.format("%.1f", time) + " сек";
            float textWidth = font.getWidth(text);
            float posX = (float) (vec3d.x + textWidth / 2 - 6);
            float posY = (float) (vec3d.y + 4);
            float padding = 3;
            float iconSize = 8;


            font.drawFontLeft(matrices, text, posX - textWidth + 8 + padding * 2, posY + 0.5F, -1);

            matrices.push();
            matrices.translate(posX - textWidth - padding + 2, posY - padding, 0);
            matrices.scale(0.5F, 0.5F, 0.5F);
            context.drawItem(point.stack, 0, 0);
            matrices.pop();
        }
    }

    @EventTarget
    public void onEvent(EventRender3D event) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;
        
        points.clear();
        MatrixStack matrix = event.getMatrices();
        
        drawPredictionInHand(matrix, mc.player.getHandItems());
        
        getProjectiles().forEach(entity -> {
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getPos();
            Vec3d prevPos;

            for (int i = 0; i < 300; i++) {
                prevPos = pos;
                pos = pos.add(motion);
                motion = calculateMotion(entity, prevPos, motion);

                HitResult result = raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);

                if (!result.getType().equals(HitResult.Type.MISS)) {
                    pos = result.getPos();
                }

                int color = ColorUtil.multAlpha(ColorUtil.fade(i), MathHelper.clamp(i / 25.0f, 0, 1));
                drawLine(prevPos, pos, color, 2);

                if (!result.getType().equals(HitResult.Type.MISS) || pos.y < -128) {
                    addPoint(entity, pos, i);
                    break;
                }
            }
        });
    }

    public void drawPredictionInHand(MatrixStack matrix, Iterable<ItemStack> stacks) {
        Item activeItem = mc.player.getActiveItem().getItem();
        
        for (ItemStack stack : stacks) {
            List<HitResult> results = switch (stack.getItem()) {
                case ExperienceBottleItem item -> checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8);
                case SplashPotionItem item -> checkTrajectory(new PotionEntity(mc.world, mc.player, stack), 0.55);
                case TridentItem item when item.equals(activeItem) && mc.player.getItemUseTime() >= 10 -> 
                        checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5);
                case SnowballItem item -> checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5);
                case EggItem item -> checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5);
                case EnderPearlItem item -> checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5);
                case BowItem item when item.equals(activeItem) && mc.player.isUsingItem() -> 
                        checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), 
                                3 * MathHelper.clamp((mc.player.getItemUseTime() + mc.getRenderTickCounter().getTickDelta(false)) / 20F, 0F, 1F));
                case CrossbowItem item when CrossbowItem.isCharged(stack) -> {
                    ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                    List<HitResult> list = new ArrayList<>();
                    if (component != null && !component.getProjectiles().isEmpty()) {
                        float velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;
                        list.add(checkTrajectory(getRotationVector(mc.player.getYaw(), mc.player.getPitch()), 
                                new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        if (component.getProjectiles().size() > 2) {
                            float pitchAbs = mc.player.getPitch() / 90;
                            float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                            float yaw = MathHelper.lerp(Math.abs(delta), 10, 90);
                            float pitch = MathHelper.lerp(delta, 0, 10);
                            list.add(checkTrajectory(getRotationVector(mc.player.getYaw() - yaw, mc.player.getPitch() - pitch), 
                                    new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                            list.add(checkTrajectory(getRotationVector(mc.player.getYaw() + yaw * 2, mc.player.getPitch()), 
                                    new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        }
                    }
                    yield list;
                }
                default -> null;
            };

            if (results != null) {
                results = results.stream().filter(Objects::nonNull).toList();
                if (!results.isEmpty()) {
                    renderProjectileResults(matrix, results);
                }
            }
            return;
        }
    }

    public void renderProjectileResults(MatrixStack matrix, List<HitResult> results) {
        for (HitResult result : results) {
            Direction direction = getDirection(result);
            int color = result.getType().equals(HitResult.Type.ENTITY) ? Color.RED.getRGB() : ColorUtil.getClientColor();
            double width = 0.3;

            Quaternionf quaternionf = switch (direction) {
                case WEST, EAST -> RotationAxis.POSITIVE_Z.rotationDegrees(90);
                case SOUTH, NORTH -> RotationAxis.POSITIVE_X.rotationDegrees(90);
                default -> new Quaternionf();
            };

            matrix.push();
            matrix.translate(result.getPos());
            matrix.multiply(quaternionf);
            MatrixStack.Entry entry = matrix.peek().copy();

            for (int i = 0, size = 90; i <= size; i++) {
                Vec3d p1 = getCosSin(i, size, width);
                Vec3d p2 = getCosSin(i + 1, size, width);
                drawLine(entry, p1, p2, color, color, 1, false);
            }

            drawLine(entry, new Vec3d(0, 0, -width), new Vec3d(0, 0, width), color, color, 1, false);
            drawLine(entry, new Vec3d(-width, 0, 0), new Vec3d(width, 0, 0), color, color, 1, false);
            matrix.pop();
        }
    }

    public List<Entity> getProjectiles() {
        List<Entity> projectiles = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if ((entity instanceof PersistentProjectileEntity || entity instanceof ThrownItemEntity || entity instanceof ItemEntity) 
                    && !isVisible(entity)) {
                projectiles.add(entity);
            }
        }
        return projectiles;
    }

    public List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity) {
        return new ArrayList<>(Collections.singleton(checkTrajectory(getRotationVector(mc.player.getYaw(), mc.player.getPitch()), entity, velocity)));
    }

    public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
        float sqrt = MathHelper.sqrt((float) lookVec.lengthSquared());
        Vec3d motion = switch (entity) {
            case ArrowEntity arrow when arrow.getItemStack().getItem().equals(Items.CROSSBOW) -> Vec3d.ZERO;
            default -> mc.player.getPos().subtract(mc.player.prevX, mc.player.prevY, mc.player.prevZ);
        };

        return traceTrajectory(mc.player.getEyePos().add(MathUtil.interpolate(mc.player).subtract(mc.player.getPos())), 
                lookVec.multiply(velocity / sqrt).add(motion), entity);
    }

    public HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
        Vec3d prevPos;
        for (int i = 0; i < 300; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = calculateMotion(entity, prevPos, motion);

            HitResult result = raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
            if (!result.getType().equals(HitResult.Type.MISS)) {
                return result;
            }

            Vec3d finalPos = pos, finalPrevPos = prevPos;
            for (Entity ent : mc.world.getEntities()) {
                if (ent instanceof LivingEntity living && living != entity.getOwner() && living.isAlive()) {
                    if (living.getBoundingBox().expand(0.3).intersects(finalPrevPos, finalPos)) {
                        return new HitResult(pos) {
                            @Override
                            public Type getType() {
                                return Type.ENTITY;
                            }
                        };
                    }
                }
            }

            if (pos.y < -128) break;
        }
        return null;
    }

    public HitResult calcTrajectory(ProjectileEntity e) {
        return traceTrajectory(e.getPos(), e.getVelocity(), e);
    }

    public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
        
        double multiply = switch (entity) {
            case TridentEntity trident -> 0.99;
            case PersistentProjectileEntity persistent when isInWater -> 0.6;
            default -> isInWater ? 0.8 : 0.99;
        };

        return motion.multiply(multiply).add(0, -entity.getFinalGravity(), 0);
    }

    private void addPoint(Entity entity, Vec3d pos, int ticks) {
        switch (entity) {
            case ItemEntity item -> points.add(new Point(item.getStack(), pos, ticks));
            case ThrownItemEntity thrown -> points.add(new Point(thrown.getStack(), pos, ticks));
            case PersistentProjectileEntity persistent -> points.add(new Point(persistent.getItemStack(), pos, ticks));
            default -> {}
        }
    }

    private Direction getDirection(HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getSide();
        }
        Vec3d dir = result.getPos().subtract(mc.player.getEyePos()).normalize();
        return Direction.getFacing(dir.x, dir.y, dir.z);
    }

    private boolean isVisible(Entity entity) {
        boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
        boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || 
                isBoxInWater(entity.getBoundingBox().expand(2)));
        return posChange || itemEntityCheck;
    }

    private boolean isBoxInWater(Box box) {
        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.WATER) {
                return true;
            }
        }
        return false;
    }

    boolean validEntity(Entity entity) {
        BooleanSetting enderPearl = projectiles.getValueByName("Эндер Пёрл");
        BooleanSetting arrow = projectiles.getValueByName("Стрела");
        BooleanSetting trident = projectiles.getValueByName("Трезубец");
        
        return (entity instanceof EnderPearlEntity && enderPearl != null && enderPearl.get())
                || (entity instanceof ArrowEntity && arrow != null && arrow.get())
                || (entity instanceof TridentEntity && trident != null && trident.get());
    }

    private Vec3d getRotationVector(float yaw, float pitch) {
        float f = -yaw * ((float) Math.PI / 180);
        float g = pitch * ((float) Math.PI / 180);
        float h = MathHelper.cos(g);
        return new Vec3d(MathHelper.sin(f) * h, -MathHelper.sin(g), MathHelper.cos(f) * h);
    }

    private Vec3d getCosSin(int i, int size, double width) {
        double angle = (i / (double) size) * Math.PI * 2;
        return new Vec3d(Math.cos(angle) * width, Math.sin(angle) * width, 0);
    }

    private void drawLine(Vec3d start, Vec3d end, int color, float width) {
        RenderUtil3D.lineQueue.add(new RenderUtil3D.LineAction(start, end, new Color(color)));
    }

    private void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int color1, int color2, float width, boolean depth) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        float r1 = ColorUtil.getRed(color1) / 255f;
        float g1 = ColorUtil.getGreen(color1) / 255f;
        float b1 = ColorUtil.getBlue(color1) / 255f;
        float a1 = ColorUtil.getAlpha(color1) / 255f;
        
        float r2 = ColorUtil.getRed(color2) / 255f;
        float g2 = ColorUtil.getGreen(color2) / 255f;
        float b2 = ColorUtil.getBlue(color2) / 255f;
        float a2 = ColorUtil.getAlpha(color2) / 255f;
        
        buffer.vertex(entry.getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z).color(r1, g1, b1, a1);
        buffer.vertex(entry.getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z).color(r2, g2, b2, a2);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private HitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, Entity entity) {
        RaycastContext context = new RaycastContext(start, end, shapeType, RaycastContext.FluidHandling.NONE, entity);
        return mc.world.raycast(context);
    }

    private record Point(ItemStack stack, Vec3d pos, int ticks) {}
}
