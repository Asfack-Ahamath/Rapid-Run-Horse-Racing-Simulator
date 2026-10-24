package org.example.horse_racing.ui.views;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.example.horse_racing.ui.View;
import org.example.horse_racing.ui.components.Ui;

/**
 * Scrollable page with responsive outer padding (roomier on wide windows).
 */
abstract class BaseView implements View {

    protected final VBox page = new VBox(22);
    /** Centres the page on very wide / ultrawide screens instead of leaving it hugging the left. */
    private final javafx.scene.layout.StackPane centerer = new javafx.scene.layout.StackPane(page) {
        @Override
        protected double computePrefHeight(double width) {
            // Measure the page at the width it will actually get (it is capped by its max width).
            double w = width < 0 ? width : Math.min(width - snappedLeftInset() - snappedRightInset(), page.getMaxWidth());
            return snappedTopInset() + snappedBottomInset() + page.prefHeight(w);
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }
    };
    private final ScrollPane scroller = Ui.scroller(centerer);

    BaseView() {
        page.getStyleClass().add("view");
        page.setMaxWidth(1920);
        centerer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        centerer.getStyleClass().add("view");
        scroller.getStyleClass().add("view");
        scroller.viewportBoundsProperty().addListener((o, a, b) -> applySpacing(b.getWidth(), b.getHeight()));
        applySpacing(1200, 800);
    }

    /** Roomier on large windows, tighter on small or short ones (laptops with display scaling). */
    private void applySpacing(double w, double h) {
        boolean shortScreen = h > 0 && h < 640;
        double side = w >= 1100 ? 36 : w >= 800 ? 26 : w >= 560 ? 18 : 12;
        double top = shortScreen ? 16 : (w >= 800 ? 30 : 20);
        page.setPadding(new Insets(top, side, shortScreen ? 20 : 32, side));
        page.setSpacing(shortScreen ? 14 : 22);
    }

    @Override
    public Node root() {
        return scroller;
    }
}
