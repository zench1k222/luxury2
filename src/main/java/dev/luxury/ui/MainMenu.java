package dev.luxury.ui;

import dev.luxury.Luxury;
import dev.luxury.modules.impl.DiscordRPC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("luxury", "textures/menu_background.png");
    private static final Identifier LOGO_TEXTURE = Identifier.of("luxury", "textures/logo.png");

    private final List<ChangeLogEntry> changeLog = new ArrayList<>();
    private float scrollOffset = 0;
    private boolean draggingScroll = false;
    private double lastMouseY = 0;
    private float backgroundAlpha = 0.9f;

    public MainMenu() {
        super(Text.literal("Luxury Client"));
        initializeChangeLog();
    }

    private void initializeChangeLog() {
        changeLog.clear();

        // Здесь добавляй свои изменения
        changeLog.add(new ChangeLogEntry("v0.7", "19.12.2024",
                List.of("Добавлен модуль AutoAccept", "Исправлен NameProtect", "Добавлен кастомный MainMenu")));

        changeLog.add(new ChangeLogEntry("v0.6", "18.12.2024",
                List.of("Добавлен модуль Spider", "Исправлены баги с миксинами", "Улучшена система настроек")));

        changeLog.add(new ChangeLogEntry("v0.5", "17.12.2024",
                List.of("Базовые модули", "Система событий", "Начальная настройка клиента")));

        changeLog.add(new ChangeLogEntry("v0.4", "16.12.2024",
                List.of("Создание проекта", "Базовые миксины", "Структура кода")));

    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int centerY = height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Singleplayer"), button -> {
            if (client != null) {
                client.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(this));
            }
        }).dimensions(centerX - 100, centerY - 40, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Multiplayer"), button -> {
            if (client != null) {
                client.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(this));
            }
        }).dimensions(centerX - 100, centerY - 15, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Options"), button -> {
            if (client != null) {
                client.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(this, client.options));
            }
        }).dimensions(centerX - 100, centerY + 10, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Quit Game"), button -> {
            if (client != null) {
                client.scheduleStop();
            }
        }).dimensions(centerX - 100, centerY + 35, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        renderChangeLog(context, mouseX, mouseY);

        renderLogo(context);

        super.render(context, mouseX, mouseY, delta);

        renderVersionInfo(context);
    }

    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xFF000000);

        context.drawTexture(
                RenderLayer::getGuiTextured,
                BACKGROUND_TEXTURE,
                0, 0,
                0, 0,
                width, height,
                width, height,
                1024, 1024,
                -1
        );

        context.fill(RenderLayer.getGui(), 0, 0, width, height,
                (int)(backgroundAlpha * 255) << 24 | 0x101010);
    }

    private void renderLogo(DrawContext context) {
        String title = "§l§6Luxury§f§l Client";
        int titleWidth = textRenderer.getWidth(title);
        int titleX = width / 2 - titleWidth / 2;
        int titleY = height / 4 - 20;

        context.drawTextWithShadow(textRenderer, title, titleX + 2, titleY + 2, 0x80000000);
        context.drawTextWithShadow(textRenderer, title, titleX, titleY, 0xFFFFFF);

        String subtitle = "Premium Minecraft Utility Mod";
        int subtitleWidth = textRenderer.getWidth(subtitle);
        context.drawTextWithShadow(textRenderer, subtitle,
                width / 2 - subtitleWidth / 2, titleY + 15, 0xAAAAAA);
    }

    private void renderChangeLog(DrawContext context, int mouseX, int mouseY) {
        int changelogWidth = Math.min(300, width / 3);
        int changelogHeight = Math.min(400, height / 2);

        int changelogX = 10;
        int changelogY = 10;

        if (changelogX + changelogWidth > width) {
            changelogWidth = width - changelogX - 10;
        }
        if (changelogY + changelogHeight > height) {
            changelogHeight = height - changelogY - 10;
        }

        context.fill(RenderLayer.getGui(), changelogX, changelogY,
                changelogX + changelogWidth, changelogY + changelogHeight, 0xAA000000);

        drawBorder(context, changelogX, changelogY, changelogWidth, changelogHeight, 0xFF6B238E);

        String changelogTitle = "§6§lChangeLog";
        int titleWidth = textRenderer.getWidth(changelogTitle);
        context.drawTextWithShadow(textRenderer, changelogTitle,
                changelogX + (changelogWidth - titleWidth) / 2, changelogY + 5, 0xFFFFFF);

        int contentHeight = changeLog.size() * 80;
        int visibleHeight = changelogHeight - 40;
        int contentY = changelogY + 25;

        if (draggingScroll) {
            double deltaY = mouseY - lastMouseY;
            scrollOffset += deltaY * (contentHeight / (double) visibleHeight);
            lastMouseY = mouseY;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, contentHeight - visibleHeight)));

        boolean isMouseInChangelog = mouseX >= changelogX && mouseX <= changelogX + changelogWidth &&
                mouseY >= changelogY && mouseY <= changelogY + changelogHeight;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(changelogX + 5, contentY - scrollOffset, 0);

        int yOffset = 0;
        for (ChangeLogEntry entry : changeLog) {
            if (yOffset + 75 > 0 && yOffset < visibleHeight) {
                renderChangeLogEntry(context, entry, 0, yOffset, changelogWidth - 10);
            }
            yOffset += 80;
        }

        matrices.pop();

        if (contentHeight > visibleHeight && visibleHeight > 0) {
            int scrollbarWidth = 4;
            int scrollbarX = changelogX + changelogWidth - scrollbarWidth - 2;

            int scrollbarHeight = Math.max(20, (int) (visibleHeight * (visibleHeight / (float) contentHeight)));
            int scrollbarY = contentY + (int) (scrollOffset * (visibleHeight / (float) contentHeight));

            context.fill(RenderLayer.getGui(), scrollbarX, contentY,
                    scrollbarX + scrollbarWidth, contentY + visibleHeight, 0x44FFFFFF);

            context.fill(RenderLayer.getGui(), scrollbarX, scrollbarY,
                    scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFF6B238E);
        }

        if (isMouseInChangelog && contentHeight > visibleHeight) {
            String hint = "§7Используйте колесико мыши для прокрутки";
            int hintWidth = textRenderer.getWidth(hint);
            context.drawTextWithShadow(textRenderer, hint,
                    changelogX + (changelogWidth - hintWidth) / 2,
                    changelogY + changelogHeight - 15, 0xAAAAAA);
        }
    }

    private void renderChangeLogEntry(DrawContext context, ChangeLogEntry entry, int x, int y, int width) {
        context.fill(RenderLayer.getGui(), x, y, x + width, y + 70, 0x80222222);

        drawBorder(context, x, y, width, 70, 0x80444444);

        context.drawTextWithShadow(textRenderer, "§l§a" + entry.version, x + 5, y + 5, 0xFFFFFF);

        String dateText = "§7" + entry.date;
        int dateWidth = textRenderer.getWidth(dateText);
        context.drawTextWithShadow(textRenderer, dateText, x + width - dateWidth - 5, y + 5, 0xAAAAAA);

        int changeY = y + 25;
        int maxWidth = width - 20;

        for (String change : entry.changes) {
            String displayText = "§7• §f" + change;
            if (textRenderer.getWidth(displayText) > maxWidth) {
                displayText = textRenderer.trimToWidth(displayText, maxWidth - 20) + "...";
            }

            context.drawTextWithShadow(textRenderer, displayText, x + 10, changeY, 0xFFFFFF);
            changeY += 12;

            if (changeY > y + 65) break;
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(RenderLayer.getGui(), x, y, x + width, y + 1, color);
        context.fill(RenderLayer.getGui(), x, y + height - 1, x + width, y + height, color);
        context.fill(RenderLayer.getGui(), x, y, x + 1, y + height, color);
        context.fill(RenderLayer.getGui(), x + width - 1, y, x + width, y + height, color);
    }

    private void renderVersionInfo(DrawContext context) {
        String version = "LuxuryFree";
        context.drawTextWithShadow(textRenderer, version, 5, height, 0xAAAAAA);

        String ver = "v0.6";
        String fps = "FPS: " + MinecraftClient.getInstance().getCurrentFps();

        String userName = DiscordRPC.instance.info.userName();
        boolean isDev = userName != null &&
                ("krasivih".equals(userName) || "_znchkx_".equals(userName) || "webimmortal".equals(userName));

        String debug = isDev ? "§7DEBUG: §aON" : "§7DEBUG: §cOFF";

        int fpsWidth = textRenderer.getWidth(fps);
        int debugWidth = textRenderer.getWidth(debug);

        context.drawTextWithShadow(textRenderer, ver, width - fpsWidth - 5, height - 20, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, fps, width - fpsWidth - 5, height - 10, 0xAAAAAA);

        context.drawTextWithShadow(textRenderer, debug, 5, height - 10, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int changelogX = 10;
        int changelogY = 10;
        int changelogWidth = Math.min(300, width / 3);
        int changelogHeight = Math.min(400, height / 2);

        if (mouseX >= changelogX && mouseX <= changelogX + changelogWidth &&
                mouseY >= changelogY && mouseY <= changelogY + changelogHeight) {

            if (button == 0) {
                draggingScroll = true;
                lastMouseY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScroll = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int changelogX = 10;
        int changelogY = 10;
        int changelogWidth = Math.min(300, width / 3);
        int changelogHeight = Math.min(400, height / 2);

        if (mouseX >= changelogX && mouseX <= changelogX + changelogWidth &&
                mouseY >= changelogY && mouseY <= changelogY + changelogHeight) {

            scrollOffset -= verticalAmount * 20;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        scrollOffset = 0;
    }

    private static class ChangeLogEntry {
        String version;
        String date;
        List<String> changes;

        ChangeLogEntry(String version, String date, List<String> changes) {
            this.version = version;
            this.date = date;
            this.changes = changes;
        }
    }

    public void addChangeLogEntry(String version, String date, List<String> changes) {
        changeLog.add(0, new ChangeLogEntry(version, date, changes));
    }

    public void clearChangeLog() {
        changeLog.clear();
    }

    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = Math.max(0, Math.min(1, alpha));
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }
}