package org.example.horse_racing.ui.components;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lightweight, non-blocking notifications stacked in the bottom-right corner.
 */
public final class Toasts {

    public enum Kind { SUCCESS, ERROR, INFO }

    private static VBox layer;

    private Toasts() {}

    public static void install(VBox toastLayer) {
        layer = toastLayer;
        layer.setAlignment(Pos.BOTTOM_RIGHT);
        layer.setSpacing(10);
        layer.setPickOnBounds(false);
        layer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    public static void success(String msg) { show(Kind.SUCCESS, msg); }
    public static void error(String msg) { show(Kind.ERROR, msg); }
    public static void info(String msg) { show(Kind.INFO, msg); }

    public static void show(Kind kind, String message) {
        if (layer == null) return;
        String icon = switch (kind) {
            case SUCCESS -> Icons.CHECK_CIRCLE;
            case ERROR -> Icons.ERROR;
            case INFO -> Icons.INFO;
        };
        var text = Ui.label(message);
        text.setWrapText(true);
        text.setMaxWidth(320);
        HBox toast = new HBox(10, Icons.of(icon, 20), text);
        toast.getStyleClass().addAll("toast", kind.name().toLowerCase());
        toast.setMaxWidth(380);
        toast.setOnMouseClicked(e -> dismiss(toast));

        layer.getChildren().add(toast);
        toast.setOpacity(0);
        toast.setTranslateY(12);
        FadeTransition fi = new FadeTransition(Duration.millis(180), toast);
        fi.setToValue(1);
        TranslateTransition ti = new TranslateTransition(Duration.millis(220), toast);
        ti.setToY(0);
        ti.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, ti).play();

        PauseTransition wait = new PauseTransition(Duration.seconds(kind == Kind.ERROR ? 5 : 3.2));
        wait.setOnFinished(e -> dismiss(toast));
        wait.play();
    }

    private static void dismiss(HBox toast) {
        if (!layer.getChildren().contains(toast) || toast.getProperties().containsKey("closing")) return;
        toast.getProperties().put("closing", true);
        FadeTransition fo = new FadeTransition(Duration.millis(200), toast);
        fo.setToValue(0);
        fo.setOnFinished(e -> layer.getChildren().remove(toast));
        fo.play();
    }
}
