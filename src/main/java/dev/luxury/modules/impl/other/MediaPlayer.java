package dev.luxury.modules.impl.other;

import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.utils.font.FontDraw;
import dev.luxury.utils.font.FontHelper;
import dev.redstones.mediaplayerinfo.MediaInfo;

import java.awt.*;

public class MediaPlayer {
    String artist;
    String title;
    byte atrworkPng;
    long position;
    long duration;
    boolean playing;
    public void render(EventRender2D e){
        FontDraw montserratMedium = FontHelper.monsterrat[16];
        MediaInfo mediaInfo = new MediaInfo(title,artist, new byte[]{atrworkPng},position,duration,playing);
        montserratMedium.drawFontLeft(e.getDrawContext().getMatrices(),mediaInfo.getArtist(),80,80, Color.white.getRGB());
    }
}
