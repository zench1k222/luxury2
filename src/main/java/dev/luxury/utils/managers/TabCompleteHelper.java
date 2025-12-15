package dev.luxury.utils.managers;

import dev.luxury.utils.commands.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TabCompleteHelper {
    private final List<String> completions = new ArrayList<>();

    public TabCompleteHelper addCommands(List<Command> commands) {
        commands.forEach(c -> completions.add(c.getName()));
        return this;
    }

    public TabCompleteHelper filterPrefix(String prefix) {
        List<String> filtered = new ArrayList<>();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(s);
            }
        }
        completions.clear();
        completions.addAll(filtered);
        return this;
    }

    public Stream<String> stream() {
        return completions.stream();
    }
}
