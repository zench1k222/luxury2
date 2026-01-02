package dev.luxury.modules.impl.other.taksa;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TaksaAI {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    @Setter
    private static String lastMsg;

    public String args() {
        List<String> args = new ArrayList<>();
        
        if (mc.player == null) return "";
        
        if (mc.player.isSneaking())
            args.add("Игрок на шифте");
        
        if (mc.player.getVelocity().horizontalLength() > 0.1)
            args.add("Игрок двигается");
        else
            args.add("Игрок стоит");
        
        if (mc.player.hurtTime > 0)
            args.add("Игрок получает урон");
        
        if (mc.player.isSwimming())
            args.add("Игрок плавает");
        
        if (mc.player.isGliding())
            args.add("Игрок летит на элитрах");
        
        if (mc.player.isOnGround())
            args.add("Игрок на земле");
        
        if (mc.player.isUsingItem())
            args.add("Игрок использует " + mc.player.getActiveItem().getName().getString());
        
        if (lastMsg != null && !lastMsg.isEmpty())
            args.add("Последнее сообщение от хозяина: " + lastMsg);
        
        return String.join("\n", args);
    }
    
    public void update() {
        TaksaScheduler.getUpdateTimer().reset2();

        String context = args();
        if (!context.isEmpty()) {
            // ChatUtil.sendMessage("Такса", "Гав!");
        }
        lastMsg = "";
    }
}

