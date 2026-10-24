package org.example.horse_racing.ui.views;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.*;
import org.example.horse_racing.service.FileExportService;
import org.example.horse_racing.service.RaceService;
import org.example.horse_racing.ui.AppShell.Navigator;
import org.example.horse_racing.ui.Page;
import org.example.horse_racing.ui.components.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Race Day: draw a field (one horse per group), watch the animated race with live standings,
 * then review the podium and finishing times.
 */
public class RaceView extends BaseView {

    private enum State { NOT_READY, NO_DRAW, DRAWN, RUNNING, FINISHED }

    /** Seconds of real time the winner takes to reach the line at 1× speed. */
    private static final double PLAYBACK_WINNER_SECONDS = 8.0;
    /** Visual exaggeration of finishing gaps so close finishes are still readable. */
    private static final double GAP_EXAGGERATION = 2.4;

    private final Navigator nav;
    private final RaceService raceService = ServiceRegistry.getRaceService();
    private final Random random = new Random();

    private final RaceTrack track = new RaceTrack(RaceService.DISTANCE_METERS);
    private final Label clock = Ui.label("0.00s", "race-clock");
    private final Label status = Ui.badge("Ready", "neutral");
    private final Label sideTitle = Ui.label("The field", "card-title");
    private final Label sideSubtitle = Ui.label("", "card-subtitle");
    private final VBox sideRows = new VBox(4);
    private final VBox notReadyBox = new VBox();
    private final VBox raceArea = new VBox(18);
    private final VBox resultsArea = new VBox(18);
    private final Button drawButton = Ui.button("New draw", Icons.SHUFFLE, "btn-secondary");
    private final Button startButton = Ui.button("Start race", Icons.PLAY, "btn-primary");
    private final ToggleGroup speedGroup = new ToggleGroup();

    private State state = State.NOT_READY;
    private Map<HorseGroup, Horse> field = new EnumMap<>(HorseGroup.class);
    private Map<HorseGroup, Double> odds = new EnumMap<>(HorseGroup.class);
    private RaceResult result;
    private AnimationTimer timer;
    private List<Integer> lastOrder = List.of();

    public RaceView(Navigator nav) {
        this.nav = nav;

        drawButton.setOnAction(e -> {
            drawField();
            if (state == State.DRAWN) Toasts.info("New field drawn — the runners are at the gate.");
        });
        startButton.setOnAction(e -> startOrStop());
        ViewHeader header = new ViewHeader("Race Day",
                RaceService.DISTANCE_METERS + "m sprint · one runner drawn from each group · odds reflect form and age",
                drawButton, startButton);

        // Track card
        ToggleButton s1 = speedToggle("1×", 1), s2 = speedToggle("2×", 2), s4 = speedToggle("4×", 4);
        s1.setSelected(true);
        Ui.requireSelection(speedGroup);
        HBox speed = Ui.segmented(s1, s2, s4);
        Ui.tip(s1, "Normal speed");
        Ui.tip(s4, "Fast forward");

        Label clockCaption = Ui.label("RACE CLOCK", "eyebrow");
        VBox clockBox = new VBox(0, clockCaption, clock);
        HBox trackHeader = Ui.hbox(14, clockBox, status, Ui.hspacer(), Icons.of(Icons.SPEED, 18), speed);
        trackHeader.getStyleClass().add("track-header");
        StackPane trackHolder = new StackPane(track);
        trackHolder.setStyle("-fx-padding: 0 14 14 14;");
        VBox trackCard = new VBox(trackHeader, trackHolder);
        trackCard.getStyleClass().addAll("card", "track-card");

        // Side card (field before the race, live standings during, final order after)
        VBox sideCard = Ui.card(new VBox(2, sideTitle, sideSubtitle), sideRows);
        sideCard.setSpacing(12);

        AdaptiveSplit split = new AdaptiveSplit(trackCard, sideCard, 1020, 330, 18);
        raceArea.getChildren().add(split);

        resultsArea.setVisible(false);
        resultsArea.setManaged(false);

        page.getChildren().addAll(header, notReadyBox, raceArea, resultsArea);

        // Keep the whole track visible on short / wide screens: cap its height to the window.
        javafx.beans.value.ChangeListener<Number> fit = (ob, a, h) -> track.setMaxTrackHeight(h.doubleValue() - 270);
        root().sceneProperty().addListener((o, oldScene, scene) -> {
            if (oldScene != null) oldScene.heightProperty().removeListener(fit);
            if (scene != null) {
                scene.heightProperty().addListener(fit);
                track.setMaxTrackHeight(scene.getHeight() - 270);
            }
        });
    }

