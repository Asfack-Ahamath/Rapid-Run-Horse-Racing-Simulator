package org.example.horse_racing.ui.components;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * A card grid that picks its column count from the available width, like CSS
 * {@code grid-template-columns: repeat(auto-fill, minmax(min, 1fr))}.
 * Cells in the same row share the tallest cell's height so cards line up.
 */
public class ResponsiveGrid extends Pane {

    private final DoubleProperty minColumnWidth = new SimpleDoubleProperty(this, "minColumnWidth", 260);
    private final IntegerProperty maxColumns = new SimpleIntegerProperty(this, "maxColumns", 12);
    private final DoubleProperty hgap = new SimpleDoubleProperty(this, "hgap", 16);
    private final DoubleProperty vgap = new SimpleDoubleProperty(this, "vgap", 16);

    public ResponsiveGrid(double minColumnWidth, double gap, Node... children) {
        setMinColumnWidth(minColumnWidth);
        hgap.set(gap);
        vgap.set(gap);
        getChildren().addAll(children);
        minColumnWidth().addListener(o -> requestLayout());
        maxColumns.addListener(o -> requestLayout());
        hgap.addListener(o -> requestLayout());
        vgap.addListener(o -> requestLayout());
    }

    public ResponsiveGrid(double minColumnWidth, Node... children) {
        this(minColumnWidth, 16, children);
    }

    public DoubleProperty minColumnWidth() { return minColumnWidth; }
    public void setMinColumnWidth(double v) { minColumnWidth.set(v); }
    public void setMaxColumns(int v) { maxColumns.set(v); }
    public void setGap(double v) { hgap.set(v); vgap.set(v); }

    private boolean balanced;

    /** Avoid orphaned cells: only use column counts that divide the item count evenly (e.g. 4 → 2 → 1). */
    public void setBalanced(boolean b) { balanced = b; requestLayout(); }

    public int columnsFor(double contentWidth) {
        double min = Math.max(1, minColumnWidth.get());
        int cols = (int) Math.floor((contentWidth + hgap.get()) / (min + hgap.get()));
        int visible = (int) managed().count();
        cols = Math.max(1, Math.min(cols, maxColumns.get()));
        if (visible > 0) cols = Math.min(cols, visible);
        if (balanced && visible > 0) while (cols > 1 && visible % cols != 0) cols--;
        return cols;
    }

    private java.util.stream.Stream<Node> managed() {
        return getChildren().stream().filter(Node::isManaged);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + Math.min(minColumnWidth.get(), 200);
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + minColumnWidth.get();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double w = (width < 0 ? Math.max(getWidth(), computePrefWidth(-1)) : width) - in.getLeft() - in.getRight();
        List<Node> kids = managed().toList();
        if (kids.isEmpty()) return in.getTop() + in.getBottom();
        int cols = columnsFor(w);
        double colW = cellWidth(w, cols);
        double total = 0;
        for (int i = 0; i < kids.size(); i += cols) {
            total += rowHeight(kids, i, cols, colW);
            if (i + cols < kids.size()) total += vgap.get();
        }
        return in.getTop() + in.getBottom() + total;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        List<Node> kids = managed().toList();
        if (kids.isEmpty()) return;
        int cols = columnsFor(w);
        double colW = cellWidth(w, cols);
        double y = in.getTop();
        for (int i = 0; i < kids.size(); i += cols) {
            double rh = rowHeight(kids, i, cols, colW);
            for (int c = 0; c < cols && i + c < kids.size(); c++) {
                Node n = kids.get(i + c);
                double x = in.getLeft() + c * (colW + hgap.get());
                n.resizeRelocate(snapPositionX(x), snapPositionY(y), snapSizeX(colW), snapSizeY(rh));
            }
            y += rh + vgap.get();
        }
    }

    private double cellWidth(double w, int cols) {
        return Math.max(0, (w - hgap.get() * (cols - 1)) / cols);
    }

    private double rowHeight(List<Node> kids, int start, int cols, double colW) {
        double h = 0;
        for (int c = 0; c < cols && start + c < kids.size(); c++) {
            Node n = kids.get(start + c);
            h = Math.max(h, n.prefHeight(colW));
        }
        return h;
    }
}
