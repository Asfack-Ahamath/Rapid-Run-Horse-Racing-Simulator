package org.example.horse_racing;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.horse_racing.ui.AppShell;
import org.example.horse_racing.ui.components.Images;
import org.example.horse_racing.ui.theme.ThemeManager;

/**
 * Entry point. The whole UI is built in Java code (no FXML).
 */
public class HorseApplication extends Application {

    @Override
    public void start(Stage stage) {
        AppShell shell = new AppShell();

        // Size the window from the screen it opens on (visual bounds already account for the
        // taskbar and OS display scaling), so it fits laptops, 4K monitors and ultrawides alike.
        WindowFit screenFit = WindowFit.fit(Screen.getPrimary().getVisualBounds());
        double w = screenFit.width, h = screenFit.height;

        var icon = Images.resource(Images.HORSE);
        String title = "Rapid Run — Horse Racing Simulator";

        Scene scene = new Scene(shell.root(), w, h);
        ThemeManager.get().install(scene);
        shell.installShortcuts(scene);

        if (icon != null) stage.getIcons().add(icon);
        stage.setTitle(title);
        stage.setMinWidth(screenFit.minWidth);
        stage.setMinHeight(screenFit.minHeight);
        stage.setScene(scene);
        stage.centerOnScreen();
        // Small screens (e.g. 1366×768 at 125–150% scaling) get the whole screen straight away.
        stage.show();
        if (screenFit.maximize) stage.setMaximized(true);
    }

    /** Initial and minimum window size for a given screen. */
    record WindowFit(double width, double height, double minWidth, double minHeight, boolean maximize) {
        static WindowFit fit(Rectangle2D vb) {
            double sw = vb.getWidth(), sh = vb.getHeight();
            double w = Math.min(1360, sw * 0.9);
            double h = Math.min(880, sh * 0.9);
            double minW = Math.min(640, sw);
            double minH = Math.min(480, sh);
            boolean maximize = sw < 1200 || sh < 720;
            return new WindowFit(Math.max(w, minW), Math.max(h, minH), minW, minH, maximize);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
