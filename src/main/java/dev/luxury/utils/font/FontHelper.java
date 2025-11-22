package dev.luxury.utils.font;

import java.awt.Font;
import java.util.Objects;

@SuppressWarnings("All")
public class FontHelper {

    public final String fontsDir = "/assets/luxury/fonts/";


    public static volatile FontDraw[] icons = new FontDraw[256];
    public static volatile FontDraw[] monsterrat = new FontDraw[256];
    public static volatile FontDraw[] sfprobold = new FontDraw[256];

    private boolean initialized = false;

    public synchronized void initialize() {
        if (initialized) return;

        initializationFont(icons, "icons.ttf");
        initializationFont(monsterrat, "Montserrat_Medium.ttf");
        initializationFont(sfprobold,"SF-Pro-Display-Bold.ttf");

        initialized = true;
    }

    private void initializationFont(FontDraw[] fontArray, String fontName) {
        if (fontArray == null) return;
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(FontHelper.class.getResourceAsStream(fontsDir + fontName)));
            for (int i = 1; i < fontArray.length; i++) {
                fontArray[i] = new FontDraw(font, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
