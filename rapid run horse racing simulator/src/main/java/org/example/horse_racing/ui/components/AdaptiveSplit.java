package org.example.horse_racing.ui.components;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Two-pane layout: main + side by side when wide enough, stacked (side below main) when narrow.
 */
public class AdaptiveSplit extends Pane {

    private final Node main;
    private final Node side;
    private final double breakpoint;
    private final double sideWidth;
    private final double gap;
    private final ReadOnlyBooleanWrapper stacked = new ReadOnlyBooleanWrapper(false);

    public AdaptiveSplit(Node main, Node side, double breakpoint, double sideWidth, double gap) {
        this.main = main;
        this.side = side;
        this.breakpoint = breakpoint;
        this.sideWidth = sideWidth;
        this.gap = gap;
        getChildren().addAll(main, side);
    }

    public ReadOnlyBooleanProperty stackedProperty() {
        return stacked.getReadOnlyProperty();
    }

    private boolean isWide(double w) {
        return w >= breakpoint;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + 200;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + breakpoint;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double w = (width < 0 ? getWidth() : width) - in.getLeft() - in.getRight();
        double h;
        if (isWide(w)) {
            double mainW = w - sideWidth - gap;
            h = Math.max(main.prefHeight(mainW), side.prefHeight(sideWidth));
        } else {
            h = main.prefHeight(w) + gap + side.prefHeight(w);
        }
        return in.getTop() + in.getBottom() + h;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        double x = in.getLeft(), y = in.getTop();
        boolean wide = isWide(w);
        stacked.set(!wide);
        if (wide) {
            double mainW = w - sideWidth - gap;
            main.resizeRelocate(x, y, mainW, h);
            side.resizeRelocate(x + mainW + gap, y, sideWidth, h);
        } else {
            double mh = main.prefHeight(w);
            main.resizeRelocate(x, y, w, mh);
            side.resizeRelocate(x, y + mh + gap, w, Math.max(side.prefHeight(w), h - mh - gap));
        }
    }
}
