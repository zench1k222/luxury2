package dev.luxury.modules.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.Luxury;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleAnnotation(
        name = "EntityEsp",
        desc = "Красивые квадраты на игроках",
        category = Category.Render
)
public class ESP extends Module {

    private final ModeListSetting targets = new ModeListSetting(
            "Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Меня", false),
            new BooleanSetting("Предметы", false)
    );

    private final BooleanSetting showNameTags = new BooleanSetting("Показывать неймтеги", true);
    private final BooleanSetting showArmor = new BooleanSetting("Показывать предметы", true);
    private final BooleanSetting showEffects = new BooleanSetting("Показывать эффекты", true);
    private final BooleanSetting showEnchants = new BooleanSetting("Показывать чары", true);
    private final BooleanSetting showSpheres = new BooleanSetting("Показывать шары/талисманы", true);
    private final BooleanSetting showShulkerContents = new BooleanSetting("Показывать содержимое шалкеров", true);

    private static final int BG_COLOR = new Color(30, 30, 30, 150).getRGB();
    private static final Set<String> IMPORTANT_ENCHANTS = Set.of(
            "Protection", "Защита",
            "Unbreaking", "Прочность",
            "Looting", "Добыча",
            "Fortune", "Удача",
            "Efficiency", "Эффективность",
            "Power", "Сила",
            "Feather Falling", "Невесомость",
            "Thorns", "Шипы",
            "Silk Touch", "Шёлковое касание",
            "Respiration", "Подводное дыхание",
            "Mending", "Починка",
            "Knockback", "Отдача",
            "Curse of Vanishing", "Проклятие утраты"
    );

    public ESP() {
        addSettings(targets, showNameTags, showArmor, showEffects, showEnchants, showSpheres, showShulkerContents);
    }

    @EventTarget
    public void onEvent(EventRender2D e) {
        if (!isEnabled()) return;
        if (mc.options.hudHidden) return;

        Matrix4f matrix = e.getDrawContext().getMatrices().peek().getPositionMatrix();

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        List<AbstractClientPlayerEntity> players = Luxury.getSync().getPlayers();
        List<Entity> entities = getTargetSetting("Предметы") ? Luxury.getSync().getEntities() : List.of();

        for (PlayerEntity player : players) {
            if (shouldRender(player)) {
                drawBox(e.getDeltatick(), buffer, player, matrix);
            }
        }

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity) {
                drawBox(e.getDeltatick(), buffer, entity, matrix);
            }
        }

        RenderUtil.render3D.endBuilding(buffer);
        RenderUtil.disableRender();

        // Render name tags - set translation only for name tags
        if (showNameTags.get()) {
            if (getTargetSetting("Игроков") || getTargetSetting("Друзей") || getTargetSetting("Меня")) {
                renderPlayerNameTags(e);
            }
            if (getTargetSetting("Предметы")) {
                renderItemNameTags(e);
            }
        }
    }

    private boolean getTargetSetting(String name) {
        BooleanSetting setting = targets.getValueByName(name);
        return setting != null && setting.get();
    }

    private boolean shouldRender(PlayerEntity entity) {
        if (entity == mc.player) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return false;
            return getTargetSetting("Меня");
        }
        if (getTargetSetting("Друзей") && FriendManager.getInstance().isFriend(entity.getName().getString())) {
            return true;
        }
        return getTargetSetting("Игроков");
    }

    public void drawBox(RenderTickCounter tick, BufferBuilder buffer, @NotNull Entity ent, Matrix4f matrix) {
        Vec3d[] corners = getVectors(tick, ent);

        Vector4d pos = null;
        for (Vec3d corner : corners) {
            Vec3d screen = RenderUtil.render3D.worldSpaceToScreenSpace(corner);
            if (screen.z <= 0 || screen.z >= 1) continue;

            if (pos == null) pos = new Vector4d(screen.x, screen.y, screen.x, screen.y);
            else {
                if (screen.x < pos.x) pos.x = screen.x;
                if (screen.y < pos.y) pos.y = screen.y;
                if (screen.x > pos.z) pos.z = screen.x;
                if (screen.y > pos.w) pos.w = screen.y;
            }
        }

        if (pos == null) return;

        double screenW = mc.getWindow().getScaledWidth();
        double screenH = mc.getWindow().getScaledHeight();
        if (pos.z < 0 || pos.x > screenW || pos.w < 0 || pos.y > screenH) return;

        float x1 = (float) pos.x;
        float y1 = (float) pos.y;
        float x2 = (float) pos.z;
        float y2 = (float) pos.w;

        int black = Color.BLACK.getRGB();

        drawRect(buffer, matrix, x1 - 1f, y1, x1 + 0.5f, y2 + 0.5f, black);
        drawRect(buffer, matrix, x1 - 1f, y1 - 0.5f, x2 + 0.5f, y1 + 1f, black);
        drawRect(buffer, matrix, x2 - 1f, y1, x2 + 0.5f, y2 + 0.5f, black);
        drawRect(buffer, matrix, x1 - 1f, y2 - 1f, x2 + 0.5f, y2 + 0.5f, black);

        int cTop = ColorUtil.getColorStyle(270);
        int cRight = ColorUtil.getColorStyle(90);
        int cBottom = ColorUtil.getColorStyle(180);
        int cLeft = ColorUtil.getColorStyle(0);

        drawRect(buffer, matrix, x1 - 0.5f, y1, x1 + 0.5f, y2, cTop, cLeft, cLeft, cTop);
        drawRect(buffer, matrix, x1, y2 - 0.5f, x2, y2, cLeft, cBottom, cBottom, cLeft);
        drawRect(buffer, matrix, x1 - 0.5f, y1, x2, y1 + 0.5f, cBottom, cRight, cRight, cBottom);
        drawRect(buffer, matrix, x2 - 0.5f, y1, x2, y2, cRight, cTop, cTop, cRight);
    }

    private void drawRect(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int c1) {
        buffer.vertex(matrix, x1, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y1, 0f).color(c1);
        buffer.vertex(matrix, x1, y1, 0f).color(c1);
    }

    private void drawRect(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2,
                          int c1, int c2, int c3, int c4) {
        buffer.vertex(matrix, x1, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y2, 0f).color(c2);
        buffer.vertex(matrix, x2, y1, 0f).color(c3);
        buffer.vertex(matrix, x1, y1, 0f).color(c4);
    }

    @NotNull
    private Vec3d[] getVectors(RenderTickCounter tick, @NotNull Entity ent) {
        double x = ent.prevX + (ent.getX() - ent.prevX) * tick.getTickDelta(true);
        double y = ent.prevY + (ent.getY() - ent.prevY) * tick.getTickDelta(true);
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * tick.getTickDelta(true);

        Box bb = ent.getBoundingBox();
        double dx = bb.minX - ent.getX() + x;
        double dy = bb.minY - ent.getY() + y;
        double dz = bb.minZ - ent.getZ() + z;
        double dx2 = bb.maxX - ent.getX() + x;
        double dy2 = bb.maxY - ent.getY() + y;
        double dz2 = bb.maxZ - ent.getZ() + z;

        return new Vec3d[]{
                new Vec3d(dx - 0.05, dy, dz - 0.05),
                new Vec3d(dx - 0.05, dy2 + 0.15, dz - 0.05),
                new Vec3d(dx2 + 0.05, dy, dz - 0.05),
                new Vec3d(dx2 + 0.05, dy2 + 0.15, dz - 0.05),
                new Vec3d(dx - 0.05, dy, dz2 + 0.05),
                new Vec3d(dx - 0.05, dy2 + 0.15, dz2 + 0.05),
                new Vec3d(dx2 + 0.05, dy, dz2 + 0.05),
                new Vec3d(dx2 + 0.05, dy2 + 0.15, dz2 + 0.05)
        };
    }

    private void renderPlayerNameTags(EventRender2D e) {
        final int screenW = mc.getWindow().getScaledWidth();
        final int screenH = mc.getWindow().getScaledHeight();
        final float tickDelta = e.getDeltatick().getTickDelta(true);
        MatrixStack matrixStack = e.getMatrixStack();

        for (PlayerEntity player : Luxury.getSync().getPlayers()) {
            if (player == null || (player instanceof ClientPlayerEntity && mc.options.getPerspective() == Perspective.FIRST_PERSON))
                continue;
            if (!shouldRender(player)) continue;

            Vec3d pos = new Vec3d(
                    player.prevX + (player.getX() - player.prevX) * tickDelta,
                    player.prevY + (player.getY() - player.prevY) * tickDelta + player.getEyeHeight(player.getPose()) * 0.85 + 0.2,
                    player.prevZ + (player.getZ() - player.prevZ) * tickDelta
            );

            Vec3d screen = RenderUtil.render3D.worldSpaceToScreenSpace(pos);
            if (screen.z < 0 || screen.z >= 1 || screen.x < 0 || screen.x > screenW || screen.y < 0 || screen.y > screenH)
                continue;

            String friendPrefix = FriendManager.getInstance().isFriend(player.getName().getString()) 
                    ? Formatting.GRAY + "[" + Formatting.GREEN + "F" + Formatting.GRAY + "] " : "";
            float health = player.getHealth() + player.getAbsorptionAmount();
            String hpText = Formatting.GRAY + " [" + (health < 300 ? Formatting.RED.toString() + (int) health : Formatting.RED + "Unknown") + Formatting.GRAY + "]" + Formatting.RESET;
            String name = player.getGameProfile().getName();
            Text prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix() : Text.literal("");

            Text itemText = null;
            if (showSpheres.get()) {
                ItemStack offHand = player.getOffHandStack();
                if (!offHand.isEmpty() && (offHand.getItem() == Items.TOTEM_OF_UNDYING || offHand.getItem() instanceof PlayerHeadItem)) {
                    Text customName = offHand.getCustomName();
                    if (customName != null) {
                        itemText = customName;
                    }
                }
            }

            float friendWidth = mc.textRenderer.getWidth(Text.literal(friendPrefix)) * 0.7f;
            float prefixWidth = mc.textRenderer.getWidth(prefix) * 0.7f;
            FontDraw font = FontHelper.monsterrat[13];
            if (font == null) continue;
            float nameHpWidth = font.getWidth(name + hpText);
            float itemWidth = itemText != null ? mc.textRenderer.getWidth(itemText) * 0.7f + 3f : 0f;
            float totalWidth = friendWidth + prefixWidth + nameHpWidth + itemWidth;

            float x = (float) screen.x - (totalWidth + 8f) / 2f;
            float y = (float) screen.y - 14f - 1f;

            RenderUtil.drawRoundedRect(matrixStack, x, y, totalWidth + 8f, 12f, new Vector4f(1.5f, 1.5f, 1.5f, 1.5f), BG_COLOR);

            MatrixStack matrices = e.getDrawContext().getMatrices();
            matrices.push();
            matrices.translate(x + 4f, y + 3.2f, 0);
            matrices.scale(0.7f, 0.7f, 1.0f);

            int dx = 0;
            if (!friendPrefix.isEmpty()) {
                mc.textRenderer.draw(Text.literal(friendPrefix), dx, 0, -1, false, matrices.peek().getPositionMatrix(), 
                        mc.getBufferBuilders().getEntityVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
                dx += friendWidth / 0.7f;
            }

            mc.textRenderer.draw(prefix, dx, 0, -1, false, matrices.peek().getPositionMatrix(), 
                    mc.getBufferBuilders().getEntityVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            dx += prefixWidth / 0.7f;
            matrices.pop();

            font.drawFontLeft(matrixStack, name + hpText, x + 4f + friendWidth + prefixWidth, y + 1.8f, -1);

            if (itemText != null) {
                matrices.push();
                matrices.translate(x + 4f + friendWidth + prefixWidth + nameHpWidth + 3f, y + 3.5f, 0);
                matrices.scale(0.7f, 0.7f, 1.0f);
                mc.textRenderer.draw(itemText, 0, 0, -1, false, matrices.peek().getPositionMatrix(), 
                        mc.getBufferBuilders().getEntityVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
                matrices.pop();
            }

            if (showNameTags.get() && showEffects.get()) renderEffects(e, player, tickDelta);
            if (showNameTags.get() && showArmor.get()) renderPlayerItems(e, x + 5f, y, player);
        }
    }

    private void renderEffects(EventRender2D e, PlayerEntity player, float tickDelta) {
        Vec3d footPos = RenderUtil.render3D.worldSpaceToScreenSpace(new Vec3d(
                player.prevX + (player.getX() - player.prevX) * tickDelta,
                player.prevY + (player.getY() - player.prevY) * tickDelta,
                player.prevZ + (player.getZ() - player.prevZ) * tickDelta
        ));
        if (footPos.z < 0) return;

        int offsetY = 5;
        FontDraw font = FontHelper.monsterrat[14];
        if (font == null) return;

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String effectName = I18n.translate(effect.getEffectType().value().getName().getString());
            int lvl = effect.getAmplifier() + 1;
            int sec = effect.getDuration() / 20;
            String text = Formatting.WHITE + effectName + (lvl > 1 ? " " + lvl : "") + Formatting.WHITE + " | " + sec / 60 + ":" + String.format("%02d", sec % 60);
            font.drawCentered(e.getDrawContext().getMatrices(), text, (float) footPos.x, (float) footPos.y + offsetY, Color.white.getRGB());
            offsetY += 9;
        }
    }

    private void renderPlayerItems(EventRender2D e, float x, float y, PlayerEntity player) {
        List<ItemStack> stacks = new ArrayList<>(6);
        stacks.add(player.getMainHandStack());
        player.getArmorItems().forEach(stacks::add);
        stacks.add(player.getOffHandStack());
        stacks.removeIf(i -> i.isEmpty() || i.getItem() instanceof AirBlockItem);

        float offset = 0;
        DrawContext context = e.getDrawContext();
        MatrixStack matrices = e.getMatrixStack();

        for (ItemStack stack : stacks) {
            matrices.push();
            matrices.translate(x + offset - 3f, y - 18f, 0);
            matrices.scale(0.8f, 0.8f, 1.0f);
            context.drawItem(stack, 0, 0);
            matrices.pop();

            if (showEnchants.get() && !stack.getEnchantments().isEmpty()) {
                List<Object2IntMap.Entry<RegistryEntry<Enchantment>>> enchantments = new ArrayList<>(stack.getEnchantments().getEnchantmentEntries());
                enchantments.removeIf(entry -> {
                    Text name = Enchantment.getName(entry.getKey(), entry.getIntValue());
                    String full = name.getString();
                    return IMPORTANT_ENCHANTS.stream().noneMatch(full::contains);
                });

                if (!enchantments.isEmpty()) {
                    int totalHeight = enchantments.size() * 8;
                    int startY = (int) (y - 18f - totalHeight);
                    FontDraw font = FontHelper.monsterrat[14];
                    if (font != null) {
                        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments) {
                            RegistryEntry<Enchantment> regEntry = entry.getKey();
                            int level = entry.getIntValue();
                            Text enchantText = Enchantment.getName(regEntry, level);
                            String display = getShortName(enchantText, level);

                            matrices.push();
                            matrices.translate(x + offset, startY, 0);
                            matrices.scale(0.7f, 0.7f, 1.0f);
                            font.drawFontLeft(e.getDrawContext().getMatrices(), display, 0, 0, Color.white.getRGB());
                            matrices.pop();
                            startY += 8;
                        }
                    }
                }
            }

            offset += 15f;
        }
    }

    private String getShortName(Text description, int level) {
        String full = description.getString();
        String[] words = full.split(" ");
        String shortName;
        if (words.length == 1) {
            shortName = words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        } else {
            shortName = "";
            for (String w : words) {
                if (!w.isEmpty()) shortName += w.charAt(0);
            }
            shortName = shortName.toUpperCase();
        }
        return shortName + " " + level;
    }

    private void renderItemNameTags(EventRender2D e) {
        final float tickDelta = e.getDeltatick().getTickDelta(true);
        final int screenW = mc.getWindow().getScaledWidth();
        final int screenH = mc.getWindow().getScaledHeight();

        for (Entity entity : Luxury.getSync().getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            ItemStack stack = itemEntity.getStack();
            Item item = stack.getItem();

            if (item instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock && showNameTags.get() && showShulkerContents.get()) {
                Vec3d vec = RenderUtil.render3D.worldSpaceToScreenSpace(new Vec3d(
                        entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
                        entity.prevY + (entity.getY() - entity.prevY) * tickDelta + 0.6,
                        entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
                ));
                if (vec.z < 0 || vec.z >= 1 || vec.x < 0 || vec.x > screenW || vec.y < 0 || vec.y > screenH) continue;
                renderShulkerToolTip(e.getDrawContext(), (int) vec.x, (int) vec.y, stack);
            }

            Vec3d vec = RenderUtil.render3D.worldSpaceToScreenSpace(new Vec3d(
                    entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
                    entity.prevY + (entity.getY() - entity.prevY) * tickDelta + 0.6,
                    entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
            ));
            if (vec.z < 0 || vec.z >= 1 || vec.x < 0 || vec.x > screenW || vec.y < 0 || vec.y > screenH) continue;

            String name = itemEntity.getName().getString();
            int count = stack.getCount();
            if (count > 1) name += " [x" + Formatting.RED + count + Formatting.WHITE + "]";

            FontDraw font = FontHelper.monsterrat[15];
            if (font == null) continue;
            float width = font.getWidth(name);
            float height = font.getHeight();
            float x = (float) vec.x - width / 2f - 3f;
            float y = (float) vec.y;

            RenderUtil.drawRoundedRect(e.getMatrixStack(), x, y, width + 6f, height + 2f, new Vector4f(1.5f, 1.5f, 1.5f, 1.5f), BG_COLOR);
            font.drawCentered(e.getDrawContext().getMatrices(), name, (float) vec.x, y + 0.3f, Color.WHITE.getRGB());
        }
    }

    private boolean renderShulkerToolTip(DrawContext context, int offsetX, int offsetY, ItemStack stack) {
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null || container.copyFirstStack().isEmpty()) return false;
        drawShulkerContents(context, container.stream().toList(), offsetX, offsetY);
        return true;
    }

    private void drawShulkerContents(DrawContext context, List<ItemStack> itemStacks, int offsetX, int offsetY) {
        final int columns = 9;
        final int rows = 3;
        final float itemSize = 16f;
        final float spacing = 0.1f;
        final int paddingX = 8;
        final int paddingY = 7;

        offsetX += paddingX;
        offsetY -= 82 - paddingY;

        int bgWidth = (int) (columns * itemSize + (columns - 1) * spacing + paddingX * 2);
        int bgHeight = (int) (rows * itemSize + (rows - 1) * spacing + paddingY * 2);

        Identifier containerTexture = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
        RenderUtil.drawImage(context.getMatrices(), containerTexture, offsetX - paddingX, offsetY - paddingY, bgWidth, bgHeight, -1);

        MatrixStack matrices = context.getMatrices();
        for (int index = 0; index < itemStacks.size(); index++) {
            int row = index / columns;
            int col = index % columns;
            float x = offsetX + col * (itemSize + spacing);
            float y = offsetY + row * (itemSize + spacing);

            matrices.push();
            matrices.translate(x, y, 0);
            matrices.scale(0.85f, 0.85f, 1.0f);
            context.drawItem(itemStacks.get(index), 0, 0);
            matrices.pop();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}

