package org.example.horse_racing.ui.views;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.horse_racing.service.AppPaths;
import org.example.horse_racing.ui.components.*;

import java.util.List;

/** About + keyboard shortcuts. */
public final class AboutDialog {

    private AboutDialog() {}

    public static void show() {
        if (Overlay.isOpen()) return;

        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        ImageView iv = new ImageView(Images.tinted(Images.HORSE, Color.web("#0F3D2A")));
        iv.setFitWidth(26);
        iv.setFitHeight(26);
        iv.setPreserveRatio(true);
        mark.getChildren().add(iv);
        VBox titles = new VBox(2, Ui.label("Rapid Run", "section-title"), Ui.label("Horse Racing Simulator · v2.1", "text-muted"));
        HBox brand = Ui.hbox(12, mark, titles);

        Label desc = Ui.label("Manage a stable across four groups, draw a field of one runner per group, and watch "
                + "them race on an animated track. Finishing times are driven by each horse's form and age plus race-day luck, "
                + "so favourites win more often — but never always.", "text-muted");
        desc.setWrapText(true);

        GridPane keys = new GridPane();
        keys.setHgap(14);
        keys.setVgap(8);
        String mod = System.getProperty("os.name", "").toLowerCase().contains("mac") ? "⌘" : "Ctrl";
        String[][] rows = {
                {mod + " 1–4", "Switch page"},
                {mod + " N", "Add a horse"},
                {mod + " F", "Search horses"},
                {mod + " R", "Start / stop race"},
                {mod + " B", "Expand / collapse sidebar"},
                {mod + " Shift T", "Light / dark mode"},
                {"Esc", "Close dialog"},
        };
        for (int i = 0; i < rows.length; i++) {
            Label k = Ui.label(rows[i][0], "kbd");
            k.setMinWidth(Region.USE_PREF_SIZE);
            keys.add(k, 0, i);
            keys.add(Ui.label(rows[i][1], "text-muted"), 1, i);
        }

        Label data = Ui.label("Data folder: " + AppPaths.dataDir(), "field-hint");
        data.setWrapText(true);

        VBox body = new VBox(16, brand, desc, Ui.divider(), Ui.label("Keyboard shortcuts", "card-title"), keys, Ui.divider(), data);
        body.setAlignment(Pos.TOP_LEFT);

        Button close = Ui.button("Close", null, "btn-primary");
        close.setDefaultButton(true);
        Overlay.Handle h = Overlay.dialog("About", Icons.INFO, body, List.of(close), 480);
        close.setOnAction(e -> h.close());
    }
}
