package dev.luxury.utils.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class GifUtils {
    private static final Map<String, GifAnimation> animations = new HashMap<>();
    private static final long DEFAULT_FRAME_DELAY = 15;
    private static final Map<String, Long> lastUsed = new HashMap<>();
    private static final long CLEANUP_INTERVAL = 60_000;
    private static final long UNUSED_THRESHOLD = 120_000;
    private static long lastCleanup = System.currentTimeMillis();
    private static String normalizeResourcePath(String path) {
        return path.toLowerCase()
                .replaceAll("[^a-z0-9/._-]", "_");
    }

    private static class GifAnimation {
        protected List<Identifier> frames;
        protected int currentFrame;
        protected long lastFrameTime;
        protected long frameDelay;

        public GifAnimation(String resourceLocation, int numImages, long frameDelay) {
            this.frames = new ArrayList<>();
            this.currentFrame = 0;
            this.lastFrameTime = System.currentTimeMillis();
            this.frameDelay = frameDelay;

            String normalizedPath = normalizeResourcePath(resourceLocation);

            for (int i = 0; i < numImages; i++) {
                try {
                    Identifier frameId = Identifier.of("luxury", normalizedPath + i + ".png");
                    frames.add(frameId);
                } catch (Exception ignored) {
                }
            }
        }

        public Identifier getCurrentFrame() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime > frameDelay) {
                lastFrameTime = currentTime;
                currentFrame = (currentFrame + 1) % frames.size();
            }
            return frames.get(currentFrame);
        }

        public void reset() {
            currentFrame = 0;
            lastFrameTime = System.currentTimeMillis();
        }

        public int getCurrentFrameIndex() {
            return currentFrame;
        }

        public int getTotalFrames() {
            return frames.size();
        }
    }


    public static void renderGif(MatrixStack matrices, String resourceLocation, int x, int y, int width, int height, Vector4f rounding, int numImages, long frameDelay) {
        try {
            cleanupUnusedAnimations();

            GifAnimation animation = animations.get(resourceLocation);
            if (animation == null) {
                animation = new GifAnimation(resourceLocation, numImages, frameDelay);
                animations.put(resourceLocation, animation);
            }

            lastUsed.put(resourceLocation, System.currentTimeMillis());

            Identifier currentImage = animation.getCurrentFrame();
            RenderUtil.drawRoundedImage(matrices, currentImage, x, y, width, height, rounding, 0xFFFFFFFF);
        } catch (Exception ignored) {
        }
    }
    public static void renderGif(MatrixStack matrices, String resourceLocation, int x, int y, int width, int height, Vector4f rounding, int numImages) {
        renderGif(matrices, resourceLocation, x, y, width, height, rounding, numImages, DEFAULT_FRAME_DELAY);
    }


    public static void resetAnimation(String resourceLocation) {
        GifAnimation animation = animations.get(resourceLocation);
        if (animation != null) animation.reset();
    }

    public static void resetAllAnimations() {
        for (GifAnimation animation : animations.values()) animation.reset();
    }

    public static void clearAnimation(String resourceLocation) {
        animations.remove(resourceLocation);
    }

    public static void clearAllAnimations() {
        animations.clear();
    }

    public static int getCurrentFrame(String resourceLocation) {
        GifAnimation animation = animations.get(resourceLocation);
        return animation != null ? animation.getCurrentFrameIndex() : -1;
    }

    public static int getTotalFrames(String resourceLocation) {
        GifAnimation animation = animations.get(resourceLocation);
        return animation != null ? animation.getTotalFrames() : 0;
    }
    public static void cleanupAll() {
        animations.clear();
        lastUsed.clear();
    }
    public static void renderGifWithFormat(MatrixStack matrices, String basePath, String format, int x, int y, int width, int height, int numImages, long frameDelay) {
        String cacheKey = basePath + "_" + format + "_" + numImages;
        try {
            cleanupUnusedAnimations();

            GifAnimation animation = animations.get(cacheKey);
            if (animation == null) {
                animation = new GifAnimationWithFormat(basePath, format, numImages, frameDelay);
                animations.put(cacheKey, animation);
            }

            lastUsed.put(cacheKey, System.currentTimeMillis());

            Identifier currentImage = animation.getCurrentFrame();
            RenderUtil.drawImage(matrices, currentImage, x, y, width, height);
        } catch (Exception ignored) {
        }
    }
    private static void cleanupUnusedAnimations() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            lastCleanup = currentTime;

            animations.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                long lastUse = lastUsed.getOrDefault(key, 0L);
                boolean shouldRemove = (currentTime - lastUse) > UNUSED_THRESHOLD;
                if (shouldRemove) {
                    lastUsed.remove(key);
                }
                return shouldRemove;
            });

            lastUsed.entrySet().removeIf(entry -> !animations.containsKey(entry.getKey()));
        }
    }
    private static class GifAnimationWithFormat extends GifAnimation {
        public GifAnimationWithFormat(String basePath, String format, int numImages, long frameDelay) {
            super("", 0, frameDelay);
            this.frames = new ArrayList<>();
            this.currentFrame = 0;
            this.lastFrameTime = System.currentTimeMillis();
            this.frameDelay = frameDelay;

            for (int i = 0; i < numImages; i++) {
                try {
                    Identifier frameId = Identifier.of("luxury", String.format(format, basePath, i));
                    frames.add(frameId);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
