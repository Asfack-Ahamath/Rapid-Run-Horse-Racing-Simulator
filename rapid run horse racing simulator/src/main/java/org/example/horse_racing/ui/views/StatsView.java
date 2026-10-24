package org.example.horse_racing.ui.views;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceResult;
import org.example.horse_racing.service.RaceHistoryService;
import org.example.horse_racing.ui.AppShell.Navigator;
import org.example.horse_racing.ui.Page;
import org.example.horse_racing.ui.components.*;

import java.util.List;
import java.util.Map;

/** Leaderboard, group win share and the full race log. */
public class StatsView extends BaseView {

    private final Navigator nav;
    private final RaceHistoryService history = ServiceRegistry.getRaceHistoryService();
    private final VBox content = new VBox(18);
    private final Button clear = Ui.button("Clear history", Icons.DELETE, "btn-secondary");

    public StatsView(Navigator nav) {
        this.nav = nav;
        clear.setOnAction(e -> Overlay.confirm("Clear race history?",
                "All " + history.count() + " recorded races and the leaderboard will be erased. Your horses are not affected.",
                "Clear history", true, () -> {
                    history.clear();
                    Toasts.success("Race history cleared.");
                }));
        ViewHeader header = new ViewHeader("Statistics", "Who's winning, which groups dominate, and every race on record.", clear);
        page.getChildren().addAll(header, content);
        history.addListener(this::refresh);
    }

    @Override
    public void onShow() {
        refresh();
    }

    private void refresh() {
        content.getChildren().clear();
        clear.setDisable(history.count() == 0);
        if (history.count() == 0) {
            Button go = Ui.button("Go to Race Day", Icons.FLAG, "btn-primary");
            go.setOnAction(e -> nav.go(Page.RACE));
            content.getChildren().add(Ui.card(Widgets.emptyState(Icons.CHART, "No statistics yet",
                    "Run a few races and the leaderboard, group share and race log will fill in here.", go)));
            return;
        }

        // Leaderboard
        VBox board = new VBox(4);
        List<Map.Entry<String, Integer>> top = history.leaderboard();
        int maxWins = top.get(0).getValue();
        for (int i = 0; i < Math.min(8, top.size()); i++) {
            var e = top.get(i);
            Label pos = Ui.label(String.valueOf(i + 1), "runner-pos");
            Label name = Ui.label(e.getKey(), "runner-name");
            name.setMinWidth(0);
            name.setPrefWidth(150);
            Region barFill = new Region();
            barFill.getStyleClass().addAll("bar");
            barFill.setStyle("-fx-background-color: -rr-primary;");
            Region track = new Region();
            track.getStyleClass().add("track");
            StackPane bar = new StackPane(track, barFill);
            bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            barFill.maxWidthProperty().bind(bar.widthProperty().multiply((double) e.getValue() / maxWins));
            track.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            bar.setMinWidth(40);
            Label wins = Ui.label(e.getValue() + (e.getValue() == 1 ? " win" : " wins"), "runner-time");
            wins.setMinWidth(64);
            wins.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            HBox row = Ui.hbox(10, pos, name, bar, wins);
            row.getStyleClass().add("runner-row");
            board.getChildren().add(row);
        }
        VBox boardCard = Ui.cardWithHeader("Leaderboard", "Most wins across " + history.count() + " races", null, board);

        // Group share
        VBox share = new VBox(14);
        Map<HorseGroup, Integer> byGroup = history.winsByGroup();
        int total = history.count();
        for (HorseGroup g : HorseGroup.values()) {
            int w = byGroup.get(g);
            Label name = Ui.label(g.toString(), "text-strong");
            name.setMinWidth(70);
            Label pct = Ui.label(Math.round(100.0 * w / total) + "%  ·  " + w, "runner-time");
            pct.setMinWidth(76);
            pct.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            share.getChildren().add(Ui.hbox(12, Ui.dot(g), name, Widgets.bar((double) w / total, g, false), pct));
        }
        var fastest = history.fastestTime();
        Label fastestLbl = Ui.label(fastest.isPresent() ? String.format("Fastest winning time: %.2fs", fastest.getAsDouble()) : "",
                "field-hint");
        VBox shareCard = Ui.cardWithHeader("Wins by group", "Share of races won by each group's runner", null, share, fastestLbl);

        ResponsiveGrid top2 = new ResponsiveGrid(380, 18, boardCard, shareCard);

        // Charts
        PieChart pie = new PieChart();
        pie.setLegendVisible(true);
        pie.setLabelsVisible(false);
        pie.setAnimated(false);
        pie.setMinHeight(260);
        pie.setPrefHeight(260);
        for (HorseGroup g : HorseGroup.values()) {
            int w = byGroup.get(g);
            if (w == 0) continue;
            PieChart.Data d = new PieChart.Data(g + " (" + w + ")", w);
            pie.getData().add(d);
            String cls = Ui.groupClass(g);
            d.nodeProperty().addListener((o, a, n) -> { if (n != null) n.setStyle("-fx-pie-color: -rr-" + cls + ";"); });
            if (d.getNode() != null) d.getNode().setStyle("-fx-pie-color: -rr-" + cls + ";");
        }
        VBox pieCard = Ui.cardWithHeader("Win distribution", "Wins by group as a share of all races", null, pie);

        List<RaceResult> all = history.all();
        int shown = Math.min(30, all.size());
        NumberAxis xAxis = new NumberAxis(1, Math.max(2, shown), shown > 10 ? 5 : 1);
        xAxis.setLabel("Race (oldest → newest)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(false);
        yAxis.setLabel("Winning time (s)");
        LineChart<Number, Number> line = new LineChart<>(xAxis, yAxis);
        line.setLegendVisible(false);
        line.setAnimated(false);
        line.setMinHeight(260);
        line.setPrefHeight(260);
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < shown; i++) {
            RaceResult r = all.get(shown - 1 - i);
            series.getData().add(new XYChart.Data<>(i + 1, r.winner().time()));
        }
        line.getData().add(series);
        VBox lineCard = Ui.cardWithHeader("Winning times", "Winning time of the last " + shown + " races", null, line);

        ResponsiveGrid charts = new ResponsiveGrid(380, 18, pieCard, lineCard);

        // Race log
        VBox log = new VBox(4);
        List<RaceResult> races = history.all();
        for (RaceResult r : races.subList(0, Math.min(25, races.size()))) {
            log.getChildren().add(DashboardView.raceRow(r));
        }
        String sub = races.size() > 25 ? "Showing the latest 25 of " + races.size() : races.size() + " races, newest first";
        VBox logCard = Ui.cardWithHeader("Race log", sub, null, log);

        content.getChildren().addAll(top2, charts, logCard);
    }
}
