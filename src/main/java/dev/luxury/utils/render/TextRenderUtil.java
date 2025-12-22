package dev.luxury.utils.render;


import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TextRenderUtil implements Event {
    String text;

    public void replaceText(String protect, String replaced) {
        if (text == null || text.isEmpty()) return;

        if (text.contains(protect)) {
            if (text.equalsIgnoreCase(protect) || text.contains(protect + " ") || text.contains(" " + protect) || text.contains("⏏" + protect) || text.contains(protect + "§")) {
                text = text.replace(protect, replaced);
            }
        }
    }
}
