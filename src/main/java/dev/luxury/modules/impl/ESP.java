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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EnchantableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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

    // НОВЫЕ НАСТРОЙКИ
    private final BooleanSetting showEnchant = new BooleanSetting("Показывать зачарования", true);
    private final BooleanSetting shortName = new BooleanSetting("Сокращенные названия", true);
    private final SliderSetting enchantScale = new SliderSetting("Масштаб текста", 0.8f, 0.5f, 1.2f, 0.1f);

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
        put("ꕅ", "§c§lVAMPIRE");
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

    // Карта сокращений для зачарований
    private final Map<String, String> enchantShortNames = new HashMap<String, String>() {{
        put("protection", "З");
        put("fire_protection", "Ог");
        put("feather_falling", "ПП");
        put("blast_protection", "Вз");
        put("projectile_protection", "Ст");
        put("respiration", "Дых");
        put("aqua_affinity", "Вод");
        put("thorns", "Шип");
        put("depth_strider", "Глуб");
        put("frost_walker", "Лед");
        put("soul_speed", "Душ");
        put("sharpness", "О");
        put("smite", "Неж");
        put("bane_of_arthropods", "Чл");
        put("knockback", "Отб");
        put("fire_aspect", "Огн");
        put("looting", "Доб");
        put("sweeping", "Шир");
        put("efficiency", "Эф");
        put("silk_touch", "Шелк");
        put("unbreaking", "Прч");
        put("fortune", "Уд");
        put("power", "Мщ");
        put("punch", "Отт");
        put("flame", "Плм");
        put("infinity", "Бск");
        put("luck_of_the_sea", "УдР");
        put("lure", "Прим");
        put("loyalty", "Врн");
        put("impaling", "Прб");
        put("riptide", "Взв");
        put("channeling", "Мол");
        put("multishot", "Мнж");
        put("quick_charge", "Бзз");
        put("piercing", "Прк");
        put("mending", "Рем");
        put("vanishing_curse", "Прп");
        put("binding_curse", "Связ");
        put("swift_sneak", "Бстр");
    }};

    private ArrayList<Entity> toRender = new ArrayList<>();
    private static ESP instance;

    public ESP() {
        addSettings(targets, itemMode, effects, itemBackgroundAlpha, itemScale,
                showBox, corners, cornerLength, thickness,
                showNameTags, hideVanillaTags, showArmor,
                showEnchant, shortName, enchantScale); // Добавлены новые настройки
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

        if (showEnchant.get()) {
            renderEnchantments(e, minX, minY, maxX, maxY, player, scale);
        }
    }

    private void renderEnchantments(EventRender2D e,
                                    float minX, float minY,
                                    float maxX, float maxY,
                                    PlayerEntity player,
                                    float scale) {

        if (!showEnchant.get()) return;

        MatrixStack ms = e.getMatrixStack();
        FontDraw font = FontHelper.sfprobold[10];
        if (font == null) return;

        List<String> enchantmentsList = new ArrayList<>();

        for (ItemStack armorStack : player.getArmorItems()) {
            if (!armorStack.isEmpty()) {
                ItemEnchantmentsComponent enchantments = armorStack.get(DataComponentTypes.ENCHANTMENTS);
                if (enchantments != null) {
                    for (var entry : enchantments.getEnchantmentEntries()) {
                        String enchantName = getEnchantDisplayName(entry.getKey(), entry.getIntValue());
                        if (enchantName != null && !enchantName.isEmpty()) {
                            enchantmentsList.add(enchantName);
                        }
                    }
                }
            }
        }

        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            ItemEnchantmentsComponent enchantments = mainHand.get(DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null) {
                for (var entry : enchantments.getEnchantmentEntries()) {
                    String enchantName = getEnchantDisplayName(entry.getKey(), entry.getIntValue());
                    if (enchantName != null && !enchantName.isEmpty()) {
                        enchantmentsList.add(enchantName);
                    }
                }
            }
        }

        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) {
            ItemEnchantmentsComponent enchantments = offHand.get(DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null) {
                for (var entry : enchantments.getEnchantmentEntries()) {
                    String enchantName = getEnchantDisplayName(entry.getKey(), entry.getIntValue());
                    if (enchantName != null && !enchantName.isEmpty()) {
                        enchantmentsList.add(enchantName);
                    }
                }
            }
        }

        if (enchantmentsList.isEmpty()) return;

        enchantmentsList = enchantmentsList.stream().distinct().collect(Collectors.toList());

        float maxWidth = 0;
        float totalHeight = 0;
        float spacing = 2f * scale * enchantScale.getFloatValue();

        for (String enchant : enchantmentsList) {
            float width = font.getWidth(enchant) * scale * enchantScale.getFloatValue();
            float height = font.getHeight() * scale * enchantScale.getFloatValue();

            maxWidth = Math.max(maxWidth, width);
            totalHeight += height + spacing;
        }

        if (totalHeight > 0) totalHeight -= spacing;

        float bgPaddingX = 4f * scale * enchantScale.getFloatValue();
        float bgPaddingY = 4f * scale * enchantScale.getFloatValue();

        float bgX = maxX + (10f * scale);
        float bgY = minY;
        float bgW = maxWidth + bgPaddingX * 2f;
        float bgH = totalHeight + bgPaddingY * 2f;

        RenderUtil.drawRoundedRect(
                ms,
                bgX,
                bgY,
                bgW,
                bgH,
                new Vector4f(3f * scale, 3f * scale, 3f * scale, 3f * scale),
                new Color(30, 30, 40, 180).getRGB()
        );

        RenderUtil.drawBorder(
                ms,
                bgX,
                bgY,
                bgW,
                bgH,
                new Vector4f(3f * scale, 3f * scale, 3f * scale, 3f * scale),
                new Color(100, 70, 200, 150).getRGB(),
                1.0f,
                1.0f,
                1.0f,
                false
        );

        float currentY = bgY + bgPaddingY;

        ms.push();
        ms.translate(bgX + bgPaddingX, currentY, 0);
        ms.scale(scale * enchantScale.getFloatValue(), scale * enchantScale.getFloatValue(), 1.0f);

        font.drawFontLeft(ms, "Зачарования:", 0, 0, new Color(170, 150, 255).getRGB());

        ms.pop();

        currentY += font.getHeight() * scale * enchantScale.getFloatValue() + (2f * scale);

        RenderUtil.drawRoundedRect(
                ms,
                bgX + bgPaddingX,
                currentY - (1f * scale),
                maxWidth,
                1,
                new Vector4f(0, 0, 0, 0),
                new Color(100, 70, 200, 100).getRGB()
        );

        currentY += (2f * scale);

        for (String enchant : enchantmentsList) {
            ms.push();
            ms.translate(bgX + bgPaddingX, currentY, 0);
            ms.scale(scale * enchantScale.getFloatValue(), scale * enchantScale.getFloatValue(), 1.0f);

            int color = getEnchantmentColor(enchant);
            font.drawFontLeft(ms, enchant, 0, 0, color);

            ms.pop();

            currentY += font.getHeight() * scale * enchantScale.getFloatValue() + spacing;
        }
    }

    private String getEnchantDisplayName(RegistryEntry<Enchantment> enchantmentEntry, int level) {
        if (enchantmentEntry == null) return "";

        try {
            String enchantId = "unknown";
            String displayName = "Enchantment";

            if (enchantmentEntry.getKey().isPresent()) {
                var key = enchantmentEntry.getKey().get();
                enchantId = key.getValue().getPath();
            }

            try {
                displayName = Text.translatable("enchantment.minecraft." + enchantId).getString();
            } catch (Exception e) {
                displayName = enchantId;
            }

            displayName = translateEnchantmentName(displayName, enchantId);

            if (shortName.get()) {
                String shortNameText = enchantShortNames.get(enchantId);
                if (shortNameText != null) {
                    return shortNameText + level;
                }

                if (!displayName.isEmpty()) {
                    return displayName.charAt(0) + String.valueOf(level);
                }

                return "E" + level;
            }

            return displayName + " " + toRoman(level);

        } catch (Exception e) {
            return "Зачарование " + toRoman(level);
        }
    }

    private String translateEnchantmentName(String name, String id) {
        Map<String, String> translations = new HashMap<String, String>() {{
            put("protection", "Защита");
            put("fire_protection", "Защита от огня");
            put("feather_falling", "Невесомость");
            put("blast_protection", "Взрывоустойчивость");
            put("projectile_protection", "Защита от снарядов");
            put("respiration", "Подводное дыхание");
            put("aqua_affinity", "Подводник");
            put("thorns", "Шипы");
            put("depth_strider", "Глубинный шаг");
            put("frost_walker", "Ледяная поступь");
            put("soul_speed", "Скорость души");
            put("sharpness", "Острота");
            put("smite", "Небесная кара");
            put("bane_of_arthropods", "Бич членистоногих");
            put("knockback", "Отдача");
            put("fire_aspect", "Огненный аспект");
            put("looting", "Добыча");
            put("sweeping", "Широкий взмах");
            put("efficiency", "Эффективность");
            put("silk_touch", "Шелковое касание");
            put("unbreaking", "Прочность");
            put("fortune", "Удача");
            put("power", "Мощность");
            put("punch", "Отталкивание");
            put("flame", "Пламя");
            put("infinity", "Бесконечность");
            put("luck_of_the_sea", "Удача моряка");
            put("lure", "Приманка");
            put("loyalty", "Верность");
            put("impaling", "Пронзание");
            put("riptide", "Отлив");
            put("channeling", "Громовержец");
            put("multishot", "Множественный выстрел");
            put("quick_charge", "Быстрая перезарядка");
            put("piercing", "Пронзание");
            put("mending", "Починка");
            put("vanishing_curse", "Проклятие исчезновения");
            put("binding_curse", "Проклятие связывания");
            put("swift_sneak", "Быстрый шаг");
        }};

        return translations.getOrDefault(id, name);
    }

    private int getEnchantmentColor(String enchantName) {
        if (enchantName.contains("Защита") || enchantName.contains("Прочность") ||
                enchantName.contains("Невесомость") || enchantName.contains("Подвод") ||
                enchantName.contains("Взрыв") || enchantName.contains("Снаряд") ||
                enchantName.contains("Шип") || enchantName.contains("Глубин") ||
                enchantName.contains("Ледя") || enchantName.contains("Скорость") ||
                enchantName.contains("З") || enchantName.contains("Ог") ||
                enchantName.contains("ПП") || enchantName.contains("Вз") ||
                enchantName.contains("Ст") || enchantName.contains("Дых") ||
                enchantName.contains("Вод") || enchantName.contains("Шип") ||
                enchantName.contains("Глуб") || enchantName.contains("Лед") ||
                enchantName.contains("Душ") || enchantName.contains("Прч")) {
            return new Color(100, 180, 255).getRGB();
        }

        if (enchantName.contains("Острота") || enchantName.contains("Небесная") ||
                enchantName.contains("Членистоногих") || enchantName.contains("Отдача") ||
                enchantName.contains("Огненный") || enchantName.contains("Добыча") ||
                enchantName.contains("Широкий") || enchantName.contains("Мощность") ||
                enchantName.contains("Отталкивание") || enchantName.contains("Пламя") ||
                enchantName.contains("Верность") || enchantName.contains("Пронзание") ||
                enchantName.contains("Отлив") || enchantName.contains("Громовержец") ||
                enchantName.contains("Множественный") || enchantName.contains("Быстрая") ||
                enchantName.contains("О") || enchantName.contains("Неж") ||
                enchantName.contains("Чл") || enchantName.contains("Отб") ||
                enchantName.contains("Огн") || enchantName.contains("Доб") ||
                enchantName.contains("Шир") || enchantName.contains("Мщ") ||
                enchantName.contains("Отт") || enchantName.contains("Плм") ||
                enchantName.contains("Врн") || enchantName.contains("Прб") ||
                enchantName.contains("Взв") || enchantName.contains("Мол") ||
                enchantName.contains("Мнж") || enchantName.contains("Бзз") ||
                enchantName.contains("Прк")) {
            return new Color(255, 100, 100).getRGB();
        }

        if (enchantName.contains("Эффективность") || enchantName.contains("Шелковое") ||
                enchantName.contains("Удача") || enchantName.contains("Бесконечность") ||
                enchantName.contains("Удача моряка") || enchantName.contains("Приманка") ||
                enchantName.contains("Починка") || enchantName.contains("Эф") ||
                enchantName.contains("Шелк") || enchantName.contains("Уд") ||
                enchantName.contains("Бск") || enchantName.contains("УдР") ||
                enchantName.contains("Прим") || enchantName.contains("Рем")) {
            return new Color(100, 255, 100).getRGB();
        }

        if (enchantName.contains("Проклятие") || enchantName.contains("Исчезновения") ||
                enchantName.contains("Связывания") || enchantName.contains("Прп") ||
                enchantName.contains("Связ")) {
            return new Color(180, 100, 255).getRGB();
        }

        return new Color(255, 255, 100).getRGB();
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

        // Используем тот же метод, что и в старом коде
        for (Map.Entry<String, String> entry : donateSymbols.entrySet()) {
            if (name.contains(entry.getKey())) {
                name = name.replace(entry.getKey(), entry.getValue());

                // Удаляем §f после замены доната
                name = name.replace("§f", "");
                break;
            }
        }

        float health = ServerUtil.getHealth(living);

        String text;
        if (entity instanceof PlayerEntity) {
            text = name + " §c" + (int) health + "HP";
        } else {
            text = name + " §c" + (int) health + "HP";
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

    private String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    private void renderColoredText(MatrixStack ms, FontDraw font, String text, float x, float y) {
        float currentX = x;
        int currentColor = 0xFFFFFFFF;

        String[] parts = text.split("(?=§[0-9a-f])|(?=§r)");

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("§")) {
                if (part.length() >= 2) {
                    char colorCode = part.charAt(1);
                    currentColor = getColorFromFormatCode(colorCode);

                    if (part.length() > 2) {
                        String textPart = part.substring(2);
                        if (!textPart.isEmpty()) {
                            font.drawFontLeft(ms, textPart, currentX, y, currentColor);
                            currentX += font.getWidth(textPart);
                        }
                    }
                }
            } else {
                font.drawFontLeft(ms, part, currentX, y, currentColor);
                currentX += font.getWidth(part);
            }
        }
    }

    private void renderFormattedText(MatrixStack ms, FontDraw font, String text, float x, float y) {
        float currentX = x;

        String[] segments = text.split("(?=§)");

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            if (segment.startsWith("§")) {
                List<Object> parsed = parseFormattingSegment(segment);
                String displayText = (String) parsed.get(0);
                int color = (int) parsed.get(1);
                boolean bold = (boolean) parsed.get(2);

                if (!displayText.isEmpty()) {

                    font.drawFontLeft(
                            ms,
                            displayText,
                            currentX,
                            y,
                            color
                    );
                    currentX += font.getWidth(displayText);

                }
            } else {
                font.drawFontLeft(
                        ms,
                        segment,
                        currentX,
                        y,
                        0xFFFFFFFF
                );
                currentX += font.getWidth(segment);
            }
        }
    }

    private List<Object> parseFormattingSegment(String segment) {
        String displayText = "";
        int color = 0xFFFFFFFF;
        boolean bold = false;

        if (segment.startsWith("§") && segment.length() >= 2) {
            int i = 1;
            while (i < segment.length() && segment.charAt(i) != '&' && !Character.isLetterOrDigit(segment.charAt(i))) {
                i++;
            }

            String formatting = segment.substring(1, Math.min(i, segment.length()));

            if (formatting.length() >= 1) {
                color = getColorFromFormatCode(formatting.charAt(0));
            }

            if (segment.contains("&l")) {
                bold = true;
                segment = segment.replace("&l", "");
            }

            displayText = segment.substring(i);
        }

        return Arrays.asList(displayText, color, bold);
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
        switch (code) {
            case '0': return 0xFF000000;
            case '1': return 0xFF0000AA;
            case '2': return 0xFF00AA00;
            case '3': return 0xFF00AAAA;
            case '4': return 0xFFAA0000;
            case '5': return 0xFFAA00AA;
            case '6': return 0xFFFFAA00;
            case '7': return 0xFFAAAAAA;
            case '8': return 0xFF555555;
            case '9': return 0xFF5555FF;
            case 'a': return 0xFF55FF55;
            case 'b': return 0xFF55FFFF;
            case 'c': return 0xFFFF5555;
            case 'd': return 0xFFFF55FF;
            case 'e': return 0xFFFFFF55;
            case 'f': return 0xFFFFFFFF;
            case 'r': return 0xFFFFFFFF;
            default: return 0xFFFFFFFF;
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