    private ToggleButton speedToggle(String text, double factor) {
        ToggleButton t = Ui.toggle(text, null, speedGroup);
        t.setUserData(factor);
        return t;
    }

    private double speed() {
        return speedGroup.getSelectedToggle() == null ? 1 : (double) speedGroup.getSelectedToggle().getUserData();
    }

    @Override
    public void onShow() {
        if (state == State.RUNNING) return;
        boolean ready = raceService.canSelectHorses();
        if (!ready) {
            setState(State.NOT_READY);
        } else if (state == State.NOT_READY || !fieldStillValid()) {
            clearField();
        } else {
            setState(state); // refresh labels
        }
    }

    /** Field horses may have been edited or deleted on the Horses page. */
    private boolean fieldStillValid() {
        if (field.size() != 4) return false;
        for (Horse h : field.values()) {
            var current = ServiceRegistry.getHorseService().getHorseById(h.id());
            if (current.isEmpty() || current.get().group() != h.group()) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ actions

    /** Horses stay hidden until the user presses "New draw". */
    private void clearField() {
        field = new EnumMap<>(HorseGroup.class);
        odds = new EnumMap<>(HorseGroup.class);
        result = null;
        track.setRunning(false);
        track.setLanes(List.of());
        track.redraw();
        clock.setText("0.00s");
        setState(State.NO_DRAW);
    }

    private void drawField() {
        if (state == State.RUNNING) return;
        try {
            field = raceService.selectHorsesForRace();
        } catch (IllegalStateException ex) {
            setState(State.NOT_READY);
            return;
        }
        odds = raceService.odds(field);
        result = null;
        List<RaceTrack.Lane> lanes = new ArrayList<>();
        for (HorseGroup g : HorseGroup.values()) {
            Horse h = field.get(g);
            lanes.add(new RaceTrack.Lane(g, h.name(), h.jockey() + "  ·  odds " + String.format("%.2f", odds.get(g))));
        }
        track.setRunning(false);
        track.setLanes(lanes);
        clock.setText("0.00s");
        setState(State.DRAWN);
    }

    public void startOrStop() {
        switch (state) {
            case RUNNING -> stopRace();
            case NO_DRAW -> Toasts.info("Click \"New draw\" first to pick the runners.");
            case DRAWN -> startRace();
            case FINISHED -> {
                resetLanes();
                startRace();
            }
            case NOT_READY -> nav.go(Page.HORSES);
        }
    }

    private void resetLanes() {
        for (RaceTrack.Lane l : track.lanes()) {
            l.progress = 0;
            l.rank = 0;
            l.timeLabel = "";
        }
    }

    private void startRace() {
        if (field.size() != 4) return;
        result = raceService.runRace(field);
        Map<Integer, RaceEntry> byLane = result.entries().stream().collect(Collectors.toMap(RaceEntry::lane, e -> e));
        double winnerT = result.winner().time();
        double scale = PLAYBACK_WINNER_SECONDS / winnerT;

        int n = track.lanes().size();
        double[] visualT = new double[n];
        double[] amp = new double[n], freq = new double[n], phi = new double[n];
        for (int i = 0; i < n; i++) {
            double t = byLane.get(i).time();
            visualT[i] = (winnerT + (t - winnerT) * GAP_EXAGGERATION) * scale;
            amp[i] = (random.nextDouble() * 2 - 1) * 0.14;   // front-runner (+) or closer (−)
            freq[i] = 0.7 + random.nextDouble() * 0.9;
            phi[i] = random.nextDouble() * Math.PI;
        }
        resetLanes();
        lastOrder = List.of();
        track.setRunning(true);
        setState(State.RUNNING);

        final int[] finishedCount = {0};
        timer = new AnimationTimer() {
            private long last = -1;
            private double elapsed = 0;

            @Override
            public void handle(long now) {
                if (last < 0) last = now;
                double dt = Math.min(0.05, (now - last) / 1e9);
                last = now;
                elapsed += dt * speed();

                for (int i = 0; i < n; i++) {
                    RaceTrack.Lane lane = track.lanes().get(i);
                    if (lane.rank > 0) continue;
                    double u = elapsed / visualT[i];
                    if (u >= 1) {
                        lane.progress = 1;
                        RaceEntry e = byLane.get(i);
                        lane.rank = e.rank();
                        lane.timeLabel = e.formattedTime();
                        finishedCount[0]++;
                        if (e.rank() == 1) clock.setText(e.formattedTime());
                    } else {
                        // Smooth surges and fades around a steady pace; always ends exactly at u=1.
                        double start = Math.min(1, u / 0.06);               // quick break from the gate
                        double pace = u * (0.85 + 0.15 * start);
                        lane.progress = pace + amp[i] * u * (1 - u) * Math.sin(2 * Math.PI * freq[i] * u + phi[i]);
                        lane.progress = Math.max(0, Math.min(0.999, lane.progress));
                        lane.phase += dt * speed() * 22;
                    }
                }
                if (track.lanes().stream().noneMatch(l -> l.rank == 1)) {
                    clock.setText(String.format("%.2fs", elapsed / scale));
                }
                updateStandings();
                track.redraw();
                if (finishedCount[0] == n) {
                    stop();
                    finishRace();
                }
            }
        };
        timer.start();
    }

    private void stopRace() {
        if (timer != null) timer.stop();
        timer = null;
        result = null;
        resetLanes();
        track.setRunning(false);
        track.redraw();
        clock.setText("0.00s");
        setState(State.DRAWN);
        Toasts.info("Race stopped. The field is back at the gate.");
    }

    private void finishRace() {
        timer = null;
        track.setRunning(false);
        track.redraw();
        ServiceRegistry.getRaceHistoryService().record(result);
        setState(State.FINISHED);
        RaceEntry w = result.winner();
        Toasts.success(w.horse().name() + " wins in " + w.formattedTime() + "!");
    }

    // ------------------------------------------------------------------ state & side panel

    private void setState(State s) {
        state = s;
        boolean notReady = s == State.NOT_READY;
        show(notReadyBox, notReady);
        show(raceArea, !notReady);
        show(resultsArea, s == State.FINISHED);
        drawButton.setDisable(s == State.RUNNING || notReady);

        status.getStyleClass().removeAll("neutral", "ok", "warn");
        switch (s) {
            case NOT_READY -> {
                buildNotReady();
                startButton.setText("Add horses");
                startButton.setGraphic(Icons.of(Icons.ADD, 16));
            }
            case NO_DRAW -> {
                status.setText("Awaiting draw");
                status.getStyleClass().add("neutral");
                startButton.setText("Start race");
                startButton.setGraphic(Icons.of(Icons.PLAY, 16));
                sideTitle.setText("The field");
                sideSubtitle.setText("No runners drawn yet");
                Label hint = Ui.label("Click \"New draw\" to pick the runners before starting the race.", "field-hint");
                hint.setWrapText(true);
                sideRows.getChildren().setAll(hint);
            }
            case DRAWN -> {
                status.setText("At the gate");
                status.getStyleClass().add("neutral");
                startButton.setText("Start race");
                startButton.setGraphic(Icons.of(Icons.PLAY, 16));
                showField();
            }
            case RUNNING -> {
                status.setText("● Live");
                status.getStyleClass().add("warn");
                startButton.setText("Stop");
                startButton.setGraphic(Icons.of(Icons.STOP, 16));
                sideTitle.setText("Live standings");
                sideSubtitle.setText("Updated as the field moves");
            }
            case FINISHED -> {
                status.setText("Result official");
                status.getStyleClass().add("ok");
                startButton.setText("Race again");
                startButton.setGraphic(Icons.of(Icons.REPLAY, 16));
                showFinalOrder();
                buildResults();
            }
        }
    }

    private void buildNotReady() {
        Set<HorseGroup> empty = raceService.getEmptyGroups();
        String missing = empty.stream().map(HorseGroup::toString).collect(Collectors.joining(", "));
        Button go = Ui.button("Go to Horses", Icons.HORSES, "btn-primary");
        go.setOnAction(e -> nav.go(Page.HORSES));
        VBox es = Widgets.emptyState(Icons.FLAG, "Not enough runners",
                "Every race needs one horse from each group. Add a horse to " + missing + " to unlock Race Day.", go);
        notReadyBox.getChildren().setAll(Ui.card(es));
    }

    private void showField() {
        sideTitle.setText("The field");
        sideSubtitle.setText("Decimal odds · lower means more fancied");
        sideRows.getChildren().clear();
        for (HorseGroup g : HorseGroup.values()) {
            Horse h = field.get(g);
            if (h == null) continue;
            RaceRecord r = RaceRecord.parse(h.raceRecord());
            Label name = Ui.label(h.name(), "runner-name");
            name.setMinWidth(0);
            Label meta = Ui.label(h.jockey() + " · " + r.wins() + "W " + r.losses() + "L · age " + h.age(), "runner-meta");
            meta.setMinWidth(0);
            VBox text = new VBox(2, name, meta);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label o = Ui.label(String.format("%.2f", odds.getOrDefault(g, 0.0)), "odds");
            o.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = Ui.hbox(12, Ui.laneChip(g), text, o);
            row.getStyleClass().add("runner-row");
            sideRows.getChildren().add(row);
        }
        Label tip = Ui.label("Tip: press Ctrl+R to start, change speed any time.", "field-hint");
        tip.setWrapText(true);
        tip.setStyle("-fx-padding: 8 4 0 4;");
        sideRows.getChildren().add(tip);
    }

    private void updateStandings() {
        List<RaceTrack.Lane> lanes = track.lanes();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < lanes.size(); i++) order.add(i);
        order.sort((a, b) -> {
            RaceTrack.Lane la = lanes.get(a), lb = lanes.get(b);
            if (la.rank > 0 || lb.rank > 0) {
                if (la.rank > 0 && lb.rank > 0) return Integer.compare(la.rank, lb.rank);
                return la.rank > 0 ? -1 : 1;
            }
            return Double.compare(lb.progress, la.progress);
        });
        boolean finishedChanged = lanes.stream().mapToInt(l -> l.rank).sum() != (int) sideRows.getProperties().getOrDefault("rankSum", 0);
        if (order.equals(lastOrder) && !finishedChanged) return;
        lastOrder = order;
        sideRows.getProperties().put("rankSum", lanes.stream().mapToInt(l -> l.rank).sum());

        sideRows.getChildren().clear();
        for (int pos = 0; pos < order.size(); pos++) {
            RaceTrack.Lane l = lanes.get(order.get(pos));
            Horse h = field.get(l.group);
            Label p = Ui.label(FileExportService.ordinal(pos + 1), "runner-pos");
            Label name = Ui.label(h.name(), "runner-name");
            name.setMinWidth(0);
            Label meta = Ui.label(h.jockey(), "runner-meta");
            VBox text = new VBox(2, name, meta);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label right = l.rank > 0 ? Ui.label(l.timeLabel, "runner-time") : Ui.label(pos == 0 ? "Leading" : "", "text-subtle", "text-small");
            right.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = Ui.hbox(10, p, Ui.laneChip(l.group), text, right);
            row.getStyleClass().add("runner-row");
            if (pos == 0) row.getStyleClass().add("leader");
            sideRows.getChildren().add(row);
        }
    }

    private void showFinalOrder() {
        sideTitle.setText("Final order");
        sideSubtitle.setText(RaceService.DISTANCE_METERS + "m · winning time " + result.winner().formattedTime());
        sideRows.getChildren().clear();
        for (RaceEntry e : result.entries()) {
            Label p = Ui.label(FileExportService.ordinal(e.rank()), "runner-pos");
            Label name = Ui.label(e.horse().name(), "runner-name");
            name.setMinWidth(0);
            double gap = result.gapToWinner(e);
            Label meta = Ui.label(e.horse().jockey() + (gap > 0 ? String.format(" · +%.2fs", gap) : ""), "runner-meta");
            meta.setMinWidth(0);
            VBox text = new VBox(2, name, meta);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label t = Ui.label(e.formattedTime(), "runner-time");
            t.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = Ui.hbox(10, p, Ui.laneChip(e.horse().group()), text, t);
            row.getStyleClass().add("runner-row");
            if (e.rank() == 1) row.getStyleClass().add("leader");
            sideRows.getChildren().add(row);
        }
    }

    // ------------------------------------------------------------------ results

    private void buildResults() {
        // Podium
        HBox podium = new HBox(10);
        podium.setAlignment(Pos.BOTTOM_CENTER);
        int[] order = {1, 0, 2};          // 2nd, 1st, 3rd
        double[] heights = {78, 108, 58};
        String[] cls = {"p2", "p1", "p3"};
        for (int k = 0; k < 3; k++) {
            RaceEntry e = result.entries().get(order[k]);
            VBox col = new VBox(8);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setMaxWidth(Double.MAX_VALUE);
            col.setMinWidth(0);
            Label nm = Ui.label(e.horse().name(), "podium-name");
            nm.setWrapText(true);
            nm.setMaxWidth(Double.MAX_VALUE);
            nm.setAlignment(Pos.CENTER);
            Label tm = Ui.label(e.formattedTime(), "runner-time");
            VBox block = new VBox(Ui.label(String.valueOf(e.rank()), "podium-rank"));
            block.getStyleClass().addAll("podium-block", cls[k]);
            block.setMinHeight(heights[k]);
            block.setPrefHeight(heights[k]);
            block.setMaxWidth(Double.MAX_VALUE);
            StackPane av = Ui.avatar(e.horse(), k == 1 ? 58 : 46);
            if (k == 1) {
                StackPane trophy = new StackPane(Icons.of(Icons.TROPHY, 18));
                trophy.getStyleClass().addAll("stat-icon", "tone-accent");
                col.getChildren().add(trophy);
            }
            col.getChildren().addAll(av, nm, tm, block);
            podium.getChildren().add(col);
        }
        RaceEntry fourth = result.entries().get(3);
        Label fourthLbl = Ui.label("4th  " + fourth.horse().name() + "  ·  " + fourth.formattedTime()
                + String.format("  (+%.2fs)", result.gapToWinner(fourth)), "text-muted", "text-small");
        Button again = Ui.button("Race same field", Icons.REPLAY, "btn-secondary", "btn-small");
        again.setOnAction(e -> startOrStop());
        Button newDraw = Ui.button("New draw", Icons.SHUFFLE, "btn-primary", "btn-small");
        newDraw.setOnAction(e -> drawField());
        VBox podiumCard = Ui.cardWithHeader("Podium", "Congratulations to " + result.winner().horse().jockey(), null,
                podium, fourthLbl, Ui.hbox(8, again, newDraw));

        // Times chart
        VBox bars = new VBox(14);
        double best = result.winner().time();
        double worst = result.entries().get(result.entries().size() - 1).time();
        double floor = best - Math.max(1.0, (worst - best) * 1.5);
        for (RaceEntry e : result.entries()) {
            double frac = (e.time() - floor) / (worst - floor);
            Label n = Ui.label(e.horse().name(), "text-strong");
            n.setMinWidth(0);
            double gap = result.gapToWinner(e);
            Label t = Ui.label(e.formattedTime() + (gap > 0 ? String.format("  +%.2f", gap) : ""), "runner-time");
            t.setMinWidth(Region.USE_PREF_SIZE);
            HBox top = Ui.hbox(8, Ui.dot(e.horse().group()), n, Ui.hspacer(), t);
            bars.getChildren().add(new VBox(6, top, Widgets.bar(frac, e.horse().group(), true)));
        }
        Label note = Ui.label("Shorter bar = faster. Odds before the off: " + result.byLane().stream()
                .map(e -> e.horse().group().name() + " " + String.format("%.2f", e.odds())).collect(Collectors.joining(" · ")), "field-hint");
        note.setWrapText(true);
        VBox timesCard = Ui.cardWithHeader("Finishing times", "Gap to the winner in seconds", null, bars, note);

        ResponsiveGrid grid = new ResponsiveGrid(360, 18, podiumCard, timesCard);
        resultsArea.getChildren().setAll(Ui.label("Results", "section-title"), grid);
    }

    private static void show(javafx.scene.Node n, boolean v) {
        n.setVisible(v);
        n.setManaged(v);
    }
}
