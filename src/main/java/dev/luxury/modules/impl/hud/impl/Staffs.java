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
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.joml.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Staffs extends DraggableHudElement {
    private static final int PADDING = 5;
    private static final int ITEM_HEIGHT = 12;

    private static final Map<String, String> SYMBOL_TO_TITLE = Map.of(
            "ꕠ", "D.HELPER",
            "ꔉ", "HELPER",
            "ꔓ", "ML.MODER",
            "ꔗ", "MODER",
            "ꔡ", "MODER+",
            "ꔥ", "ST.MODER",
            "ꔩ", "GL.MODER",
            "ꔳ", "ML.ADMIN",
            "ꔷ", "ADMIN"
    );

    private static final Set<String> STAFF_SYMBOLS = SYMBOL_TO_TITLE.keySet();

    private static final Set<String> STAFF_KEYWORDS = Set.of(
            "helper", "ᴀдмин", "moder", "staff", "admin", "curator",
            "стажёр", "сотрудник", "помощник", "админ", "модер"
    );

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            ".*(mod|der|adm|help|wne|хелп|адм|поддержка|влад|tik|тик|таф|кура|own|taf|curat|dev|supp|yt|сотруд|ꔓ|ꔗ|ꔡ|ꔥ|ꔩ|ꔳ|ꔷ).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

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

        int colorstandart = new Color(115, 115, 120, 255).getRGB();
        int colorfonts1 = Color.WHITE.getRGB();
        int colorfonts2 = new Color(150, 150, 160).getRGB();

        List<StaffInfo> staffList = new ArrayList<>(staffCache.values());

        if (staffList.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        float maxWidth = 97.5f;
        for (StaffInfo staff : staffList) {
            float nameWidth = sfpro1.getWidth(staff.getName());
            float prefixWidth = prefixFont.getWidth(staff.getPrefix());
            float statusWidth = sfpro2.getWidth(staff.getStatus().toString());
            float headSize = 8f;

            float totalWidth = PADDING + headSize + 3 + nameWidth + 2 + prefixWidth + 5 + statusWidth + PADDING;
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

        RenderUtil.drawBlur(matrices, startX, startY, width, totalHeight,
                new Vector4f(8, 8, 8, 8), 18f, colorstandart);
        sfpro.drawGradientText(matrices, "Staffs", startX + PADDING + 13,
                startY + PADDING - 3, colorfonts1, colorfonts2);
        iconsFont.drawFontLeft(matrices, icons[8], startX + 5, startY + 3,
                new Color(45, 125, 255).getRGB());
        RenderUtil.drawBlur(matrices, startX, startY + 14, width, 0.9f,
                new Vector4f(0f, 0f, 0f, 0f), 18f, new Color(60, 60, 70).getRGB());

        int currentY = (int) (startY + PADDING + titleHeight);
        for (StaffInfo staff : staffList) {
            renderStaffEntry(matrices, startX, currentY, width, staff,
                    sfpro1, sfpro2, prefixFont, colorfonts1, colorfonts2);
            currentY += ITEM_HEIGHT;
        }
    }

    private void renderStaffEntry(MatrixStack matrices, float startX, float currentY,
                                  float width, StaffInfo staff, FontDraw nameFont, FontDraw statusFont,
                                  FontDraw prefixFont, int colorfonts1, int colorfonts2) {

        boolean isVanished = staff.isVanished();

        int nameAlpha = isVanished ? 150 : 255;
        int color1 = new Color(255, 255, 255, nameAlpha).getRGB();
        int color2 = new Color(150, 150, 160, nameAlpha).getRGB();

        float headSize = 8f;
        float headX = startX + PADDING;
        float headY = currentY - 0.8f;

        PlayerListEntry playerEntry = getPlayerEntry(staff.getName());
        if (playerEntry != null && playerEntry.getSkinTextures() != null) {
            Identifier skinTexture = playerEntry.getSkinTextures().texture();
            int headAlpha = isVanished ? 150 : 255; // Тусклее для ванишедших
            RenderUtil.drawRoundedImage(matrices, skinTexture, headX, headY,
                    headSize, headSize, 0.125f, 0.126f, 0.25f, 0.26f,
                    new Vector4f(3f, 3f, 3f, 3f),
                    (headAlpha << 24) | 0xFFFFFF);
        }

        float squareX = headX + headSize + 3f;
        float squareY = currentY + 1.5f;
        int squareColor = isVanished ? Color.RED.getRGB() : new Color(45, 125, 255).getRGB();
        RenderUtil.drawBlur(matrices, squareX, squareY, 4, 4,
                new Vector4f(1, 1, 1, 1), 18f, squareColor);

        float nameX = squareX + 4 + 3f;

        String statusText = staff.getStatus().toString();
        float statusWidth = statusFont.getWidth(statusText);
        float statusX = startX + width - statusWidth - PADDING;

        String prefix = staff.getPrefix();
        float prefixWidth = prefixFont.getWidth(prefix);

        float maxNameWidth = statusX - nameX - 2 - prefixWidth - 2;
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

        int prefixColor = isVanished ?
                new Color(200, 150, 150).getRGB() :
                colorfonts2;

        prefixFont.drawFontLeft(matrices, prefix, prefixX, prefixY, prefixColor);

        int statusColor = isVanished ?
                new Color(255, 100, 100).getRGB() :
                new Color(100, 255, 100).getRGB();

        statusFont.drawFontLeft(matrices, statusText, statusX, currentY - 1, statusColor);
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
        if (mc.getNetworkHandler() == null || mc.world == null || mc.world.getScoreboard() == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) return;
        lastUpdate = currentTime;

        Set<String> currentKeys = new HashSet<>();

        System.out.println("=== Staff Detection Start ===");

        for (Team team : mc.world.getScoreboard().getTeams()) {
            String rawPrefix = team.getPrefix().getString();
            String cleanPrefix = cleanPrefix(rawPrefix);

            System.out.println("Team: " + team.getName() +
                    " | Raw: '" + rawPrefix + "'" +
                    " | Clean: '" + cleanPrefix + "'" +
                    " | Players: " + team.getPlayerList());

            boolean isStaff = isStaffBySymbol(cleanPrefix);

            if (!isStaff) {
                isStaff = isStaffByKeyword(cleanPrefix);
            }

            System.out.println("  -> IsStaff: " + isStaff +
                    " | Symbol: " + isStaffBySymbol(cleanPrefix) +
                    " | Keyword: " + isStaffByKeyword(cleanPrefix));

            if (!isStaff) {
                continue;
            }

            System.out.println("  ^ Adding staff players...");

            for (String playerName : team.getPlayerList()) {
                if (playerName.equals(mc.player.getName().getString())) {
                    continue;
                }

                PlayerListEntry playerEntry = getPlayerEntry(playerName);
                StaffStatus status;

                if (playerEntry == null) {
                    status = StaffStatus.VANISHED; // Игрок не в табах = ваниш
                } else if (playerEntry.getGameMode() == GameMode.SPECTATOR) {
                    status = StaffStatus.VANISHED; // В спектаторах = ваниш
                } else {
                    status = StaffStatus.ONLINE;
                }

                String staffSymbol = extractStaffSymbol(cleanPrefix);
                String displayPrefix = SYMBOL_TO_TITLE.getOrDefault(staffSymbol,
                        extractStaffTitle(cleanPrefix));

                staffCache.put(playerName,
                        new StaffInfo(
                                Text.of(rawPrefix + playerName),
                                playerName,
                                displayPrefix,
                                status,
                                currentTime
                        )
                );
                currentKeys.add(playerName);

                System.out.println("    - " + playerName + " | Status: " + status +
                        " | Prefix: " + displayPrefix);
            }
        }

        staffCache.entrySet().removeIf(entry -> !currentKeys.contains(entry.getKey()));

        System.out.println("=== Staff Detection End. Total: " + staffCache.size() + " ===");
    }

    private String cleanPrefix(String prefix) {
        if (prefix == null) return "";
        return prefix.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private boolean isStaffBySymbol(String prefix) {
        if (prefix == null || prefix.isEmpty()) return false;

        for (String symbol : STAFF_SYMBOLS) {
            if (prefix.contains(symbol)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStaffByKeyword(String prefix) {
        if (prefix == null || prefix.isEmpty()) return false;

        String lowerPrefix = prefix.toLowerCase();

        if (PREFIX_PATTERN.matcher(lowerPrefix).matches()) {
            return true;
        }

        String[] words = lowerPrefix.split("[^a-zа-я0-9]+");
        for (String word : words) {
            if (word.length() < 3) continue;

            for (String keyword : STAFF_KEYWORDS) {
                if (word.equals(keyword) || word.startsWith(keyword) || word.endsWith(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractStaffSymbol(String prefix) {
        if (prefix == null) return "";

        for (String symbol : STAFF_SYMBOLS) {
            if (prefix.contains(symbol)) {
                return symbol;
            }
        }
        return "";
    }

    private String extractStaffTitle(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "STAFF";

        String lowerPrefix = prefix.toLowerCase();
        for (String keyword : STAFF_KEYWORDS) {
            if (lowerPrefix.contains(keyword)) {
                return keyword.toUpperCase();
            }
        }

        if (PREFIX_PATTERN.matcher(lowerPrefix).matches()) {
            return "STAFF";
        }

        return "UNKNOWN";
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

        public boolean isVanished() {
            return status == StaffStatus.VANISHED;
        }
    }

    public enum StaffStatus {
        ONLINE,
        VANISHED;

        @Override
        public String toString() {
            return this.name();
        }
    }
}