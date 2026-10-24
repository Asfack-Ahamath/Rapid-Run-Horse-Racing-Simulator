package org.example.horse_racing.ui.components;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Bundled image loading and recolouring (the horse silhouette is drawn in any colour). */
public final class Images {

    public static final String HORSE = "/org/example/horse_racing/assets/horse-icon.png";

    private static final Map<String, Image> CACHE = new HashMap<>();

    private Images() {}

    public static Image resource(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = Images.class.getResourceAsStream(p)) {
                return in == null ? null : new Image(in);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Returns the silhouette recoloured to {@code color}. Dark pixels become opaque {@code color},
     * light/transparent pixels become transparent, so it works whether or not the source has alpha.
     */
    public static Image tinted(String path, Color color) {
        String key = path + "#" + color;
        Image cached = CACHE.get(key);
        if (cached != null) return cached;
        Image src = resource(path);
        if (src == null) return null;
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        WritableImage out = new WritableImage(w, h);
        PixelReader r = src.getPixelReader();
        PixelWriter pw = out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = r.getColor(x, y);
                double ink = c.getOpacity() * (1.0 - c.getBrightness());
                pw.setColor(x, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(ink * color.getOpacity())));
            }
        }
        CACHE.put(key, out);
        return out;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
