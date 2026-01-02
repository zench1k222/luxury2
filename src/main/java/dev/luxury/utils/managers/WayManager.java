package dev.luxury.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.luxury.common.way.Way;
import dev.luxury.common.way.WayRepository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import static dev.luxury.modules.impl.other.targetesp.mode.Circle.mc;

public class WayManager {
    private static WayManager instance;
    private final WayRepository wayRepository = WayRepository.getInstance();
    private final File configsFolder;

    private WayManager() {
        this.configsFolder = new File(mc.runDirectory, "config/luxury/ways");
        if (!configsFolder.exists()) {
            configsFolder.mkdirs();
        }
    }

    public static WayManager getInstance() {
        if (instance == null) {
            instance = new WayManager();
        }
        return instance;
    }

    public void saveWays(String configName) {
        try {
            File configFile = new File(configsFolder, configName + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(wayRepository.wayList, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWays(String configName) {
        try {
            File configFile = new File(configsFolder, configName + ".json");

            if (!configFile.exists()) {
                return;
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Way>>() {}.getType();

            try (FileReader reader = new FileReader(configFile)) {
                List<Way> ways = gson.fromJson(reader, listType);
                wayRepository.wayList.clear();
                wayRepository.wayList.addAll(ways);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openWaysFolder() {
        try {
            java.awt.Desktop.getDesktop().open(configsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}