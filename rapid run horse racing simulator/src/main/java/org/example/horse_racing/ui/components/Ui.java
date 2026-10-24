package org.example.horse_racing.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

/**
 * Small factory helpers so views read as layout, not boilerplate.
 */
public final class Ui {

    private Ui() {}

    public static Label label(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        return l;
    }

    public static Button button(String text, String icon, String... classes) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(classes);
        if (icon != null) b.setGraphic(Icons.of(icon, 16));
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    public static Button iconButton(String icon, String tooltip, String... classes) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, 18));
        b.getStyleClass().setAll("icon-btn");
        b.getStyleClass().addAll(classes);
        if (tooltip != null) tip(b, tooltip);
        b.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return b;
    }

    public static <T extends Control> T tip(T control, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(350));
        control.setTooltip(t);
        return control;
    }

    public static Region hspacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        r.setMinWidth(0);
        return r;
    }

    public static Region vspacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region divider() {
        Region r = new Region();
        r.getStyleClass().add("divider");
        return r;
    }

    public static HBox hbox(double spacing, Node... kids) {
        HBox h = new HBox(spacing, kids);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static VBox vbox(double spacing, Node... kids) {
        return new VBox(spacing, kids);
    }

    public static VBox card(Node... kids) {
        VBox v = new VBox(14, kids);
        v.getStyleClass().add("card");
        return v;
    }

    public static VBox cardWithHeader(String title, String subtitle, Node trailing, Node... body) {
        VBox titles = vbox(2, label(title, "card-title"));
        if (subtitle != null) titles.getChildren().add(label(subtitle, "card-subtitle"));
        HBox header = hbox(8, titles, hspacer());
        if (trailing != null) header.getChildren().add(trailing);
        VBox card = card(header);
        card.getChildren().addAll(body);
        return card;
    }

    public static String groupClass(HorseGroup g) {
        return "group-" + g.name().toLowerCase();
    }

    public static Label badge(String text, String... classes) {
        Label l = label(text, "badge");
        l.getStyleClass().addAll(classes);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static Label groupBadge(HorseGroup g) {
        return badge(g.toString(), groupClass(g));
    }

    public static Region dot(HorseGroup g) {
        Region r = new Region();
        r.getStyleClass().addAll("dot", groupClass(g));
        return r;
    }

    public static StackPane laneChip(HorseGroup g) {
        StackPane p = new StackPane(label(g.name()));
        p.getStyleClass().addAll("lane-chip", groupClass(g));
        return p;
    }

    /** Circular avatar: the horse's photo if it has one, otherwise its initials on a group tint. */
    public static StackPane avatar(Horse h, double size) {
        StackPane p = new StackPane();
        p.getStyleClass().addAll("avatar", groupClass(h.group()));
        p.setMinSize(size, size);
        p.setPrefSize(size, size);
        p.setMaxSize(size, size);
        Image img = loadImage(h.imagePath(), size * 2);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(false);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setClip(new Circle(size / 2, size / 2, size / 2));
            p.getChildren().add(iv);
        } else {
            Label l = label(initials(h.name()), "avatar-text");
            l.setStyle("-fx-font-size: " + Math.round(size * 0.36) + "px;");
            p.getChildren().add(l);
        }
        return p;
    }

    public static Image loadImage(String url, double size) {
        if (url == null || url.isBlank()) return null;
        try {
            Image img = new Image(url, size, size, true, true, false);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "");
        return s.toUpperCase();
    }

    /** A scroll pane configured for full-width vertical scrolling of a view. */
    public static ScrollPane scroller(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return sp;
    }

    public static ToggleButton toggle(String text, String icon, ToggleGroup group) {
        ToggleButton t = new ToggleButton(text);
        if (icon != null) t.setGraphic(Icons.of(icon, 16));
        t.setToggleGroup(group);
        t.setMinWidth(Region.USE_PREF_SIZE);
        return t;
    }

    /** Keeps exactly one toggle selected in a group (no "unselect by clicking again"). */
    public static void requireSelection(ToggleGroup group) {
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) old.setSelected(true);
        });
    }

    public static HBox segmented(ToggleButton... toggles) {
        HBox h = new HBox(2, toggles);
        h.getStyleClass().add("segmented");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setMinWidth(Region.USE_PREF_SIZE);
        h.setMaxWidth(Region.USE_PREF_SIZE);
        return h;
    }
}
