package dev.luxury.utils.commands;

import dev.luxury.utils.managers.RCTManager;
import net.minecraft.scoreboard.ScoreboardObjective;
import ru.nexusguard.protection.annotations.Native;

public class RCTCommand extends Command {
    public RCTCommand(){
        super("rct", "Перезаход на сервер.", ".rct");
    }

     
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            try {
                new RCTManager().run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
