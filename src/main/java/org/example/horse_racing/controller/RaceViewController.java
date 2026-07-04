package org.example.horse_racing.controller;

import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.service.FileExportService;
import org.example.horse_racing.service.RaceService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RaceViewController {

    /** Light colors matching chart (gold, blue, red, green) */
    private static final String[] HORSE_COLORS_LIGHT = {"#e8c97a", "#7eb8e8", "#e8a0a0", "#6ed4a0"};
    private static final String[] RANK_LABELS = {"1st", "2nd", "3rd", "4th"};

    @FXML private Button saveButton, selectButton, restartButton, simulateButton, resultsButton, viewChartButton;
    @FXML private VBox selectedHorsesBox, raceTrackBox;
    @FXML private HBox selectedHorsesDisplay;
    @FXML private Pane raceTrackPane;
    @FXML private Label timerLabel;

    private final RaceService raceService = ServiceRegistry.getRaceService();
    private final FileExportService fileExportService = ServiceRegistry.getFileExportService();
    private Map<HorseGroup, Horse> selectedHorses = new HashMap<>();
    private List<Horse> winningHorses = new ArrayList<>();
    private boolean dataSaved = false;
    private Runnable refreshCallback;
    private List<TranslateTransition> currentTransitions = new ArrayList<>();
    private AnimationTimer currentTimer;
    private Group trackContentGroup;
    private ChangeListener<Number> trackResizeListener;
    private static final double BASE_TRACK_WIDTH = 900;
    private static final double BASE_TRACK_HEIGHT = 220;

    @FXML
    public void initialize() {
        if (viewChartButton != null) {
            viewChartButton.setVisible(false);
            viewChartButton.setDisable(true);
        }
        if (resultsButton != null) {
            resultsButton.setVisible(false);
            resultsButton.setDisable(true);
        }
    }

    public void setRefreshCallback(Runnable r) {
        this.refreshCallback = r;
    }

    @FXML
    private void saveToFile() {
        try {
            Path path = Path.of("horse_details.txt");
            fileExportService.exportToTextFile(ServiceRegistry.getHorseService().getAllHorses(), path);
            dataSaved = true;
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Horse details saved to " + path.toAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    @FXML
    private void restartRace() {
        removeTrackResizeListener();
        if (currentTimer != null) {
            currentTimer.stop();
            currentTimer = null;
        }
        for (TranslateTransition tt : currentTransitions) {
            if (tt != null) tt.stop();
        }
        currentTransitions = new ArrayList<>();
        selectedHorses = new HashMap<>();
        winningHorses = new ArrayList<>();
        dataSaved = false;
        selectedHorsesBox.setVisible(false);
        raceTrackBox.setVisible(false);
        simulateButton.setVisible(false);
        raceTrackPane.getChildren().clear();
        if (timerLabel != null) timerLabel.setText("0.0s");
        if (simulateButton != null) {
            simulateButton.setText("▶ Simulate Race");
            simulateButton.setOnAction(e -> startSimulation());
            simulateButton.setDisable(false);
            simulateButton.setVisible(false);
        }
        if (viewChartButton != null) {
            viewChartButton.setVisible(false);
            viewChartButton.setDisable(true);
        }
        if (resultsButton != null) {
            resultsButton.setVisible(false);
            resultsButton.setDisable(true);
        }
        showAlert(Alert.AlertType.INFORMATION, "Restart", "Race has been reset. Save to File, then Select Horses to start a new race.");
    }

    @FXML
    private void selectHorses() {
        if (!dataSaved) {
            showAlert(Alert.AlertType.WARNING, "Save First", "Please run Save to File before selecting horses.");
            return;
        }
        try {
            selectedHorses = raceService.selectHorsesForRace();
            displaySelectedHorses();
            selectedHorsesBox.setVisible(true);
            raceTrackBox.setVisible(true);
            simulateButton.setVisible(true);
            if (viewChartButton != null) {
                viewChartButton.setVisible(true);
                viewChartButton.setDisable(true);
            }
            if (resultsButton != null) {
                resultsButton.setVisible(true);
                resultsButton.setDisable(true);
            }
        } catch (IllegalStateException ex) {
            Set<HorseGroup> empty = raceService.getEmptyGroups();
            StringBuilder sb = new StringBuilder("Each group needs at least one horse.\nEmpty: ");
            empty.forEach(g -> sb.append(g.name()).append(", "));
            showAlert(Alert.AlertType.WARNING, "Cannot Select", sb.toString());
        }
    }

    private void displaySelectedHorses() {
        selectedHorsesDisplay.getChildren().clear();
        List<Horse> inOrder = getHorsesInSelectedOrder();
        for (int i = 0; i < inOrder.size(); i++) {
            Horse h = inOrder.get(i);
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: " + HORSE_COLORS_LIGHT[i] + "; -fx-background-radius: 8; -fx-padding: 14; -fx-pref-width: 140; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
            Label nameLbl = new Label(h.name());
            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c2416;");
            Label groupLbl = new Label("Group " + h.group().name());
            groupLbl.setStyle("-fx-text-fill: #2c2416; -fx-opacity: 0.9;");
            card.getChildren().addAll(nameLbl, groupLbl);
            selectedHorsesDisplay.getChildren().add(card);
        }
    }

    private List<Horse> getHorsesInSelectedOrder() {
        return Arrays.stream(HorseGroup.values())
                .filter(selectedHorses::containsKey)
                .map(selectedHorses::get)
                .toList();
    }

    private List<Horse> getHorsesInLaneOrderWithRaceTimes() {
        return Arrays.stream(HorseGroup.values())
                .filter(selectedHorses::containsKey)
                .map(g -> winningHorses.stream().filter(h -> h.group() == g).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @FXML
    private void startSimulation() {
        if (selectedHorses.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Select Horses", "Please select horses first.");
            return;
        }

        timerLabel.setText("0.0s");
        if (simulateButton != null) {
            simulateButton.setText("⏹ Stop Race");
            simulateButton.setOnAction(e -> stopRace());
        }
        if (viewChartButton != null) viewChartButton.setDisable(true);

        winningHorses = raceService.simulateRace(selectedHorses);
        List<Horse> inOrder = getHorsesInLaneOrderWithRaceTimes();

        // Defer to get responsive dimensions; double runLater ensures layout pass
        Platform.runLater(() -> Platform.runLater(() -> buildAndRunRace(inOrder)));
    }

    private void buildAndRunRace(List<Horse> inOrder) {
        raceTrackPane.getChildren().clear();
        removeTrackResizeListener();

        trackContentGroup = new Group();
        double trackWidth = BASE_TRACK_WIDTH;
        double trackHeight = BASE_TRACK_HEIGHT;
        double laneHeight = trackHeight / 4;
        double horseWidth = 52;
        double horseHeight = 38;
        double badgeWidth = 110;
        double startX = 38;
        double finishX = trackWidth - badgeWidth - 12;
        double travelDist = finishX - startX - horseWidth - 6;

        // Lane labels (A, B, C, D)
        String[] laneLabels = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            Text lbl = new Text(laneLabels[i]);
            lbl.setFill(Color.color(1, 1, 1, 0.95));
            lbl.setFont(Font.font("System Bold", 12));
            lbl.setLayoutX(10);
            lbl.setLayoutY(i * laneHeight + laneHeight / 2 + 4);
            trackContentGroup.getChildren().add(lbl);
        }

        // Lane dividers (horizontal lines)
        for (int i = 1; i < 4; i++) {
            Line line = new Line(startX, i * laneHeight, trackWidth, i * laneHeight);
            line.setStroke(Color.color(1, 1, 1, 0.35));
            line.setStrokeWidth(1.5);
            trackContentGroup.getChildren().add(line);
        }

        // Start line
        Line startLine = new Line(startX, 0, startX, trackHeight);
        startLine.setStroke(Color.color(1, 1, 1, 0.6));
        startLine.setStrokeWidth(3);
        trackContentGroup.getChildren().add(startLine);

        // Finish line
        Line finish = new Line(finishX, 0, finishX, trackHeight);
        finish.setStroke(Color.WHITE);
        finish.setStrokeWidth(4);
        trackContentGroup.getChildren().add(finish);

        int minTime = winningHorses.stream().mapToInt(Horse::seconds).min().orElse(1);
        AtomicInteger finishOrder = new AtomicInteger(0);
        currentTimer = new AnimationTimer() {
            private long start = -1;
            @Override
            public void handle(long now) {
                if (start < 0) start = now;
                double elapsed = (now - start) / 1_000_000_000.0;
                if (timerLabel != null) timerLabel.setText(String.format("%.1fs", elapsed));
            }
        };
        currentTimer.start();

        List<TranslateTransition> allTransitions = new ArrayList<>();

        for (int i = 0; i < inOrder.size(); i++) {
            Horse h = inOrder.get(i);
            // Center horse vertically in lane: laneTop + (laneHeight - horseHeight) / 2
            double y = i * laneHeight + (laneHeight - horseHeight) / 2;
            String color = HORSE_COLORS_LIGHT[h.group().ordinal()];

            StackPane horseNode = new StackPane();
            horseNode.setPrefSize(horseWidth, horseHeight);
            horseNode.setMinSize(horseWidth, horseHeight);
            horseNode.setLayoutX(startX + 4);
            horseNode.setLayoutY(y);
            horseNode.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 0, 2); -fx-padding: 4; -fx-alignment: center;");
            ImageView iconView = new ImageView();
            try {
                Image icon = new Image(getClass().getResourceAsStream("/org/example/horse_racing/assets/horse-icon.png"));
                iconView.setImage(icon);
                iconView.setFitWidth(36);
                iconView.setFitHeight(28);
                iconView.setPreserveRatio(true);
            } catch (Exception ignored) {}
            horseNode.getChildren().add(iconView);

            trackContentGroup.getChildren().add(horseNode);

            int raceTime = h.seconds();
            double durationMs = Math.max(1500, 2500 + (raceTime - minTime) * 350);
            TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), horseNode);
            tt.setByX(travelDist);
            allTransitions.add(tt);

            int idx = i;
            int raceTimeForBadge = h.seconds();
            tt.setOnFinished(e -> Platform.runLater(() -> {
                int rank = finishOrder.incrementAndGet();
                addFinishBadge(h, rank, raceTimeForBadge, idx, horseHeight, laneHeight, finishX, trackWidth);
                if (rank == 4) {
                    if (currentTimer != null) currentTimer.stop();
                    if (simulateButton != null) {
                        simulateButton.setText("▶ Simulate Race");
                        simulateButton.setOnAction(ev -> startSimulation());
                        simulateButton.setDisable(false);
                    }
                    if (viewChartButton != null) viewChartButton.setDisable(true);
                    if (resultsButton != null) resultsButton.setDisable(false);
                    Platform.runLater(() -> showRaceFinishedDialog(true));
                }
            }));

        }

        raceTrackPane.getChildren().add(trackContentGroup);
        updateTrackScale();

        trackResizeListener = (o, oldVal, newVal) -> updateTrackScale();
        raceTrackPane.widthProperty().addListener(trackResizeListener);
        raceTrackPane.heightProperty().addListener(trackResizeListener);

        currentTransitions = allTransitions;
        for (TranslateTransition tt : allTransitions) {
            tt.play();
        }
    }

    private void updateTrackScale() {
        if (trackContentGroup == null) return;
        double w = raceTrackPane.getWidth();
        double h = raceTrackPane.getHeight();
        if (w > 0 && h > 0) {
            double scaleX = w / BASE_TRACK_WIDTH;
            double scaleY = h / BASE_TRACK_HEIGHT;
            trackContentGroup.getTransforms().setAll(new Scale(scaleX, scaleY, 0, 0));
        }
    }

    private void removeTrackResizeListener() {
        if (trackResizeListener != null && raceTrackPane != null) {
            raceTrackPane.widthProperty().removeListener(trackResizeListener);
            raceTrackPane.heightProperty().removeListener(trackResizeListener);
            trackResizeListener = null;
        }
    }

    @FXML
    private void stopRace() {
        removeTrackResizeListener();
        if (currentTimer != null) {
            currentTimer.stop();
            currentTimer = null;
        }
        for (TranslateTransition tt : currentTransitions) {
            tt.stop();
            if (tt.getNode() != null) tt.getNode().setTranslateX(0);
        }
        currentTransitions.clear();
        if (timerLabel != null) timerLabel.setText("0.0s");
        if (simulateButton != null) {
            simulateButton.setText("▶ Simulate Race");
            simulateButton.setOnAction(e -> startSimulation());
            simulateButton.setDisable(false);
        }
        if (viewChartButton != null) viewChartButton.setDisable(true);
        showAlert(Alert.AlertType.INFORMATION, "Race Stopped", "The race has been stopped.");
    }

    @FXML
    private void showResults() {
        if (winningHorses.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Results", "Run a race first to view results.");
            return;
        }
        showRaceFinishedDialog(false);
    }

    @FXML
    private void viewChart() {
        if (winningHorses.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Results", "Run a race first to view the chart.");
            return;
        }
        showChartPopup();
    }

    private void addFinishBadge(Horse h, int rank, int raceTime, int laneIdx,
                                double badgeHeight, double laneHeight, double finishX, double trackWidth) {
        double badgeWidth = 110;
        double x = finishX + 8;
        double y = laneIdx * laneHeight + (laneHeight - badgeHeight) / 2;  // Same centering as horse

        VBox badge = new VBox(1);
        badge.setPrefSize(badgeWidth, badgeHeight);
        badge.setMinSize(badgeWidth, badgeHeight);
        badge.setLayoutX(x);
        badge.setLayoutY(y);
        String groupColor = HORSE_COLORS_LIGHT[h.group().ordinal()];
        badge.setStyle("-fx-background-color: " + groupColor + "; -fx-background-radius: 6; -fx-padding: 1 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 1);");
        Label rankLbl = new Label(RANK_LABELS[rank - 1]);
        rankLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #2c2416;");
        Label nameLbl = new Label(h.name());
        nameLbl.setStyle("-fx-font-size: 8px; -fx-text-fill: #2c2416;");
        nameLbl.setWrapText(true);
        Label timeLbl = new Label(raceTime + "s");
        timeLbl.setStyle("-fx-font-size: 8px; -fx-text-fill: #2c2416; -fx-opacity: 0.9;");
        badge.getChildren().addAll(rankLbl, nameLbl, timeLbl);
        if (trackContentGroup != null) trackContentGroup.getChildren().add(badge);
    }

    private void showRaceFinishedDialog(boolean showChartOnClose) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Race Finished!");
        stage.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f6f0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 8);");
        root.setMinWidth(420);
        root.setPrefWidth(450);

        // Header
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color: linear-gradient(to bottom, #0d4a2e 0%, #1a5c3f 100%); -fx-padding: 24 28; -fx-alignment: center;");
        Label titleLbl = new Label("🏆 Race Finished!");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label("Finishing Results");
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);");
        header.getChildren().addAll(titleLbl, subLbl);
        root.getChildren().add(header);

        // Results
        VBox resultsBox = new VBox(10);
        resultsBox.setStyle("-fx-padding: 24 28; -fx-background-color: #fdfcf9;");
        for (int i = 0; i < winningHorses.size(); i++) {
            Horse h = winningHorses.get(i);
            String color = HORSE_COLORS_LIGHT[h.group().ordinal()];
            HBox row = new HBox(14);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-padding: 14 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);");
            Label rankLbl = new Label(RANK_LABELS[i]);
            rankLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c2416; -fx-min-width: 45;");
            Label nameLbl = new Label(h.name());
            nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c2416;");
            nameLbl.setWrapText(true);
            Label timeLbl = new Label(h.seconds() + "s");
            timeLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c2416; -fx-opacity: 0.9;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(rankLbl, nameLbl, spacer, timeLbl);
            resultsBox.getChildren().add(row);
        }
        root.getChildren().add(resultsBox);

        // Buttons
        HBox btnBox = new HBox(12);
        btnBox.setStyle("-fx-padding: 20 28 24; -fx-background-color: #fdfcf9; -fx-alignment: center;");
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setStyle("-fx-padding: 10 28;");
        closeBtn.setOnAction(e -> {
            stage.close();
            if (showChartOnClose && viewChartButton != null) {
                Platform.runLater(() -> viewChartButton.setDisable(false));
            }
        });
        btnBox.getChildren().add(closeBtn);
        root.getChildren().add(btnBox);

        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/org/example/horse_racing/styles/theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void showChartPopup() {
        showChartPopup(false);
    }

    private void showChartPopup(boolean enableViewChartOnClose) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/BarChart.fxml"));
            Pane chartRoot = loader.load();
            BarChartController ctrl = loader.getController();

            ctrl.setData(winningHorses, List.of());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            javafx.stage.Window owner = viewChartButton != null && viewChartButton.getScene() != null ? viewChartButton.getScene().getWindow() : null;
            if (owner != null) stage.initOwner(owner);
            stage.setTitle("Race Results — Chart");
            stage.setResizable(true);
            stage.setMinWidth(640);
            stage.setMinHeight(520);

            VBox root = new VBox(20);
            root.setStyle("-fx-background-color: #f8f6f0; -fx-padding: 28;");
            root.setMinSize(640, 520);
            root.setPrefSize(640, 520);

            Label titleLbl = new Label("Race Results — Finishing Times");
            titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0d4a2e;");
            VBox.setVgrow(chartRoot, Priority.ALWAYS);
            chartRoot.setMaxWidth(Double.MAX_VALUE);
            chartRoot.setMaxHeight(Double.MAX_VALUE);

            Button closeBtn = new Button("Close");
            closeBtn.getStyleClass().add("btn-primary");
            closeBtn.setOnAction(e -> stage.close());
            HBox btnBox = new HBox(closeBtn);
            btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            root.getChildren().addAll(titleLbl, chartRoot, btnBox);

            Scene scene = new Scene(root);
            java.net.URL themeCss = getClass().getResource("/org/example/horse_racing/styles/theme.css");
            java.net.URL chartCss = getClass().getResource("/org/example/horse_racing/styles/chart.css");
            if (themeCss != null) scene.getStylesheets().add(themeCss.toExternalForm());
            if (chartCss != null) scene.getStylesheets().add(chartCss.toExternalForm());
            stage.setScene(scene);
            if (enableViewChartOnClose && viewChartButton != null) {
                stage.setOnHidden(e -> Platform.runLater(() -> viewChartButton.setDisable(false)));
            }
            stage.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(title);
        a.showAndWait();
    }
}
