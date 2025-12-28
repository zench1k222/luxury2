package dev.luxury.utils.commands;

import net.minecraft.client.MinecraftClient;
import ru.nexusguard.protection.annotations.Native;

public abstract class Command {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String name;
    private final String description;
    private final String usage;

    public Command(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    @Native
    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }
}