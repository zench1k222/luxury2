package dev.luxury.utils.font;

import dev.luxury.Luxury;
import org.jetbrains.annotations.NotNull;


import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class FontDraw {

    public static FontHelper minecraft;
    public static FontHelper sf_medium;
    public static FontHelper Montserrat_Medium;
    public static FontHelper Montserrat_Big;
    public static FontHelper icons;

    public static @NotNull FontHelper create(float size, String name) throws IOException, FontFormatException {
        return new FontHelper(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Luxury.class.getClassLoader().getResourceAsStream("assets/luxury/fonts/" + name + ".ttf"))).deriveFont(Font.PLAIN, size / 2f), size / 2f);
    }
}
