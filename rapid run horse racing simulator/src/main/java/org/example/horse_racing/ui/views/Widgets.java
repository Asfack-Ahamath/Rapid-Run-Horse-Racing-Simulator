package org.example.horse_racing.ui.views;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.ui.components.Icons;
import org.example.horse_racing.ui.components.Ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** View-level building blocks shared by several pages. */
final class Widgets {

    private Widgets() {}

    /** Stat tile: icon, big value, label, small footnote. */
    static final class StatTile extends VBox {
        final Label value = Ui.label("—", "stat-value");
        final Label foot = Ui.label("", "stat-foot");

        StatTile(String label, String icon, String tone) {
            super(10);
            getStyleClass().add("card");
            StackPane ic = new StackPane(Icons.of(icon, 20));
            ic.getStyleClass().add("stat-icon");
            if (tone != null) ic.getStyleClass().add(tone);
            Label l = Ui.label(label, "stat-label");
            HBox top = Ui.hbox(10, l, Ui.hspacer(), ic);
            value.setMinWidth(0);
            value.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            foot.setWrapText(true);
            getChildren().addAll(top, value, foot);
        }

        void set(String v, String f) {
            value.setText(v);
            foot.setText(f);
        }
    }

    /** A horizontal proportion bar (track + coloured fill). */
    static StackPane bar(double fraction, HorseGroup g, boolean tall) {
        Region track = new Region();
        track.getStyleClass().add("track");
        Region fill = new Region();
        fill.getStyleClass().addAll("bar", Ui.groupClass(g));
        if (tall) {
            track.getStyleClass().add("tall");
            fill.getStyleClass().add("tall");
        }
        StackPane p = new StackPane(track, fill);
        p.setAlignment(Pos.CENTER_LEFT);
        double f = Math.max(0, Math.min(1, fraction));
        fill.maxWidthProperty().bind(p.widthProperty().multiply(f));
        fill.setMinWidth(f > 0 ? 8 : 0);
        track.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(p, Priority.ALWAYS);
        p.setMinWidth(40);
        return p;
    }

    static VBox emptyState(String icon, String title, String message, Button action) {
        StackPane ic = new StackPane(Icons.of(icon, 28));
        ic.getStyleClass().add("empty-icon");
        Label t = Ui.label(title, "section-title");
        Label m = Ui.label(message, "text-muted");
        m.setWrapText(true);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox v = new VBox(10, ic, t, m);
        v.getStyleClass().add("empty-state");
        if (action != null) {
            VBox.setMargin(action, new javafx.geometry.Insets(6, 0, 0, 0));
            v.getChildren().add(action);
        }
        return v;
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    static String relative(LocalDateTime t) {
        Duration d = Duration.between(t, LocalDateTime.now());
        if (d.isNegative() || d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + " min ago";
        if (d.toHours() < 24) return d.toHours() + " h ago";
        if (d.toDays() < 7) return d.toDays() + (d.toDays() == 1 ? " day ago" : " days ago");
        return DATE.format(t);
    }

    static Node withMinWidthZero(Label l) {
        l.setMinWidth(0);
        return l;
    }
}
