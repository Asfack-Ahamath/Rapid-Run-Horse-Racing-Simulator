package org.example.horse_racing.ui.views;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceRecord;
import org.example.horse_racing.domain.RaceResult;
import org.example.horse_racing.service.RaceHistoryService;
import org.example.horse_racing.ui.AppShell.Navigator;
import org.example.horse_racing.ui.Page;
import org.example.horse_racing.ui.components.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Overview: stable size, race readiness, roster balance and recent results. */
public class DashboardView extends BaseView {

    private final Navigator nav;
    private final RaceHistoryService history = ServiceRegistry.getRaceHistoryService();

    private final Widgets.StatTile horsesTile = new Widgets.StatTile("Horses in stable", Icons.USERS, null);
    private final Widgets.StatTile readyTile = new Widgets.StatTile("Race readiness", Icons.FLAG, "tone-blue");
    private final Widgets.StatTile racesTile = new Widgets.StatTile("Races run", Icons.TIMER, "tone-accent");
    private final Widgets.StatTile topTile = new Widgets.StatTile("Top performer", Icons.TROPHY, "tone-red");

    private final VBox rosterBody = new VBox(14);
    private final VBox recentBody = new VBox(4);
    private final HBox banner = new HBox();

    public DashboardView(Navigator nav) {
        this.nav = nav;

        Button addHorse = Ui.button("Add horse", Icons.ADD, "btn-secondary");
        addHorse.setOnAction(e -> {
            nav.go(Page.HORSES);
            HorsesView.requestAddDialog();
        });
        Button race = Ui.button("Go to Race Day", Icons.FLAG, "btn-primary");
        race.setOnAction(e -> nav.go(Page.RACE));

        ViewHeader header = new ViewHeader("Dashboard", "Your stable at a glance — roster balance, readiness and recent results.", addHorse, race);

        ResponsiveGrid stats = new ResponsiveGrid(210, horsesTile, readyTile, racesTile, topTile);
        stats.setBalanced(true);

        Button viewAll = Ui.button("All horses", null, "btn-ghost", "btn-small");
        viewAll.setOnAction(e -> nav.go(Page.HORSES));
        VBox rosterCard = Ui.cardWithHeader("Roster by group", "One horse from each group is drawn into every race", viewAll, rosterBody);
        VBox.setVgrow(rosterBody, Priority.ALWAYS);

        Button history = Ui.button("Statistics", null, "btn-ghost", "btn-small");
        history.setOnAction(e -> nav.go(Page.STATS));
        VBox recentCard = Ui.cardWithHeader("Recent races", "Latest results, newest first", history, recentBody);

        ResponsiveGrid cards = new ResponsiveGrid(380, 18, rosterCard, recentCard);

        banner.getStyleClass().add("banner");
        page.getChildren().addAll(header, banner, stats, cards);

        this.history.addListener(this::refresh);
    }

    @Override
    public void onShow() {
        refresh();
    }

