package org.example.horse_racing.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.example.horse_racing.domain.Horse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BarChartController {

    @FXML private BarChart<String, Number> barChart;
    @FXML private NumberAxis yAxis;
    @FXML private Pane chartOverlay;

    private List<Horse> horsesInOrder = new ArrayList<>();
    private ChangeListener<Number> resizeListener;

    /**
     * Sets chart data: horses in A,B,C,D order, bar colors by group, time at top of each bar.
     */
    public void setData(List<Horse> winningHorses, List<String> barColors) {
        if (barChart == null || winningHorses == null) return;
        barChart.getData().clear();
        if (chartOverlay != null) chartOverlay.getChildren().clear();

        // Order by group A, B, C, D (not winning order)
        horsesInOrder = winningHorses.stream()
                .sorted(Comparator.comparing(h -> h.group().ordinal()))
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Race Time (seconds)");
        for (Horse h : horsesInOrder) {
            series.getData().add(new XYChart.Data<>(h.name(), h.seconds()));
        }
        barChart.getData().add(series);

        barChart.setBarGap(0);
        barChart.setCategoryGap(60);

        int maxTime = winningHorses.stream().mapToInt(Horse::seconds).max().orElse(90);
        if (yAxis != null) {
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(Math.ceil((maxTime + 5) / 10.0) * 10);
            yAxis.setTickUnit(10);
        }

        if (resizeListener != null && barChart != null) {
            barChart.widthProperty().removeListener(resizeListener);
            barChart.heightProperty().removeListener(resizeListener);
        }
        resizeListener = (o, oldVal, newVal) -> Platform.runLater(this::updateBarsAndLabels);
        if (barChart != null) {
            barChart.widthProperty().addListener(resizeListener);
            barChart.heightProperty().addListener(resizeListener);
        }
        // Multiple delayed passes so chart is fully laid out before positioning labels
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(80));
            pause.setOnFinished(e -> updateBarsAndLabels());
            pause.play();
        }));
    }

    private void updateBarsAndLabels() {
        if (barChart == null || chartOverlay == null || horsesInOrder.isEmpty()) return;
        chartOverlay.getChildren().clear();

        int idx = 0;
        for (XYChart.Series<String, Number> s : barChart.getData()) {
            for (XYChart.Data<String, Number> data : s.getData()) {
                if (idx >= horsesInOrder.size()) break;
                javafx.scene.Node bar = data.getNode();
                Horse h = horsesInOrder.get(idx);
                if (bar != null) {
                    bar.getStyleClass().removeIf(c -> c.startsWith("horse-color-"));
                    bar.getStyleClass().add("horse-color-" + h.group().ordinal());
                }
                if (bar != null && bar.getParent() != null) {
                    Bounds barBounds = bar.localToScene(bar.getBoundsInLocal());
                    javafx.geometry.Bounds overlayBounds = chartOverlay.sceneToLocal(barBounds);
                    int time = h.seconds();
                    Label lbl = new Label(time + "s");
                    lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c2416;");
                    chartOverlay.getChildren().add(lbl);
                    lbl.applyCss();
                    double centerX = overlayBounds.getMinX() + overlayBounds.getWidth() / 2;
                    double barTop = overlayBounds.getMinY();
                    double lw = lbl.prefWidth(-1) > 0 ? lbl.prefWidth(-1) : 28;
                    double lh = lbl.prefHeight(-1) > 0 ? lbl.prefHeight(-1) : 18;
                    lbl.setLayoutX(centerX - lw / 2);
                    lbl.setLayoutY(barTop - lh - 4);
                }
                idx++;
            }
        }
    }
}
