package org.example.horse_racing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HorseApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(HorseApplication.class.getResource("Welcome.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 520, 420);
        scene.getStylesheets().add(getClass().getResource("styles/theme.css").toExternalForm());
        stage.setTitle("Rapid Run — Horse Racing Simulator");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}