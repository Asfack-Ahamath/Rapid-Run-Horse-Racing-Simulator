package org.example.horse_racing.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeController {

    @SuppressWarnings("unused")
    public void welcome(ActionEvent e) throws IOException {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/MainSimulator.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1080, 720);
        java.net.URL themeCss = getClass().getResource("/org/example/horse_racing/styles/theme.css");
        java.net.URL dashboardCss = getClass().getResource("/org/example/horse_racing/styles/dashboard.css");
        if (themeCss != null) scene.getStylesheets().add(themeCss.toExternalForm());
        if (dashboardCss != null) scene.getStylesheets().add(dashboardCss.toExternalForm());

        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
