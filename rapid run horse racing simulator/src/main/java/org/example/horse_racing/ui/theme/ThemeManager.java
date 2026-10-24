package org.example.horse_racing.ui.theme;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Scene;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Owns the application's colour theme: {@link Mode#LIGHT} or {@link Mode#DARK}.
 * On first launch it follows the OS appearance when the JavaFX runtime exposes it (JavaFX 22+);
 * after that the user's choice is remembered between launches.
 */
public final class ThemeManager {

    public enum Mode { LIGHT, DARK }

    private static final String DARK_CLASS = "theme-dark";
    private static final String PREF_KEY = "themeMode";
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.LIGHT);
    private final ReadOnlyBooleanWrapper dark = new ReadOnlyBooleanWrapper(false);
    private Scene scene;

    private ThemeManager() {
        String saved = prefs.get(PREF_KEY, null);
        if ("LIGHT".equals(saved) || "DARK".equals(saved)) {
            mode.set(Mode.valueOf(saved));
        } else {
            mode.set(systemPrefersDark() ? Mode.DARK : Mode.LIGHT);
        }
        mode.addListener((o, a, b) -> {
            prefs.put(PREF_KEY, b.name());
            recompute();
        });
    }

    public static ThemeManager get() {
        return INSTANCE;
    }

    /** Attach the stylesheet and start tracking a scene. Call once, on the FX thread. */
    public void install(Scene scene) {
        this.scene = scene;
        String css = Objects.requireNonNull(ThemeManager.class.getResource("/org/example/horse_racing/styles/app.css")).toExternalForm();
        scene.getStylesheets().setAll(css);
        recompute();
    }

    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public Mode getMode() {
        return mode.get();
    }

    public void setMode(Mode m) {
        mode.set(m);
    }

    /** Switches between Light and Dark. */
    public void toggle() {
        setMode(getMode() == Mode.LIGHT ? Mode.DARK : Mode.LIGHT);
    }

    public ReadOnlyBooleanProperty darkProperty() {
        return dark.getReadOnlyProperty();
    }

    public boolean isDark() {
        return dark.get();
    }

    public Palette palette() {
        return Palette.of(isDark());
    }

    private void recompute() {
        boolean d = mode.get() == Mode.DARK;
        dark.set(d);
        if (scene != null && scene.getRoot() != null) {
            var classes = scene.getRoot().getStyleClass();
            classes.remove(DARK_CLASS);
            if (d) classes.add(DARK_CLASS);
        }
    }

    /**
     * Reads {@code Platform.getPreferences().getColorScheme()} (JavaFX 22+) via reflection so the
     * project still compiles and runs on older JavaFX versions. Used only for the first-launch default.
     */
    private static boolean systemPrefersDark() {
        try {
            Object platformPrefs = Platform.class.getMethod("getPreferences").invoke(null);
            Class<?> prefsApi = Class.forName("javafx.application.Platform$Preferences");
            Object scheme = prefsApi.getMethod("getColorScheme").invoke(platformPrefs);
            return "DARK".equals(String.valueOf(scheme));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }
}
