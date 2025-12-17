package dev.luxury.utils.managers;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private static final List<Runnable> tasks = new ArrayList<>();

    public static void runTask(Runnable task) {
        tasks.add(task);
    }

    public static void onTick() {
        if (!tasks.isEmpty()) {
            for (Runnable task : tasks) {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tasks.clear();
        }
    }
}