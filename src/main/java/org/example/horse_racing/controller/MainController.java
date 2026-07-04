package org.example.horse_racing.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentStack;
    @FXML private Button exitButton;
    @FXML private Button navHome;
    @FXML private Button navHorses;
    @FXML private Button navRace;
    @FXML private Button navAbout;

    private Node homeView;
    private Node horsesView;
    private Node raceView;
    private DashboardController dashboardController;
    private HorsesViewController horsesViewController;
    private RaceViewController raceViewController;

    private static final String NAV_ACTIVE = "nav-btn-active";

    @FXML
    public void initialize() {
        loadViews();
        showHome();
    }

    private void loadViews() {
        try {
            FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/DashboardView.fxml"));
            homeView = homeLoader.load();
            homeView.setCache(true);
            homeView.setCacheHint(javafx.scene.CacheHint.SPEED);
            dashboardController = homeLoader.getController();

            FXMLLoader horsesLoader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/HorsesView.fxml"));
            horsesView = horsesLoader.load();
            horsesView.setCache(true);
            horsesView.setCacheHint(javafx.scene.CacheHint.SPEED);
            horsesViewController = horsesLoader.getController();
            horsesViewController.setRefreshCallback(() -> dashboardController.refresh());

            FXMLLoader raceLoader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/RaceView.fxml"));
            raceView = raceLoader.load();
            raceViewController = raceLoader.getController();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setActiveNav(Button active) {
        navHome.getStyleClass().remove(NAV_ACTIVE);
        navHorses.getStyleClass().remove(NAV_ACTIVE);
        navRace.getStyleClass().remove(NAV_ACTIVE);
        navAbout.getStyleClass().remove(NAV_ACTIVE);
        if (active != null) active.getStyleClass().add(NAV_ACTIVE);
    }

    private void restoreActiveNav() {
        if (contentStack.getChildren().isEmpty()) return;
        Node top = contentStack.getChildren().get(0);
        if (top == homeView) setActiveNav(navHome);
        else if (top == horsesView) setActiveNav(navHorses);
        else if (top == raceView) setActiveNav(navRace);
    }

    @FXML
    private void showHome() {
        contentStack.getChildren().clear();
        contentStack.getChildren().add(homeView);
        setActiveNav(navHome);
        dashboardController.refresh();
    }

    @FXML
    private void showHorses() {
        contentStack.getChildren().clear();
        contentStack.getChildren().add(horsesView);
        setActiveNav(navHorses);
        horsesViewController.refresh();
    }

    @FXML
    private void showRace() {
        contentStack.getChildren().clear();
        contentStack.getChildren().add(raceView);
        setActiveNav(navRace);
    }

    @FXML
    private void showAbout() {
        setActiveNav(navAbout);
        Stage owner = (Stage) navAbout.getScene().getWindow();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("About Rapid Run");

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #fdfcf9 0%, #f0ebe0 100%); -fx-padding: 32; -fx-min-width: 420; -fx-max-width: 520;");
        root.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("RAPID RUN");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0d4a2e;");
        Label subtitle = new Label("Horse Racing Simulator");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b5d4b;");

        VBox gameSection = new VBox(8);
        gameSection.setAlignment(Pos.TOP_LEFT);
        gameSection.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        Label gameTitle = new Label("About the Game");
        gameTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0d4a2e;");
        Label gameDesc = new Label(
                "Rapid Run is a professional horse racing simulation that lets you manage your stable, " +
                "select champions from four groups (A, B, C, D), and watch them race in real-time with animated visuals. " +
                "Save your horses to file, run races, and view detailed results with interactive charts.");
        gameDesc.setWrapText(true);
        gameDesc.setTextAlignment(TextAlignment.JUSTIFY);
        gameDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c2416; -fx-line-spacing: 4;");
        gameSection.getChildren().addAll(gameTitle, gameDesc);

        VBox devSection = new VBox(8);
        devSection.setAlignment(Pos.TOP_LEFT);
        devSection.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        Label devTitle = new Label("Developer");
        devTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0d4a2e;");
        Label devInfo = new Label(
                "Rapid Run v1.0\n" +
                "Built with JavaFX 21\n" +
                "© 2025 Rapid Run Team");
        devInfo.setWrapText(true);
        devInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c2416; -fx-line-spacing: 4;");
        devSection.getChildren().addAll(devTitle, devInfo);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> {
            stage.close();
            restoreActiveNav();
        });
        stage.setOnHidden(e -> restoreActiveNav());

        root.getChildren().addAll(title, subtitle, gameSection, devSection, closeBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        java.net.URL css = getClass().getResource("/org/example/horse_racing/styles/theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }

    @FXML
    private void handleExit() {
        ((Stage) exitButton.getScene().getWindow()).close();
    }
}
