package dev.luxury.ui;

import dev.luxury.Luxury;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.utils.client.ChatUtil;
import dev.luxury.utils.notifications.Notification;
import dev.luxury.utils.notifications.NotificationsManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

@ModuleAnnotation(
        name = "ClickGui",
        desc = "Tabbed GUI with modern design",
        category = Category.Render
)
public class Clickgui extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting mode = new ModeSetting("Режим ГУИ", "2.0", new String[]{"1.0", "2.0", "3.0"});

    public Clickgui() {
        this.setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mode.is("3.0")) {
            NotificationsManager.getInstance().info("3 версия еще не доделана :(", 3000);
        }

        if (mode.is("1.0")) {
            if (mc.currentScreen == null) {
                mc.setScreen(new Csgui(Luxury.getInstance().getModuleManager()));
            }
        } if (mode.is("2.0")) {
            if (mc.currentScreen == null) {
                mc.setScreen(new TabbedGUI(Luxury.getInstance().getModuleManager()));
            }
        } if (mode.is("3.0")) {
            if (mc.currentScreen == null) {
                mc.setScreen(new DropDownGUI(Luxury.getInstance().getModuleManager()));
            }
        }


        disable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}