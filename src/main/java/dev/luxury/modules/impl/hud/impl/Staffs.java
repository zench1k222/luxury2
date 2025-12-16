package dev.luxury.modules.impl.hud.impl;

import com.mojang.authlib.GameProfile;
import dev.luxury.modules.impl.hud.api.DraggableHudElement;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.luxury.utils.render.RenderUtil;
import dev.luxury.utils.render.ScissorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Staffs extends DraggableHudElement {
    private static final int PADDING = 5;
    private static final int ITEM_HEIGHT = 12;
    private static final Set<String> STAFF_PREFIXES = Set.of("helper", "ᴀдмин", "moder", "staff", "admin", "curator", "стажёр", "сотрудник", "помощник", "админ", "модер");
    private static final Map<String, StaffInfo> staffCache = new LinkedHashMap<>();
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000;
    private final String[] icons = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

    public Staffs(String name, float x, float y) {
        super(name, x, y);
    }

    @Override
    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.getMatrices();

        updateStaffList();

        FontDraw sfpro = FontHelper.sfprobold[18];
        FontDraw sfpro1 = FontHelper.sfprobold[16];
        FontDraw sfpro2 = FontHelper.sfprobold[13];
        FontDraw prefixFont = FontHelper.sfprobold[10];
        FontDraw iconsFont = FontHelper.icons[20];

        int colorstandart = new Color(25, 25, 30, 255).getRGB();
        int colorfonts1 = Color.WHITE.getRGB();
        int colorfonts2 = new Color(150, 150, 160).getRGB();

        List<StaffInfo> staffList = new ArrayList<>(staffCache.values());

        float maxWidth = 97.5f;
        for (StaffInfo staff : staffList) {
            float nameWidth = sfpro1.getWidth(staff.getName());
            float prefixWidth = prefixFont.getWidth(staff.getPrefix());
            float timeWidth = sfpro2.getWidth(staff.getOnlineTime());
            float headSize = 8f;

            float totalWidth = PADDING + headSize + 3 + nameWidth + 2 + prefixWidth + 5 + timeWidth + PADDING;
            if (totalWidth > maxWidth) {
                maxWidth = totalWidth;
            }
        }

        float width = maxWidth;
        float startX = this.x;
        float startY = this.y;
        int titleHeight = 14;

        int totalHeight = (PADDING * 2 + titleHeight + staffList.size() * ITEM_HEIGHT - 3);

        this.width = width;
        this.height = totalHeight;

        RenderUtil.drawRoundedRect(matrices, startX, startY, width, totalHeight, new Vector4f(8, 8, 8, 8), colorstandart);
        sfpro.drawGradientText(matrices, "Staffs", startX + PADDING + 13, startY + PADDING - 3, colorfonts1, colorfonts2);
        iconsFont.drawFontLeft(matrices, icons[8], startX + 5, startY + 3, new Color(45, 125, 255).getRGB());
        RenderUtil.drawRoundedRect(matrices, startX, startY + 14, width, 0.9f, new Vector4f(0f, 0f, 0f, 0f), new Color(60, 60, 70).getRGB());

        int currentY = (int) (startY + PADDING + titleHeight);
        for (StaffInfo staff : staffList) {
            renderStaffEntry(matrices, startX, currentY, width, staff, sfpro1, sfpro2, prefixFont, colorfonts1, colorfonts2);
            currentY += ITEM_HEIGHT;
        }
    }

    // ... остальной код остаётся без изменений ...

    private void renderStaffEntry(MatrixStack matrices, float startX, float currentY, float width, StaffInfo staff, FontDraw nameFont, FontDraw timeFont, FontDraw prefixFont, int colorfonts1, int colorfonts2) {
        boolean isVanished = staff.isVanished();
        int nameAlpha = isVanished ? 255 : 150;
        int color1 = new Color(255, 255, 255, nameAlpha).getRGB();
        int color2 = new Color(150, 150, 160, nameAlpha).getRGB();

        float headSize = 8f;
        float headX = startX + PADDING;
        float headY = currentY - 0.8f;

        PlayerListEntry playerEntry = getPlayerEntry(staff.getName());
        if (playerEntry != null && playerEntry.getSkinTextures() != null) {
            Identifier skinTexture = playerEntry.getSkinTextures().texture();
            RenderUtil.drawRoundedImage(matrices, skinTexture, headX, headY, headSize, headSize, 0.125f, 0.126f, 0.25f, 0.26f, new Vector4f(3f, 3f, 3f, 3f), 0xFFFFFFFF);
        }

        float squareX = headX + headSize + 3f;
        float squareY = currentY + 1.5f;
        RenderUtil.drawRoundedRect(matrices, squareX, squareY, 4, 4, new Vector4f(1, 1, 1, 1), new Color(45, 125, 255).getRGB());

        float nameX = squareX + 4 + 3f;

        String timeText = staff.getOnlineTime();
        float timeWidth = timeFont.getWidth(timeText);
        float timeX = startX + width - timeWidth - PADDING;

        String prefix = staff.getPrefix();
        float prefixWidth = prefixFont.getWidth(prefix);

        float maxNameWidth = timeX - nameX - 2 - prefixWidth - 2;
        float nameWidth = nameFont.getWidth(staff.getName());

        boolean needsClipping = nameWidth > maxNameWidth;
        float actualNameWidth = needsClipping ? maxNameWidth : nameWidth;

        if (needsClipping) {
            ScissorUtil.push();
            ScissorUtil.setFromComponentCoordinates(nameX, currentY - 1.5f, actualNameWidth, nameFont.getHeight());
        }

        nameFont.drawGradientText(matrices, staff.getName(), nameX, currentY - 1.5f, color1, color2);

        if (needsClipping) {
            ScissorUtil.pop();
        }

        float prefixX = nameX + actualNameWidth + 2;
        float prefixY = currentY - 1.5f;

        prefixFont.drawFontLeft(matrices, prefix, prefixX, prefixY, colorfonts2);
        timeFont.drawFontLeft(matrices, timeText, timeX, currentY - 1, colorfonts2);
    }

    private PlayerListEntry getPlayerEntry(String playerName) {
        if (mc.getNetworkHandler() == null) return null;

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            GameProfile profile = entry.getProfile();
            if (profile != null && profile.getName().equals(playerName)) {
                return entry;
            }
        }
        return null;
    }

    private void updateStaffList() {
        if (mc.getNetworkHandler() == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) return;

        lastUpdate = currentTime;

        Set<String> currentKeys = new HashSet<>();

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            GameProfile profile = entry.getProfile();
            Text displayName = entry.getDisplayName();

            if (displayName == null || profile == null) continue;

            String display = displayName.getString();
            String name = profile.getName();

            String fullPrefix = display.replace(name, "").trim();

            if (fullPrefix.length() < 2) continue;
            if (!containsStaffKeyword(fullPrefix)) continue;

            String donatePrefix = extractDonatePrefix(fullPrefix);

            String key = display;

            StaffStatus status = (entry.getGameMode() == GameMode.SPECTATOR) ? StaffStatus.VANISHED : StaffStatus.ONLINE;

            staffCache.computeIfAbsent(key, k ->
                    new StaffInfo(displayName, name, donatePrefix, status, currentTime));

            currentKeys.add(key);
        }

        staffCache.entrySet().removeIf(entry -> !currentKeys.contains(entry.getKey()));
    }

    private String extractDonatePrefix(String fullPrefix) {
        if (fullPrefix == null || fullPrefix.isEmpty()) return "";

        String cleaned = repairString(fullPrefix).trim();
        cleaned = cleaned.replaceAll("^[^a-zA-Zа-яА-ЯёЁ0-9]+", "").trim();

        if (cleaned.isEmpty()) return "";

        String[] words = cleaned.split("\\s+");

        List<String> validWords = new ArrayList<>();
        for (String word : words) {
            if (word.isEmpty() || word.length() <= 1) continue;
            if (word.matches(".*[a-zA-Zа-яА-ЯёЁ].*")) {
                validWords.add(word);
            }
        }

        if (validWords.isEmpty()) return "";
        for (int i = 0; i < validWords.size(); i++) {
            String word = validWords.get(i);
            String lower = word.toLowerCase(Locale.US);

            boolean containsStaffKeyword = false;
            for (String keyword : STAFF_PREFIXES) {
                if (lower.contains(keyword)) {
                    containsStaffKeyword = true;
                    break;
                }
            }

            if (containsStaffKeyword) {
                if (i > 0) {
                    return validWords.get(i - 1);
                }
                return "";
            }
        }

        return validWords.get(0);
    }

    private String repairString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 65281 && c <= 65374) {
                sb.append((char) (c - 65248));
            } else if (c < 32 || (c >= 127 && c < 160)) {
                continue;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean containsStaffKeyword(String text) {
        if (text == null || text.isEmpty()) return false;

        String lower = text.toLowerCase(Locale.US);
        for (String keyword : STAFF_PREFIXES) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static class StaffInfo {
        private final Text displayName;
        private final String name;
        private final String prefix;
        private final StaffStatus status;
        private final long joinTime;

        public StaffInfo(Text displayName, String name, String prefix, StaffStatus status, long joinTime) {
            this.displayName = displayName;
            this.name = name;
            this.prefix = prefix;
            this.status = status;
            this.joinTime = joinTime;
        }

        public Text getDisplayName() { return displayName; }
        public String getName() { return name; }
        public String getPrefix() { return prefix; }
        public StaffStatus getStatus() { return status; }
        public long getJoinTime() { return joinTime; }

        public String getOnlineTime() {
            long ms = System.currentTimeMillis() - joinTime;
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%d:%02d", minutes, seconds);
        }

        public boolean isVanished() {
            return status == StaffStatus.VANISHED;
        }
    }

    public enum StaffStatus {
        ONLINE,
        VANISHED
    }
}