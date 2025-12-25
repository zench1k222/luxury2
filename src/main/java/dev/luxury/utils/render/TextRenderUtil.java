package dev.luxury.utils.render;

import dev.luxury.events.impl.eventapi.events.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

@Setter
@Getter
public class TextRenderUtil implements Event {
    private Text originalText;
    private MutableText modifiedText;

    public TextRenderUtil(Text text) {
        this.originalText = text;
        this.modifiedText = text.copy();
    }

    public String getText() {
        return modifiedText.getString();
    }

    public void setText(String text) {
        this.modifiedText = Text.literal(text).setStyle(originalText.getStyle());
    }

    public void replaceText(String search, String replacement) {
        if (modifiedText != null && search != null && replacement != null) {
            String current = modifiedText.getString();
            if (current.contains(search)) {
                String newText = current.replace(search, replacement);
                modifiedText = Text.literal(newText).setStyle(originalText.getStyle());
            }
        }
    }
}