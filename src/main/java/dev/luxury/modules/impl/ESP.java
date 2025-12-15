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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    private final BooleanSetting corners = new BooleanSetting("Углы", false);
    private final SliderSetting cornerLength = new SliderSetting("Высота", 0.3f, 0.1f, 0.4f, 0.1f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.9f, 0.5f, 2.0f, 0.1f);
    private final BooleanSetting showNameTags = new BooleanSetting("Неймтеги", true);
    private final BooleanSetting hideVanillaTags = new BooleanSetting("Скрыть ванильные", true);
    private final BooleanSetting showArmor = new BooleanSetting("Броня и предметы", false);

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
        addSettings(targets, corners, cornerLength, thickness, showNameTags, hideVanillaTags, showArmor);
        instance = this;
    }

    public static ESP getInstance() {
        return instance;
    }

    private float getDistanceScale(Entity entity) {
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
        return getTargetSetting("Предметы");
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

    @EventTarget
    public void onGameTick(EventTick event) {
        if (!isEnabled()) return;

        toRender.clear();

        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ClientPlayerEntity && mc.options.getPerspective() == Perspective.FIRST_PERSON)
                continue;
            if (entity.isRemoved())
                continue;

            if (shouldRenderEntity(entity)) {
                toRender.add(entity);
            }
        }
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
                renderArmorAndHands(e, minX, minY, maxX, maxY, player, getDistanceScale(entity));
            }

            renderBox(e.getMatrixStack(), minX, minY, maxX, maxY, entity);
        }

        RenderUtil.render3D.endBuilding(buffer);
        RenderUtil.disableRender();
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

        if (entity instanceof ItemEntity) {
            return getTargetSetting("Предметы");
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
        return setting != null && setting.get();
    }

    private void renderNameTag(EventRender2D e,
                               float minX, float minY,
                               float maxX, float maxY,
                               Entity entity) {

        if (!(entity instanceof LivingEntity living)) return;

        FontDraw font = FontHelper.sfprobold[15];
        if (font == null) return;

        float scale = getDistanceScale(entity);

        String name = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getDisplayName().getString();

        for (Map.Entry<String, String> entry : donateSymbols.entrySet()) {
            if (name.contains(entry.getKey())) {
                name = name.replace(entry.getKey(), entry.getValue());

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