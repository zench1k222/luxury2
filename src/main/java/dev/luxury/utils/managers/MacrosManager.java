package dev.luxury.utils.managers;

import dev.luxury.events.impl.client.EventKeyInput;
import dev.luxury.events.impl.eventapi.EventTarget;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class MacrosManager {
    private static MacrosManager instance;
    private final List<Macro> macros = new ArrayList<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private MacrosManager() {}

    public static MacrosManager getInstance() {
        if (instance == null) {
            instance = new MacrosManager();
        }
        return instance;
    }

    public void addMacro(String name, String message, int key) {
        for (Macro macro : macros) {
            if (macro.getName().equalsIgnoreCase(name)) {
                macro.setMessage(message);
                macro.setKey(key);
                return;
            }
        }
        macros.add(new Macro(name, message, key));
    }

    public boolean removeMacro(String name) {
        return macros.removeIf(macro -> macro.getName().equalsIgnoreCase(name));
    }

    public void clearMacros() {
        macros.clear();
    }

    public List<Macro> getMacros() {
        return new ArrayList<>(macros);
    }

    public Macro getMacroByName(String name) {
        for (Macro macro : macros) {
            if (macro.getName().equalsIgnoreCase(name)) {
                return macro;
            }
        }
        return null;
    }

    @EventTarget
    public void onKey(EventKeyInput e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (e.getAction() == 1) {
            for (Macro macro : macros) {
                if (macro.getKey() == e.getKey()) {
                    executeMacro(macro);
                }
            }
        }
    }

    private void executeMacro(Macro macro) {
        String message = macro.getMessage();

        if (message.startsWith("/")) {
            String command = message.substring(1);
            mc.player.networkHandler.sendChatCommand(command);
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }

    public void setMacros(List<Macro> macros) {
        this.macros.clear();
        this.macros.addAll(macros);
    }
}