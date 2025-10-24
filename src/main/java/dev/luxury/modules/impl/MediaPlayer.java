package dev.luxury.modules.impl;

import dev.luxury.events.impl.render.EventRender2D;
import dev.luxury.utils.font.FontDraw;
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
        MediaInfo mediaInfo = new MediaInfo(title,artist, new byte[]{atrworkPng},position,duration,playing);
        FontDraw.Montserrat_Medium.drawString(e.getDrawContext().getMatrices(),mediaInfo.getArtist(),80,80, Color.white.getRGB());
    }
}
