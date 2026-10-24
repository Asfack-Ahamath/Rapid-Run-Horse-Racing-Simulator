package org.example.horse_racing.ui.theme;

import javafx.scene.paint.Color;
import org.example.horse_racing.domain.HorseGroup;

/**
 * Colours for code-drawn graphics (the race track canvas). Mirrors the tokens in {@code app.css}.
 */
public record Palette(
        boolean dark,
        Color surface,
        Color text,
        Color textMuted,
        Color border,
        Color turf,
        Color turfStripe,
        Color rail,
        Color laneLine,
        Color accent,
        Color[] groups
) {

    private static final Palette LIGHT = new Palette(false,
            Color.web("#FFFFFF"), Color.web("#1B1D1A"), Color.web("#6B6A63"), Color.web("#E4DFD4"),
            Color.web("#2F7A4C"), Color.web("#2A7045"), Color.web("#F5F1E6"), Color.web("#FFFFFF", 0.28),
            Color.web("#B8862B"),
            new Color[]{Color.web("#D99A1E"), Color.web("#3B7BEA"), Color.web("#E0525A"), Color.web("#12A594")});

    private static final Palette DARK = new Palette(true,
            Color.web("#151C19"), Color.web("#E8EEEA"), Color.web("#9BA8A1"), Color.web("#26312C"),
            Color.web("#1C4A30"), Color.web("#1A432C"), Color.web("#C9C3B3"), Color.web("#FFFFFF", 0.16),
            Color.web("#E3B656"),
            new Color[]{Color.web("#F0B43C"), Color.web("#5B95F5"), Color.web("#F06B72"), Color.web("#2CC7B3")});

    public static Palette of(boolean dark) {
        return dark ? DARK : LIGHT;
    }

    public Color group(HorseGroup g) {
        return groups[g.ordinal()];
    }

    public Color group(int lane) {
        return groups[Math.floorMod(lane, groups.length)];
    }
}
