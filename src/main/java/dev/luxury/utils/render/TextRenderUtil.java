package dev.luxury.utils.render;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TextRenderUtil implements Event {
    String text;

    public TextRenderUtil(String text) {
        this.text = text;
    }

    public void replaceText(String protect, String replaced) {
        if (text != null && protect != null && replaced != null) {
            text = text.replace(protect, replaced);
        }
    }
}