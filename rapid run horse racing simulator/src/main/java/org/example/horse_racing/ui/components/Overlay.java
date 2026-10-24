package org.example.horse_racing.ui.components;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

/**
 * In-window modal dialogs. They are part of the main scene, so they inherit the active theme,
 * scale with the window and never get lost behind other windows.
 */
public final class Overlay {

    private static StackPane layer;

    private Overlay() {}

    public static void install(StackPane overlayLayer) {
        layer = overlayLayer;
        layer.setPickOnBounds(false);
    }

    public static boolean isOpen() {
        return layer != null && !layer.getChildren().isEmpty();
    }

    /** Handle to an open dialog. */
    public static final class Handle {
        private final Runnable closer;
        private Runnable onClosed;
        private boolean closed;

        private Handle(Runnable closer) { this.closer = closer; }

        public void close() {
            if (closed) return;
            closed = true;
            closer.run();
            if (onClosed != null) onClosed.run();
        }

        public Handle onClosed(Runnable r) {
            this.onClosed = r;
            return this;
        }
    }

    /**
     * Opens a standard dialog with a title, scrollable body and a footer of buttons.
     * The returned handle closes it; Escape and clicking the backdrop also close it.
     */
    public static Handle dialog(String title, String icon, Node body, List<Button> actions, double maxWidth) {
        VBox box = new VBox();
        box.getStyleClass().add("dialog");

        HBox header = new HBox(12);
        header.getStyleClass().add("dialog-header");
        if (icon != null) {
            StackPane iconWrap = new StackPane(Icons.of(icon, 20));
            iconWrap.getStyleClass().add("stat-icon");
            header.getChildren().add(iconWrap);
        }
        header.getChildren().addAll(Ui.label(title, "dialog-title"), Ui.hspacer());
        Button x = Ui.iconButton(Icons.CLOSE, "Close (Esc)");
        header.getChildren().add(x);

        VBox bodyWrap = new VBox(body);
        bodyWrap.getStyleClass().add("dialog-body");
        ScrollPane sp = Ui.scroller(bodyWrap);
        sp.setFitToHeight(false);
        sp.setMinHeight(80);
        sp.prefViewportHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> bodyWrap.prefHeight(sp.getViewportBounds().getWidth() > 0 ? sp.getViewportBounds().getWidth() : -1),
                sp.viewportBoundsProperty(), bodyWrap.needsLayoutProperty()));
        VBox.setVgrow(sp, Priority.ALWAYS);

        HBox footer = new HBox(10);
        footer.getStyleClass().add("dialog-footer");
        footer.getChildren().addAll(actions);
        // Wrap footer buttons onto separate lines on very narrow windows.
        footer.setMinWidth(0);

        box.getChildren().addAll(header, sp);
        if (!actions.isEmpty()) box.getChildren().add(footer);

        Handle h = show(box, maxWidth, true);
        x.setOnAction(e -> h.close());
        return h;
    }

    /** Shows arbitrary content centred over a dimmed backdrop. */
    public static Handle show(Region content, double maxWidth, boolean dismissOnBackdrop) {
        if (layer == null) throw new IllegalStateException("Overlay not installed");

        Region scrim = new Region();
        scrim.getStyleClass().add("overlay-scrim");

        // A VBox gives the dialog its preferred height and only shrinks it (letting the body
        // scroll) when the window is too short; width is capped and centred.
        VBox holder = new VBox(content);
        holder.setAlignment(Pos.CENTER);
        holder.setPickOnBounds(false);
        holder.setPadding(new javafx.geometry.Insets(20, 16, 20, 16));
        holder.setFillWidth(true);
        content.maxWidthProperty().bind(javafx.beans.binding.Bindings.min(maxWidth, layer.widthProperty().subtract(32)));
        content.setMaxHeight(Region.USE_PREF_SIZE);
        content.setPrefWidth(maxWidth);

        StackPane modal = new StackPane(scrim, holder);
        layer.getChildren().add(modal);

        final Handle[] ref = new Handle[1];
        Runnable closer = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(140), modal);
            ft.setToValue(0);
            ScaleTransition st = new ScaleTransition(Duration.millis(140), content);
            st.setToX(0.97);
            st.setToY(0.97);
            ParallelTransition pt = new ParallelTransition(ft, st);
            pt.setOnFinished(e -> layer.getChildren().remove(modal));
            pt.play();
        };
        Handle handle = new Handle(closer);
        ref[0] = handle;

        if (dismissOnBackdrop) scrim.setOnMouseClicked(e -> handle.close());
        modal.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                handle.close();
                e.consume();
            }
        });

        modal.setOpacity(0);
        content.setScaleX(0.96);
        content.setScaleY(0.96);
        FadeTransition fi = new FadeTransition(Duration.millis(160), modal);
        fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(200), content);
        si.setToX(1);
        si.setToY(1);
        si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();

        content.requestFocus();
        return handle;
    }

    /** Confirmation dialog. */
    public static void confirm(String title, String message, String confirmText, boolean danger, Runnable onConfirm) {
        var msg = Ui.label(message, "text-muted");
        msg.setWrapText(true);
        Button cancel = Ui.button("Cancel", null, "btn-secondary");
        Button ok = Ui.button(confirmText, danger ? Icons.DELETE : Icons.CHECK, danger ? "btn-danger" : "btn-primary");
        ok.setDefaultButton(true);
        Handle h = dialog(title, danger ? Icons.WARNING : Icons.INFO, msg, List.of(cancel, ok), 440);
        cancel.setOnAction(e -> h.close());
        ok.setOnAction(e -> {
            h.close();
            onConfirm.run();
        });
        cancel.setCancelButton(true);
        javafx.application.Platform.runLater(ok::requestFocus);
    }
}
