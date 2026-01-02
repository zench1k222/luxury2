package dev.luxury.modules.impl.render;

import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;

@ModuleAnnotation(
        name = "CustomModels",
        category = Category.Render
)
public class CustomModels extends Module {
    public final ModeSetting type = new ModeSetting("Тип", "Crazy Rabbit", new String[]{"Crazy Rabbit", "Freddy Bear","Red Demon","White Demon","Sonic","Chinchilla"});
    public CustomModels() {
        addSettings(type);
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