    private void refresh() {
        List<Horse> all = ServiceRegistry.getHorseService().getAllHorses();
        Map<HorseGroup, Long> counts = new EnumMap<>(HorseGroup.class);
        for (HorseGroup g : HorseGroup.values()) counts.put(g, 0L);
        all.forEach(h -> counts.merge(h.group(), 1L, Long::sum));
        Set<HorseGroup> empty = ServiceRegistry.getRaceService().getEmptyGroups();

        horsesTile.set(String.valueOf(all.size()), "Across " + (4 - empty.size()) + " of 4 groups");
        if (empty.isEmpty()) {
            readyTile.set("Ready", "Every group has at least one horse");
        } else {
            readyTile.set("Not ready", "Missing: " + empty.stream().map(HorseGroup::toString).collect(Collectors.joining(", ")));
        }

        var fastest = history.fastestTime();
        racesTile.set(String.valueOf(history.count()),
                fastest.isPresent() ? String.format("Fastest winning time %.2fs", fastest.getAsDouble()) : "No races yet");
        var board = history.leaderboard();
        if (board.isEmpty()) {
            topTile.set("—", "Run a race to crown a champion");
        } else {
            var top = board.get(0);
            topTile.set(top.getKey(), top.getValue() + (top.getValue() == 1 ? " win" : " wins") + " on record");
        }

        // Banner
        banner.getChildren().clear();
        banner.getStyleClass().removeAll("ok", "warn");
        if (empty.isEmpty()) {
            banner.getStyleClass().add("ok");
            Label l = Ui.label("All four groups are filled — you're ready to race.");
            l.setWrapText(true);
            HBox.setHgrow(l, Priority.ALWAYS);
            Button go = Ui.button("Start a race", Icons.PLAY, "btn-primary", "btn-small");
            go.setOnAction(e -> nav.go(Page.RACE));
            banner.getChildren().addAll(Icons.of(Icons.CHECK_CIRCLE, 20), l, go);
        } else {
            banner.getStyleClass().add("warn");
            Label l = Ui.label("Add at least one horse to " + empty.stream().map(HorseGroup::toString).collect(Collectors.joining(", "))
                    + " before you can race.");
            l.setWrapText(true);
            HBox.setHgrow(l, Priority.ALWAYS);
            Button add = Ui.button("Add horse", Icons.ADD, "btn-secondary", "btn-small");
            add.setOnAction(e -> {
                nav.go(Page.HORSES);
                HorsesView.requestAddDialog();
            });
            banner.getChildren().addAll(Icons.of(Icons.WARNING, 20), l, add);
        }

        // Roster bars
        rosterBody.getChildren().clear();
        long max = Math.max(1, counts.values().stream().mapToLong(Long::longValue).max().orElse(1));
        for (HorseGroup g : HorseGroup.values()) {
            long c = counts.get(g);
            Label name = Ui.label(g.toString(), "text-strong");
            name.setMinWidth(70);
            Label count = Ui.label(c + (c == 1 ? " horse" : " horses"), "text-muted", "text-small");
            count.setMinWidth(64);
            count.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            HBox row = Ui.hbox(12, Ui.dot(g), name, Widgets.bar((double) c / max, g, false), count);
            rosterBody.getChildren().add(row);
        }
        if (!all.isEmpty()) {
            double avgAge = all.stream().mapToInt(Horse::age).average().orElse(0);
            double avgWin = all.stream().mapToDouble(h -> RaceRecord.parse(h.raceRecord()).winRate()).average().orElse(0);
            var raceService = ServiceRegistry.getRaceService();
            Horse best = all.stream().max(java.util.Comparator.comparingDouble(raceService::rating)).orElse(all.get(0));
            HBox summary = new HBox(8,
                    miniMetric(String.format("%.1f", avgAge), "Avg age"),
                    miniMetric(Math.round(avgWin * 100) + "%", "Avg win rate"),
                    miniMetric(best.name(), "Highest rated"));
            Region grow = Ui.vspacer();
            rosterBody.getChildren().addAll(grow, Ui.divider(), summary);
        }

        // Recent races
        recentBody.getChildren().clear();
        List<RaceResult> races = history.all();
        if (races.isEmpty()) {
            Button go = Ui.button("Run your first race", Icons.PLAY, "btn-primary", "btn-small");
            go.setOnAction(e -> nav.go(Page.RACE));
            recentBody.getChildren().add(Widgets.emptyState(Icons.HISTORY, "No races yet",
                    "Results will appear here after each race.", go));
        } else {
            for (RaceResult r : races.subList(0, Math.min(5, races.size()))) {
                recentBody.getChildren().add(raceRow(r));
            }
        }
    }

    private static VBox miniMetric(String value, String label) {
        Label v = Ui.label(value, "metric-value");
        v.setMinWidth(0);
        VBox box = new VBox(1, v, Ui.label(label, "metric-label"));
        box.getStyleClass().add("metric-box");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinWidth(0);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    static HBox raceRow(RaceResult r) {
        var w = r.winner();
        Label name = Ui.label(w.horse().name(), "runner-name");
        name.setMinWidth(0);
        Label meta = Ui.label(w.horse().jockey() + " · " + Widgets.relative(r.runAt()), "runner-meta");
        meta.setMinWidth(0);
        VBox text = new VBox(2, name, meta);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label time = Ui.label(w.formattedTime(), "runner-time");
        time.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = Ui.hbox(12, Ui.laneChip(w.horse().group()), text, Ui.groupBadge(w.horse().group()), time);
        row.getStyleClass().add("runner-row");
        return row;
    }
}
