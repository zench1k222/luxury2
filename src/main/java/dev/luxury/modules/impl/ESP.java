package dev.luxury.modules.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.BooleanSetting;
import dev.luxury.modules.api.settings.ModeListSetting;
import dev.luxury.modules.api.settings.SliderSetting;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.managers.FriendManager;
import dev.luxury.utils.player.PlayerIntersectionUtil;
import dev.luxury.utils.player.ServerUtil;
import dev.luxury.utils.render.ColorUtil;
import dev.luxury.utils.render.RenderUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    private final ModeListSetting itemMode = new ModeListSetting(
            "Режим предметов",
            new BooleanSetting("Текст", true),
            new BooleanSetting("Иконка", false)
    );

    private final SliderSetting itemBackgroundAlpha = new SliderSetting("Прозрачность фона", 0.7f, 0.1f, 1.0f, 0.1f);
    private final SliderSetting itemScale = new SliderSetting("Размер иконки", 0.8f, 0.5f, 1.5f, 0.1f);

    private final BooleanSetting showBox = new BooleanSetting("Показывать ESP Боксы", true);
    private final BooleanSetting corners = new BooleanSetting("Углы", false);
    private final SliderSetting cornerLength = new SliderSetting("Высота", 0.3f, 0.1f, 0.4f, 0.1f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.9f, 0.5f, 2.0f, 0.1f);
    private final BooleanSetting showNameTags = new BooleanSetting("Неймтеги", true);
    private final BooleanSetting hideVanillaTags = new BooleanSetting("Скрыть ванильные", true);
    private final BooleanSetting showArmor = new BooleanSetting("Броня и предметы", true);
    private final BooleanSetting effects = new BooleanSetting("Эффекты", true);



    private final Map<String, String> donateSymbols = new HashMap<String, String>() {{
        put("ꔀ", "§7&lPLAYER");
        put("ꔄ", "§9&lHERO");
        put("ꔈ", "§e&lTITAN");
        put("ꔒ", "§a&lAVENGER");
        put("ꔖ", "§b&lOVERLORD");
        put("ꔠ", "§6&lMAGISTER");
        put("ꔤ", "§c&lIMPERATOR");
        put("ꔨ", "§d&lDRAGON");
        put("ꔲ", "§5&lBULL");
        put("ꕒ", "§f&lRABBIT");
        put("ꔶ", "§6&lTIGER");
        put("ꕄ", "§4&lDRACULA");
        put("ꕖ", "§8&lBUNNY");
        put("ꕀ", "§2&lHYDRA");
        put("ꕈ", "§a&lCOBRA");
        put("ꕁ", "§6&lGOD");
        put("ꔁ", "§5&lMEDIA");
        put("ꔅ", "§cY§fT");
        put("ꕠ", "§e&lD.HELPER");
        put("ꔉ", "§e&lHELPER");
        put("ꔓ", "§1&lML.MODER");
        put("ꔗ", "§1&lMODER");
        put("ꔡ", "§5&lMODER+");
        put("ꔥ", "§1&lST.MODER");
        put("ꔩ", "§1&lGL.MODER");
        put("ꔳ", "§b&lML.ADMIN");
        put("ꔷ", "§c&lADMIN");
    }};

    private ArrayList<Entity> toRender = new ArrayList<>();
    private static ESP instance;

    public ESP() {
        addSettings(targets, itemMode, effects, itemBackgroundAlpha, itemScale ,showBox, corners, cornerLength, thickness, showNameTags, hideVanillaTags, showArmor);
        instance = this;
    }

    public static ESP getInstance() {
        return instance;
    }

    private float getPlayerScale(Entity entity) {
        if (mc.player == null) return 1.0f;

        double distance = mc.player.squaredDistanceTo(entity);
        distance = Math.sqrt(distance);

        float minDist = 2.0f;
        float maxDist = 30.0f;

        float maxScale = 1.25f;
        float minScale = 0.6f;

        float t = (float) ((distance - minDist) / (maxDist - minDist));
        t = Math.max(0f, Math.min(1f, t));

        return maxScale + (minScale - maxScale) * t;
    }

    private float getItemScale(ItemEntity itemEntity) {
        if (mc.player == null) return 1.0f;

        double distance = mc.player.squaredDistanceTo(itemEntity);
        distance = Math.sqrt(distance);

        float minDist = 2.0f;
        float maxDist = 50.0f;

        float maxScale = 1.0f;
        float minScale = 0.8f;

        if (distance < minDist) return maxScale;
        if (distance > maxDist) return minScale;

        float t = (float) ((distance - minDist) / (maxDist - minDist));
        return maxScale + (minScale - maxScale) * t;
    }

    private String getDonateRank(String prefix) {
        for (Map.Entry<String, String> entry : donateSymbols.entrySet()) {
            if (prefix.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean shouldHideVanillaNameTag(EntityRenderState state) {
        if (!isEnabled() || !hideVanillaTags.get()) {
            return false;
        }

        if (state instanceof PlayerEntityRenderState playerState) {
            return shouldHidePlayerNameTag(playerState);
        } else if (state instanceof ItemEntityRenderState itemState) {
            return shouldHideItemNameTag(itemState);
        }

        return false;
    }

    private boolean shouldHidePlayerNameTag(PlayerEntityRenderState playerState) {
        if (!showNameTags.get()) return false;

        Entity entity = getEntityFromState(playerState);
        if (entity == null) return false;

        if (entity == mc.player) {
            return getTargetSetting("Меня");
        }

        if (entity instanceof PlayerEntity player) {
            if (getTargetSetting("Друзей") && FriendManager.getInstance().isFriend(player.getName().getString())) {
                return true;
            }
            return getTargetSetting("Игроков");
        }

        return false;
    }

    private boolean shouldHideItemNameTag(ItemEntityRenderState itemState) {
        if (!showNameTags.get()) return false;
        return getTargetSetting("Предметы") && getTargetSetting("Текст");
    }

    private Entity getEntityFromState(EntityRenderState state) {
        try {
            for (java.lang.reflect.Field field : state.getClass().getDeclaredFields()) {
                if (Entity.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(state);
                    if (value instanceof Entity) {
                        return (Entity) value;
                    }
                }
            }

            String[] possibleFieldNames = {"entity", "owner", "target", "parent"};
            for (String fieldName : possibleFieldNames) {
                try {
                    java.lang.reflect.Field field = state.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(state);
                    if (value instanceof Entity) {
                        return (Entity) value;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (Math.abs(entity.getX() - state.x) < 0.5 &&
                        Math.abs(entity.getY() - state.y) < 0.5 &&
                        Math.abs(entity.getZ() - state.z) < 0.5) {
                    return entity;
                }
            }
        }

        return null;
    }

    private ArrayList<ItemEntity> itemsToRender = new ArrayList<>();

    @EventTarget
    public void onGameTick(EventTick event) {
        if (!isEnabled()) return;

        toRender.clear();
        itemsToRender.clear();

        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ClientPlayerEntity && mc.options.getPerspective() == Perspective.FIRST_PERSON)
                continue;
            if (entity.isRemoved())
                continue;

            if (shouldRenderEntity(entity)) {
                toRender.add(entity);
            }
            if (entity instanceof ItemEntity itemEntity && getTargetSetting("Предметы")) {
                itemsToRender.add(itemEntity);
            }
        }
    }

    private void renderItems(EventRender2D e) {
        if (itemsToRender.isEmpty() || !getTargetSetting("Предметы")) return;

        for (ItemEntity itemEntity : itemsToRender) {
            if (itemEntity == null || itemEntity.isRemoved()) continue;

            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty() || stack.getItem() == Items.AIR) continue;

            Vec3d interp = itemEntity.getLerpedPos(mc.getRenderTickCounter().getTickDelta(true));
            Box box = itemEntity.getBoundingBox().offset(interp.subtract(itemEntity.getPos()));

            Vec3d[] corners = new Vec3d[]{
                    new Vec3d(box.minX, box.minY, box.minZ),
                    new Vec3d(box.minX, box.minY, box.maxZ),
                    new Vec3d(box.maxX, box.minY, box.minZ),
                    new Vec3d(box.maxX, box.minY, box.maxZ),
                    new Vec3d(box.minX, box.maxY, box.minZ),
                    new Vec3d(box.minX, box.maxY, box.maxZ),
                    new Vec3d(box.maxX, box.maxY, box.minZ),
                    new Vec3d(box.maxX, box.maxY, box.maxZ)
            };

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            boolean anyVisible = false;

            for (Vec3d corner : corners) {
                Vec3d projected = RenderUtil.render3D.worldSpaceToScreenSpace(corner);
                if (projected.z <= 0 || projected.z >= 1) continue;

                anyVisible = true;

                minX = (float) Math.min(minX, projected.x);
                minY = (float) Math.min(minY, projected.y);
                maxX = (float) Math.max(maxX, projected.x);
                maxY = (float) Math.max(maxY, projected.y);
            }

            if (!anyVisible) continue;

            if (maxX < 0 || maxY < 0 || minX > mc.getWindow().getScaledWidth() || minY > mc.getWindow().getScaledHeight())
                continue;

            if (showBox.get()) {
                renderItemBox(e.getMatrixStack(), minX, minY, maxX, maxY, itemEntity);
            }

            renderItemInfo(e, minX, minY, maxX, maxY, itemEntity, stack);
        }
    }

    private void renderItemInfo(EventRender2D e,
                                       float minX, float minY,
                                       float maxX, float maxY,
                                       ItemEntity itemEntity,
                                       ItemStack stack) {

        boolean showText = getTargetSetting("Текст");
        boolean showIcon = getTargetSetting("Иконка");

        if (!showText && !showIcon) return;

        FontDraw font = FontHelper.sfprobold[15];
        if (font == null) return;

        float distanceScale = getItemScale(itemEntity);
        float iconSize = 16f * itemScale.getFloatValue() * distanceScale;

        String text = getItemDisplayText(stack);
        String cleanText = text.replaceAll("§[0-9a-fk-or]", "");

        float textWidth = showText ? font.getWidth(cleanText) * distanceScale : 0;
        float textHeight = showText ? font.getHeight() * distanceScale : 0;
        float iconWidth = showIcon ? iconSize : 0;

        float paddingX = 4f * distanceScale;
        float paddingY = 2f * distanceScale;
        float spacing = 2f * distanceScale;

        float totalWidth = textWidth + iconWidth;
        if (showText && showIcon) {
            totalWidth += spacing;
        }

        float bgWidth = totalWidth + paddingX * 2f;
        float bgHeight = Math.max(textHeight, iconSize) + paddingY * 2f;

        float centerX = (minX + maxX) / 2f;
        float posX = centerX - bgWidth / 2f;
        float posY = minY - bgHeight - (6f * distanceScale);

        int bgAlpha = (int) (itemBackgroundAlpha.getFloatValue() * 255);
        int bgColor = new Color(0, 0, 0, bgAlpha).getRGB();

        MatrixStack ms = e.getMatrixStack();
        ms.push();
        ms.translate(posX, posY, 0);

        if (bgAlpha > 0) {
            RenderUtil.drawRoundedRect(
                    ms,
                    0,
                    0,
                    bgWidth,
                    bgHeight,
                    new Vector4f(3f * distanceScale, 3f * distanceScale, 3f * distanceScale, 3f * distanceScale),
                    bgColor
            );
        }

        float contentX = paddingX;
        float contentY = (bgHeight - (showText ? textHeight : iconSize)) / 2f;

        if (showIcon) {
            RenderSystem.enableDepthTest();
            RenderUtil.drawItemStack(
                    e.getDrawContext(),
                    stack,
                    contentX,
                    contentY,
                    iconSize
            );
            RenderSystem.disableDepthTest();

            contentX += iconSize + spacing;
        }

        if (showText) {
            ms.push();
            ms.translate(contentX, contentY, 0);
            ms.scale(distanceScale, distanceScale, 1.0f);

            float xOffset = 0;
            String[] segments = text.split("(?=§)");

            for (String segment : segments) {
                if (segment.isEmpty()) continue;

                int color = 0xFFFFFFFF;
                String displayText;

                if (segment.startsWith("§") && segment.length() > 1) {
                    char colorChar = segment.charAt(1);
                    color = getColorFromFormatCode(colorChar);
                    displayText = segment.length() > 2 ? segment.substring(2) : "";
                } else {
                    displayText = segment;
                }

                if (!displayText.isEmpty()) {
                    font.drawFontLeft(
                            ms,
                            displayText,
                            xOffset,
                            0,
                            color
                    );
                    xOffset += font.getWidth(displayText);
                }
            }

            ms.pop();
        }

        ms.pop();
    }

    private void renderItemBox(MatrixStack matrix, float minX, float minY, float maxX, float maxY, ItemEntity item) {
        float width = thickness.getFloatValue();
        float w = maxX - minX;
        float h = maxY - minY;

        int color1 = new Color(170, 0, 170).getRGB();
        int color2 = new Color(255, 85, 255).getRGB();

        Vector4f zero = new Vector4f(0, 0, 0, 0);

        if (!corners.get()) {
            RenderUtil.drawRoundedRect(matrix, minX, minY, w, width, zero, color1);
            RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w, width, zero, color2);
            RenderUtil.drawRoundedRect(matrix, minX, minY, width, h, zero, color1);
            RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h, zero, color2);
            return;
        }

        float cornerLen = cornerLength.getFloatValue();

        RenderUtil.drawRoundedRect(matrix, minX, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, minX, minY, width, h * cornerLen, zero, color2);

        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h * cornerLen, zero, color2);

        RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, minX, maxY - h * cornerLen, width, h * cornerLen, zero, color1);

        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, maxX - width, maxY - h * cornerLen, width, h * cornerLen, zero, color1);
    }

    private void renderItemNameTag(EventRender2D e,
                                   float minX, float minY,
                                   float maxX, float maxY,
                                   ItemEntity itemEntity) {

        FontDraw font = FontHelper.sfprobold[15];
        if (font == null) return;

        ItemStack stack = itemEntity.getStack();
        if (stack.isEmpty()) return;

        float scale = getPlayerScale(itemEntity);

        String itemName = stack.getName().getString();
        int count = stack.getCount();

        String text = itemName;
        if (count > 1) {
            text = itemName + " §7[" + count + "x]";
        }

        String cleanText = text.replaceAll("§[0-9a-fk-or]", "");

        float textWidth = font.getWidth(cleanText) * scale;
        float textHeight = font.getHeight() * scale;

        float paddingX = 4f * scale;
        float paddingY = 2f * scale;

        float bgWidth = textWidth + paddingX * 2f;
        float bgHeight = textHeight + paddingY * 2f;

        float centerX = (minX + maxX) / 2f;
        float posX = centerX - bgWidth / 2f;
        float posY = minY - bgHeight - (6f * scale);

        int bgColor = new Color(0, 0, 0, 150).getRGB();

        MatrixStack ms = e.getMatrixStack();
        ms.push();
        ms.translate(posX, posY, 0);
        ms.scale(scale, scale, 1.0f);

        RenderUtil.drawRoundedRect(
                ms,
                0,
                0,
                bgWidth / scale,
                bgHeight / scale,
                new Vector4f(3f, 3f, 3f, 3f),
                bgColor
        );

        float xOffset = paddingX / scale;
        String[] segments = text.split("(?=§)");

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            int color = 0xFFFFFFFF;
            String displayText;

            if (segment.startsWith("§")) {
                char colorChar = segment.charAt(1);
                color = getColorFromFormatCode(colorChar);

                if (segment.length() > 2) {
                    displayText = segment.substring(2);
                } else {
                    displayText = "";
                }
            } else {
                displayText = segment;
            }

            if (!displayText.isEmpty()) {
                font.drawFontLeft(
                        ms,
                        displayText,
                        xOffset,
                        paddingY / scale - 0.5f,
                        color
                );
                xOffset += font.getWidth(displayText);
            }
        }

        ms.pop();
    }

    private void renderItemIcon(DrawContext context, ItemStack stack, float x, float y, float size) {
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(size / 16f, size / 16f, 1f);

        RenderUtil.drawItemStack(context, stack, 0, 0, 16f);

        if (stack.getCount() > 1) {
            matrices.push();
            matrices.translate(0, 0, 200);

            String countText = String.valueOf(stack.getCount());
            FontDraw font = FontHelper.sfprobold[12];
            if (font != null) {
                float textWidth = font.getWidth(countText);
                float textX = 16 - textWidth - 1;
                float textY = 16 - font.getHeight() - 1;

                RenderUtil.drawRoundedRect(
                        matrices,
                        textX - 1,
                        textY - 1,
                        textWidth + 2,
                        font.getHeight() + 2,
                        new Vector4f(2f, 2f, 2f, 2f),
                        new Color(0, 0, 0, 150).getRGB()
                );

                font.drawFontLeft(
                        matrices,
                        countText,
                        textX,
                        textY,
                        0xFFFFFFFF
                );
            }

            matrices.pop();
        }

        matrices.pop();
    }

    private String getItemDisplayText(ItemStack stack) {
        String itemName = stack.getName().getString();
        int count = stack.getCount();

        if (stack.hasEnchantments()) {
            itemName = "§b" + itemName;
        }

        if (isValuableItem(stack)) {
            itemName = "§6" + itemName;
        }

        if (count > 1) {
            return itemName + "§r §7[" + count + "x]";
        }

        return itemName + "§r";
    }

    private boolean isValuableItem(ItemStack stack) {
        return stack.getItem() == Items.DIAMOND ||
                stack.getItem() == Items.NETHERITE_INGOT ||
                stack.getItem() == Items.ENDER_PEARL ||
                stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE ||
                stack.getItem() == Items.NETHERITE_SCRAP ||
                stack.getItem() == Items.ANCIENT_DEBRIS ||
                stack.getItem() == Items.ECHO_SHARD ||
                stack.getItem() == Items.TRIDENT ||
                stack.getItem() == Items.TOTEM_OF_UNDYING ||
                stack.getItem() == Items.NETHER_STAR;
    }

    private void renderArmorAndHands(EventRender2D e,
                                     float minX, float minY,
                                     float maxX, float maxY,
                                     PlayerEntity player,
                                     float scale) {

        MatrixStack ms = e.getMatrixStack();

        ArrayList<ItemStack> items = new ArrayList<>();

        player.getArmorItems().forEach(stack -> {
            if (!stack.isEmpty()) items.add(stack);
        });

        if (!player.getMainHandStack().isEmpty())
            items.add(player.getMainHandStack());

        if (!player.getOffHandStack().isEmpty())
            items.add(player.getOffHandStack());

        if (items.isEmpty()) return;

        float baseIconSize = 12f;
        float baseSpacing = 13f;

        float iconSize = baseIconSize * scale;
        float spacing = baseSpacing * scale;

        float totalWidth = items.size() * spacing - 1;
        float centerX = (minX + maxX) / 2f;
        float startX = centerX - totalWidth / 2f;

        float iconY = minY - (33f * scale);

        float bgPaddingX = 4f * scale;
        float bgPaddingY = 3f * scale;

        float bgX = startX - bgPaddingX;
        float bgY = iconY - bgPaddingY;
        float bgW = totalWidth + bgPaddingX * 2f;
        float bgH = iconSize + bgPaddingY * 2f;

        RenderUtil.drawRoundedRect(
                ms,
                bgX,
                bgY - 5,
                bgW,
                bgH,
                new Vector4f(3f * scale, 3f * scale, 3f * scale, 3f * scale),
                new Color(0, 0, 0, 140).getRGB()
        );

        RenderSystem.enableDepthTest();

        for (int i = 0; i < items.size(); i++) {
            RenderUtil.drawItemStack(
                    e.getDrawContext(),
                    items.get(i),
                    startX + i * spacing,
                    iconY - 5,
                    iconSize
            );
        }

        RenderSystem.disableDepthTest();
    }


    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!isEnabled()) return;
        if (mc.options.hudHidden) return;

        Matrix4f matrix = e.getDrawContext().getMatrices().peek().getPositionMatrix();

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Iterator<Entity> iterator = toRender.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();

            Vec3d interp = entity.getLerpedPos(mc.getRenderTickCounter().getTickDelta(true));
            Box box = entity.getBoundingBox().offset(interp.subtract(entity.getPos()));

            Vec3d[] corners = new Vec3d[]{
                    new Vec3d(box.minX, box.minY, box.minZ),
                    new Vec3d(box.minX, box.minY, box.maxZ),
                    new Vec3d(box.maxX, box.minY, box.minZ),
                    new Vec3d(box.maxX, box.minY, box.maxZ),
                    new Vec3d(box.minX, box.maxY, box.minZ),
                    new Vec3d(box.minX, box.maxY, box.maxZ),
                    new Vec3d(box.maxX, box.maxY, box.minZ),
                    new Vec3d(box.maxX, box.maxY, box.maxZ)
            };

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            boolean anyVisible = false;

            for (Vec3d corner : corners) {
                Vec3d projected = RenderUtil.render3D.worldSpaceToScreenSpace(corner);
                if (projected.z <= 0 || projected.z >= 1) continue;

                anyVisible = true;

                minX = (float) Math.min(minX, projected.x);
                minY = (float) Math.min(minY, projected.y);
                maxX = (float) Math.max(maxX, projected.x);
                maxY = (float) Math.max(maxY, projected.y);
            }

            if (!anyVisible) continue;

            if (maxX < 0 || maxY < 0 || minX > mc.getWindow().getScaledWidth() || minY > mc.getWindow().getScaledHeight())
                continue;

            if (showNameTags.get()) {
                renderNameTag(e, minX, minY, maxX, maxY, entity);
            }

            if (showArmor.get() && entity instanceof PlayerEntity player) {
                renderArmorAndHands(e, minX, minY, maxX, maxY, player, getPlayerScale(entity));
            }

            if (effects.get() && entity instanceof LivingEntity living) {
                renderEffects(e, minX, minY, maxX, maxY, living, getPlayerScale(entity));
            }

            if (showBox.get()) {
                renderBox(e.getMatrixStack(), minX, minY, maxX, maxY, entity);
            }
        }

        if (getTargetSetting("Предметы")) {
            renderItems(e);
        }

        RenderUtil.render3D.endBuilding(buffer);
        RenderUtil.disableRender();
    }


    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    private void renderEffects(EventRender2D e,
                               float minX, float minY,
                               float maxX, float maxY,
                               LivingEntity entity,
                               float scale) {

        if (!effects.get()) return;

        MatrixStack ms = e.getMatrixStack();
        FontDraw font = FontHelper.sfprobold[10];
        if (font == null) return;

        ArrayList<String> effectsList = new ArrayList<>();

        entity.getActiveStatusEffects().forEach((registryEntry, instance) -> {
            if (instance != null && registryEntry != null) {
                StatusEffect effect = registryEntry.value();
                if (effect != null) {
                    String effectName = getEffectDisplayName(effect, instance);
                    if (effectName != null && !effectName.isEmpty()) {
                        effectsList.add(effectName);
                    }
                }
            }
        });

        if (effectsList.isEmpty()) return;

        float maxWidth = 0;
        float totalHeight = 0;
        float spacing = 2f * scale;

        for (String effect : effectsList) {
            float width = font.getWidth(effect) * scale;
            float height = font.getHeight() * scale;

            maxWidth = Math.max(maxWidth, width);
            totalHeight += height + spacing;
        }

        if (totalHeight > 0) totalHeight -= spacing;

        float bgPaddingX = 4f * scale;
        float bgPaddingY = 4f * scale;

        float bgX = minX - maxWidth - bgPaddingX * 2f - (10f * scale);
        float bgY = minY;
        float bgW = maxWidth + bgPaddingX * 2f;
        float bgH = totalHeight + bgPaddingY * 2f;

        RenderUtil.drawRoundedRect(
                ms,
                bgX,
                bgY,
                bgW,
                bgH,
                new org.joml.Vector4f(3f * scale, 3f * scale, 3f * scale, 3f * scale),
                new Color(0, 0, 0, 140).getRGB()
        );

        float currentY = bgY + bgPaddingY;

        for (String effect : effectsList) {
            ms.push();
            ms.translate(bgX + bgPaddingX, currentY, 0);
            ms.scale(scale, scale, 1.0f);

            int color = getEffectColor(effect);
            font.drawFontLeft(ms, effect, 0, 0, color);

            ms.pop();

            currentY += font.getHeight() * scale + spacing;
        }
    }

    private String getEffectDisplayName(net.minecraft.entity.effect.StatusEffect effect,
                                        net.minecraft.entity.effect.StatusEffectInstance instance) {
        if (effect == null) return "";

        try {
            String name = effect.getName().getString();
            int amplifier = instance.getAmplifier();
            int duration = instance.getDuration() / 20;

            name = name.replace("Effect.", "")
                    .replace("minecraft:", "")
                    .replace("speed", "Speed")
                    .replace("slowness", "Slow")
                    .replace("haste", "Haste")
                    .replace("mining_fatigue", "Fatigue")
                    .replace("strength", "Strength")
                    .replace("instant_health", "Heal")
                    .replace("instant_damage", "Damage")
                    .replace("jump_boost", "Jump")
                    .replace("nausea", "Nausea")
                    .replace("regeneration", "Regen")
                    .replace("resistance", "Resist")
                    .replace("fire_resistance", "Fire Resist")
                    .replace("water_breathing", "Water Breath")
                    .replace("invisibility", "Invis")
                    .replace("blindness", "Blind")
                    .replace("night_vision", "Night Vision")
                    .replace("hunger", "Hunger")
                    .replace("weakness", "Weak")
                    .replace("poison", "Poison")
                    .replace("wither", "Wither")
                    .replace("health_boost", "Health Boost")
                    .replace("absorption", "Absorption")
                    .replace("saturation", "Saturation")
                    .replace("glowing", "Glowing")
                    .replace("levitation", "Levitation")
                    .replace("luck", "Luck")
                    .replace("unluck", "Bad Luck")
                    .replace("slow_falling", "Slow Fall")
                    .replace("conduit_power", "Conduit")
                    .replace("dolphins_grace", "Dolphin")
                    .replace("bad_omen", "Bad Omen")
                    .replace("hero_of_the_village", "Hero")
                    .replace("darkness", "Darkness");

            if (amplifier > 0) {
                name += " " + (amplifier + 1);
            }

            if (duration < 60) {
                name += " §7" + duration + "s";
            } else if (duration < 3600) {
                name += " §7" + (duration / 60) + "m";
            }

            return name;

        } catch (Exception e) {
            return "";
        }
    }

    private int getEffectColor(String effectName) {
        if (effectName.contains("Speed") || effectName.contains("Haste") ||
                effectName.contains("Strength") || effectName.contains("Jump")) {
            return 0xFF55FF55;
        } else if (effectName.contains("Heal") || effectName.contains("Regen") ||
                effectName.contains("Resist") || effectName.contains("Absorption")) {
            return 0xFFFF5555;
        } else if (effectName.contains("Slow") || effectName.contains("Weak") ||
                effectName.contains("Poison") || effectName.contains("Wither")) {
            return 0xFFFFAA00;
        } else if (effectName.contains("Invis") || effectName.contains("Night Vision") ||
                effectName.contains("Water Breath")) {
            return 0xFF55FFFF;
        } else if (effectName.contains("Fire Resist")) {
            return 0xFFFFFF55;
        }

        return 0xFFFFFFFF;
    }

    private boolean shouldRenderEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (entity == mc.player) {
                return getTargetSetting("Меня");
            }
            if (getTargetSetting("Друзей") && FriendManager.getInstance().isFriend(player.getName().getString())) {
                return true;
            }
            return getTargetSetting("Игроков");
        }
        return false;
    }

    private void renderBox(MatrixStack matrix, float minX, float minY, float maxX, float maxY, Entity entity) {
        float width = thickness.getFloatValue();
        float w = maxX - minX;
        float h = maxY - minY;

        int color1 = ColorUtil.getColorStyle(0);
        int color2 = ColorUtil.getColorStyle(90);

        Vector4f zero = new Vector4f(0, 0, 0, 0);

        if (!corners.get()) {
            RenderUtil.drawRoundedRect(matrix, minX, minY, w, width, zero, color1);
            RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w, width, zero, color2);
            RenderUtil.drawRoundedRect(matrix, minX, minY, width, h, zero, color1);
            RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h, zero, color2);
            return;
        }

        float cornerLen = cornerLength.getFloatValue();

        RenderUtil.drawRoundedRect(matrix, minX, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, minX, minY, width, h * cornerLen, zero, color2);

        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, minY, w * cornerLen, width, zero, color1);
        RenderUtil.drawRoundedRect(matrix, maxX - width, minY, width, h * cornerLen, zero, color2);

        RenderUtil.drawRoundedRect(matrix, minX, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, minX, maxY - h * cornerLen, width, h * cornerLen, zero, color1);

        RenderUtil.drawRoundedRect(matrix, maxX - w * cornerLen, maxY - width, w * cornerLen, width, zero, color2);
        RenderUtil.drawRoundedRect(matrix, maxX - width, maxY - h * cornerLen, width, h * cornerLen, zero, color1);
    }

    private boolean getTargetSetting(String name) {
        BooleanSetting setting = targets.getValueByName(name);
        if (setting != null) {
            return setting.get();
        }

        if (name.equals("Текст") || name.equals("Иконка")) {
            BooleanSetting modeSetting = itemMode.getValueByName(name);
            return modeSetting != null && modeSetting.get();
        }

        return false;
    }

    private void renderNameTag(EventRender2D e,
                               float minX, float minY,
                               float maxX, float maxY,
                               Entity entity) {

        if (!(entity instanceof LivingEntity living)) return;

        FontDraw font = FontHelper.sfprobold[15];
        if (font == null) return;

        float scale = getPlayerScale(entity);

        String name = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getDisplayName().getString();

        String formattedName = replaceDonateSymbols(name);

        float health = ServerUtil.getHealth(living);

        String text;
        if (entity instanceof PlayerEntity) {
            text = formattedName + " §c" + (int) health + "HP";
        } else {
            text = formattedName + " §c" + (int) health + "HP";
        }

        String cleanText = text.replaceAll("§[0-9a-fk-or]", "");
        float textWidth = font.getWidth(cleanText) * scale;
        float textHeight = font.getHeight() * scale;

        float paddingX = 4f * scale;
        float paddingY = 2f * scale;

        float bgWidth = textWidth + paddingX * 2f;
        float bgHeight = textHeight + paddingY * 2f;

        float centerX = (minX + maxX) / 2f;
        float posX = centerX - bgWidth / 2f;
        float posY = minY - bgHeight - (6f * scale);

        int bgColor;
        if (entity instanceof PlayerEntity player &&
                FriendManager.getInstance().isFriend(player.getName().getString())) {
            bgColor = new Color(0, 255, 0, 90).getRGB();
        } else {
            bgColor = new Color(0, 0, 0, 150).getRGB();
        }

        MatrixStack ms = e.getMatrixStack();
        ms.push();
        ms.translate(posX, posY, 0);
        ms.scale(scale, scale, 1.0f);

        RenderUtil.drawRoundedRect(
                ms,
                0,
                0,
                bgWidth / scale,
                bgHeight / scale,
                new Vector4f(3f, 3f, 3f, 3f),
                bgColor
        );

        float xOffset = paddingX / scale;

        String[] segments = text.split("(?=§)");

        if (!text.startsWith("§") && segments.length > 0 && !segments[0].isEmpty()) {
            String firstSegment = segments[0];
            font.drawFontLeft(
                    ms,
                    firstSegment,
                    xOffset,
                    paddingY / scale - 0.5f,
                    0xFFFFFFFF
            );
            xOffset += font.getWidth(firstSegment);
        }

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            if (segment.startsWith("§") && segment.length() >= 2) {
                char colorChar = segment.charAt(1);
                int color = getColorFromFormatCode(colorChar);

                String displayText = segment.length() > 2 ? segment.substring(2) : "";

                if (!displayText.isEmpty()) {
                    font.drawFontLeft(
                            ms,
                            displayText,
                            xOffset,
                            paddingY / scale - 0.5f,
                            color
                    );
                    xOffset += font.getWidth(displayText);
                }
            }
        }

        ms.pop();
    }

    private String replaceDonateSymbols(String name) {
        String result = name;

        for (Map.Entry<String, String> entry : donateSymbols.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private int getColorFromFormatCode(char code) {
        return switch (code) {
            case '0' -> 0xFF000000; // black
            case '1' -> 0xFF0000AA; // dark_blue
            case '2' -> 0xFF00AA00; // dark_green
            case '3' -> 0xFF00AAAA; // dark_aqua
            case '4' -> 0xFFAA0000; // dark_red
            case '5' -> 0xFFAA00AA; // dark_purple
            case '6' -> 0xFFFFAA00; // gold
            case '7' -> 0xFFAAAAAA; // gray
            case '8' -> 0xFF555555; // dark_gray
            case '9' -> 0xFF5555FF; // blue
            case 'a' -> 0xFF55FF55; // green
            case 'b' -> 0xFF55FFFF; // aqua
            case 'c' -> 0xFFFF5555; // red
            case 'd' -> 0xFFFF55FF; // light_purple
            case 'e' -> 0xFFFFFF55; // yellow
            case 'f' -> 0xFFFFFFFF; // white
            case 'r' -> 0xFFFFFFFF; // reset
            default -> 0xFFFFFFFF;
        };
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