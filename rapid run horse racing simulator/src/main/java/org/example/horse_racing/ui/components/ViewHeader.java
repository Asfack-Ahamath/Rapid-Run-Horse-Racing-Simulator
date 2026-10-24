package org.example.horse_racing.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Page header: title + subtitle on the left and actions on the right.
 * When the actions don't fit beside the title they drop onto their own row.
 */
public class ViewHeader extends Pane {

    private static final double GAP = 16;

    private final VBox titles;
    private final HBox actions;

    public ViewHeader(String title, String subtitle, Node... actionNodes) {
        Label t = Ui.label(title, "view-title");
        Label s = Ui.label(subtitle, "view-subtitle");
        s.setWrapText(true);
        titles = new VBox(4, t, s);
        actions = new HBox(10, actionNodes);
        actions.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(titles, actions);
    }

    public HBox actions() {
        return actions;
    }

    private boolean fits(double w) {
        return titles.prefWidth(-1) + GAP + actions.prefWidth(-1) <= w;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + titles.prefWidth(-1) + GAP + actions.prefWidth(-1);
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double w = (width < 0 ? getWidth() : width) - in.getLeft() - in.getRight();
        double h = (w <= 0 || fits(w))
                ? Math.max(titles.prefHeight(-1), actions.prefHeight(-1))
                : titles.prefHeight(w) + 12 + actions.prefHeight(-1);
        return in.getTop() + in.getBottom() + h;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        double x = in.getLeft(), y = in.getTop();
        double aw = actions.prefWidth(-1), ah = actions.prefHeight(-1);
        if (actions.getChildren().isEmpty() || fits(w)) {
            double th = titles.prefHeight(w - aw - GAP);
            double rowH = Math.max(th, ah);
            titles.resizeRelocate(x, y + (rowH - th) / 2, w - aw - GAP, th);
            actions.resizeRelocate(x + w - aw, y + (rowH - ah) / 2, aw, ah);
        } else {
            double th = titles.prefHeight(w);
            titles.resizeRelocate(x, y, w, th);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.resizeRelocate(x, y + th + 12, Math.min(w, aw), ah);
            return;
        }
        actions.setAlignment(Pos.CENTER_RIGHT);
    }
}
