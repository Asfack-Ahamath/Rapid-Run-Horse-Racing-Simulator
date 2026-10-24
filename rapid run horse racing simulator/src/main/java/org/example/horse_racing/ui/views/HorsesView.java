package org.example.horse_racing.ui.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceRecord;
import org.example.horse_racing.service.HorseService;
import org.example.horse_racing.ui.components.*;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Roster management: searchable, filterable grid or table of horses with add/edit/delete. */
public class HorsesView extends BaseView {

    private static HorsesView instance;

    private final HorseService service = ServiceRegistry.getHorseService();
    private final ObservableList<Horse> visible = FXCollections.observableArrayList();

    private final TextField search = new TextField();
    private final ToggleGroup filterGroup = new ToggleGroup();
    private final ComboBox<String> sort = new ComboBox<>();
    private final ToggleGroup viewGroup = new ToggleGroup();
    private final ToggleButton gridToggle = Ui.toggle(null, Icons.GRID, viewGroup);
    private final ToggleButton tableToggle = Ui.toggle(null, Icons.ROWS, viewGroup);

    private final ResponsiveGrid grid = new ResponsiveGrid(270, 16);
    private final TableView<Horse> table = new TableView<>(visible);
    private final StackPane body = new StackPane();
    private final Label countLabel = Ui.label("", "text-muted", "text-small");
    private final ViewHeader header;
    private TableColumn<Horse, String> jockeyCol, breedCol, recordCol, ageCol;

    public HorsesView() {
        instance = this;

        Button export = Ui.button("Export report", Icons.DOWNLOAD, "btn-secondary");
        export.setOnAction(e -> exportReport());
        Button add = Ui.button("Add horse", Icons.ADD, "btn-primary");
        add.setOnAction(e -> openAddDialog());
        header = new ViewHeader("Horses", "Manage your roster.", export, add);

        page.getChildren().addAll(header, buildToolbar(), body);
        buildTable();
        gridToggle.setSelected(true);
        Ui.tip(gridToggle, "Card view");
        Ui.tip(tableToggle, "Table view");
        viewGroup.selectedToggleProperty().addListener(o -> render());
        Ui.requireSelection(viewGroup);
    }

    /** Called from other pages (dashboard quick action). */
    static void requestAddDialog() {
        if (instance != null) Platform.runLater(instance::openAddDialog);
    }

    public void openAddDialog() {
        if (!Overlay.isOpen()) HorseFormDialog.open(null, this::refresh);
    }

    public void focusSearch() {
        Platform.runLater(search::requestFocus);
    }

    @Override
    public void onShow() {
        refresh();
    }

    // ------------------------------------------------------------------ toolbar

    private Pane buildToolbar() {
        search.setPromptText("Search name, jockey, breed or ID");
        search.textProperty().addListener(o -> applyFilter());
        HBox searchBox = new HBox(Icons.of(Icons.SEARCH, 18), search);
        searchBox.getStyleClass().add("search-box");
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPrefWidth(300);
        searchBox.setMinWidth(220);

        ToggleButton all = Ui.toggle("All", null, filterGroup);
        all.setUserData(null);
        all.setSelected(true);
        HBox filters = Ui.segmented(all);
        for (HorseGroup g : HorseGroup.values()) {
            ToggleButton t = Ui.toggle(g.name(), null, filterGroup);
            t.setGraphic(Ui.dot(g));
            t.setUserData(g);
            Ui.tip(t, "Show " + g + " only");
            filters.getChildren().add(t);
        }
        Ui.requireSelection(filterGroup);
        filterGroup.selectedToggleProperty().addListener(o -> applyFilter());

        sort.getItems().setAll("Sort: ID", "Sort: Name", "Sort: Win rate", "Sort: Age", "Sort: Group");
        sort.getSelectionModel().selectFirst();
        sort.valueProperty().addListener(o -> applyFilter());
        sort.setMinWidth(Region.USE_PREF_SIZE);

        HBox viewSwitch = Ui.segmented(gridToggle, tableToggle);

        FlowPane bar = new FlowPane(10, 10, searchBox, filters, sort, viewSwitch, countLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setRowValignment(javafx.geometry.VPos.CENTER);
        return bar;
    }

    // ------------------------------------------------------------------ data

    private void refresh() {
        applyFilter();
    }

    private void applyFilter() {
        List<Horse> allHorses = service.getAllHorses();
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        Toggle ft = filterGroup.getSelectedToggle();
        HorseGroup g = ft == null ? null : (HorseGroup) ft.getUserData();

        Comparator<Horse> cmp = switch (sort.getSelectionModel().getSelectedIndex()) {
            case 1 -> Comparator.comparing(h -> h.name().toLowerCase(Locale.ROOT));
            case 2 -> Comparator.comparingDouble((Horse h) -> RaceRecord.parse(h.raceRecord()).winRate()).reversed();
            case 3 -> Comparator.comparingInt(Horse::age);
            case 4 -> Comparator.comparing((Horse h) -> h.group().ordinal()).thenComparingInt(HorsesView::numericId);
            default -> Comparator.comparingInt(HorsesView::numericId);
        };

        List<Horse> filtered = allHorses.stream()
                .filter(h -> g == null || h.group() == g)
                .filter(h -> q.isEmpty() || matches(h, q))
                .sorted(cmp)
                .toList();
        visible.setAll(filtered);
        countLabel.setText(filtered.size() == allHorses.size()
                ? allHorses.size() + " horses"
                : filtered.size() + " of " + allHorses.size() + " horses");
        render();
    }

    private static boolean matches(Horse h, String q) {
        return h.name().toLowerCase(Locale.ROOT).contains(q)
                || h.jockey().toLowerCase(Locale.ROOT).contains(q)
                || h.breed().toLowerCase(Locale.ROOT).contains(q)
                || h.id().toLowerCase(Locale.ROOT).contains(q);
    }

    static int numericId(Horse h) {
        try {
            return Integer.parseInt(h.id().replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        if (visible.isEmpty()) {
            boolean filtering = !search.getText().isBlank() || filterGroup.getSelectedToggle().getUserData() != null;
            Button action;
            if (filtering) {
                action = Ui.button("Clear filters", null, "btn-secondary");
                action.setOnAction(e -> {
                    search.clear();
                    filterGroup.getToggles().get(0).setSelected(true);
                });
            } else {
                action = Ui.button("Add your first horse", Icons.ADD, "btn-primary");
                action.setOnAction(e -> openAddDialog());
            }
            VBox empty = Widgets.emptyState(Icons.SEARCH, filtering ? "No matching horses" : "Your stable is empty",
                    filtering ? "Try a different search or group filter." : "Add horses to each group to start racing.", action);
            VBox card = Ui.card(empty);
            body.getChildren().setAll(card);
            return;
        }
        if (tableToggle.isSelected()) {
            table.refresh();
            body.getChildren().setAll(table);
        } else {
            grid.getChildren().setAll(visible.stream().map(this::horseCard).toList());
            body.getChildren().setAll(grid);
        }
    }

    private StackPane horseCard(Horse h) {
        RaceRecord rec = RaceRecord.parse(h.raceRecord());

        Label name = Ui.label(h.name(), "horse-name");
        name.setMinWidth(0);
        Label sub = Ui.label("#" + h.id() + " · " + h.breed(), "text-muted", "text-small");
        sub.setMinWidth(0);
        VBox titles = new VBox(2, name, sub);
        titles.setMinWidth(0);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox top = Ui.hbox(12, Ui.avatar(h, 46), titles, Ui.groupBadge(h.group()));
        top.setAlignment(Pos.TOP_LEFT);

        HBox metrics = new HBox(8,
                metric(String.valueOf(h.age()), "Age"),
                metric(rec.wins() + "–" + rec.losses(), "W–L"),
                metric(rec.starts() == 0 ? "—" : Math.round(rec.winRate() * 100) + "%", "Win rate"));

        Label jockey = Ui.label(h.jockey(), "text-strong");
        jockey.setMinWidth(0);
        Label jl = Ui.label("Jockey", "text-subtle", "text-small");
        VBox jbox = new VBox(1, jl, jockey);
        jbox.setMinWidth(0);
        HBox.setHgrow(jbox, Priority.ALWAYS);
        Button edit = Ui.iconButton(Icons.EDIT, "Edit");
        edit.setOnAction(e -> HorseFormDialog.open(h, this::refresh));
        Button del = Ui.iconButton(Icons.DELETE, "Delete", "danger");
        del.setOnAction(e -> confirmDelete(h));
        HBox bottom = Ui.hbox(4, jbox, edit, del);

        VBox inner = new VBox(14, top, metrics, Ui.divider(), bottom);
        inner.setStyle("-fx-padding: 16 16 12 16;");
        // Body (stripe + details) is clipped to the card's full outer rounded rectangle, and the
        // border is a separate overlay painted on top, so the colour always follows the curve.
        Region stripe = new Region();
        stripe.getStyleClass().addAll("stripe", Ui.groupClass(h.group()));
        VBox body = new VBox(stripe, inner);
        body.getStyleClass().add("horse-body");
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(body.widthProperty());
        clip.heightProperty().bind(body.heightProperty());
        body.setClip(clip);
        Region border = new Region();
        border.getStyleClass().add("horse-border");
        border.setMouseTransparent(true);
        StackPane card = new StackPane(body, border);
        card.getStyleClass().add("horse-shell");
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) HorseFormDialog.open(h, this::refresh);
        });
        return card;
    }

    private static VBox metric(String value, String label) {
        VBox v = new VBox(1, Ui.label(value, "metric-value"), Ui.label(label, "metric-label"));
        v.getStyleClass().add("metric-box");
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    @SuppressWarnings("deprecation")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(52);
        table.setPlaceholder(Ui.label("No horses", "text-muted"));
        table.prefHeightProperty().bind(javafx.beans.binding.Bindings.size(visible).multiply(52).add(64));
        table.setMinHeight(160);

        TableColumn<Horse, Horse> horseCol = new TableColumn<>("HORSE");
        horseCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        horseCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Horse h, boolean empty) {
                super.updateItem(h, empty);
                if (empty || h == null) {
                    setGraphic(null);
                    return;
                }
                Label n = Ui.label(h.name(), "text-strong");
                Label id = Ui.label("#" + h.id(), "text-subtle", "text-small");
                n.setMinWidth(0);
                id.setMinWidth(0);
                VBox titles = new VBox(1, n, id);
                titles.setAlignment(Pos.CENTER_LEFT);
                titles.setMinWidth(0);
                HBox cell = Ui.hbox(10, Ui.avatar(h, 32), titles);
                cell.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });
        horseCol.setComparator(Comparator.comparing(h -> h.name().toLowerCase(Locale.ROOT)));
        horseCol.setPrefWidth(240);
        horseCol.setMinWidth(190);

        jockeyCol = textCol("JOCKEY", Horse::jockey, 150);
        ageCol = textCol("AGE", h -> String.valueOf(h.age()), 60);
        ageCol.setMinWidth(64);
        ageCol.setMaxWidth(80);
        breedCol = textCol("BREED", Horse::breed, 130);
        recordCol = textCol("RECORD", h -> {
            RaceRecord r = RaceRecord.parse(h.raceRecord());
            return r.wins() + "W – " + r.losses() + "L" + (r.starts() > 0 ? "  (" + Math.round(r.winRate() * 100) + "%)" : "");
        }, 140);

        TableColumn<Horse, Horse> groupCol = new TableColumn<>("GROUP");
        groupCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        groupCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Horse h, boolean empty) {
                super.updateItem(h, empty);
                setText(null);
                setGraphic(empty || h == null ? null : Ui.groupBadge(h.group()));
            }
        });
        groupCol.setComparator(Comparator.comparing(h -> h.group().ordinal()));
        groupCol.setPrefWidth(100);
        groupCol.setMinWidth(90);
        groupCol.setMaxWidth(130);

        TableColumn<Horse, Horse> actionsCol = new TableColumn<>("");
        actionsCol.setSortable(false);
        actionsCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        actionsCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Horse h, boolean empty) {
                super.updateItem(h, empty);
                setText(null);
                if (empty || h == null) {
                    setGraphic(null);
                    return;
                }
                Button edit = Ui.iconButton(Icons.EDIT, "Edit");
                edit.setOnAction(e -> HorseFormDialog.open(h, HorsesView.this::refresh));
                Button del = Ui.iconButton(Icons.DELETE, "Delete", "danger");
                del.setOnAction(e -> confirmDelete(h));
                HBox box = new HBox(2, edit, del);
                box.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(box);
            }
        });
        actionsCol.setPrefWidth(90);
        actionsCol.setMinWidth(90);
        actionsCol.setMaxWidth(110);

        table.getColumns().setAll(List.of(horseCol, jockeyCol, ageCol, breedCol, recordCol, groupCol, actionsCol));
        table.setRowFactory(tv -> {
            TableRow<Horse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) HorseFormDialog.open(row.getItem(), this::refresh);
            });
            return row;
        });

        // Hide lower-priority columns as the table narrows.
        table.widthProperty().addListener((o, a, w) -> {
            double width = w.doubleValue();
            breedCol.setVisible(width >= 820);
            recordCol.setVisible(width >= 700);
            ageCol.setVisible(width >= 560);
            jockeyCol.setVisible(width >= 480);
        });
    }

    private TableColumn<Horse, String> textCol(String title, Function<Horse, String> fn, double width) {
        TableColumn<Horse, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new SimpleStringProperty(fn.apply(cd.getValue())));
        col.setPrefWidth(width);
        col.setStyle("-fx-alignment: CENTER-LEFT;");
        return col;
    }

    // ------------------------------------------------------------------ actions

    private void confirmDelete(Horse h) {
        Overlay.confirm("Delete " + h.name() + "?",
                "This removes " + h.name() + " (" + h.group() + ", ridden by " + h.jockey() + ") from your stable. This can't be undone.",
                "Delete", true, () -> {
                    service.deleteHorse(h.id());
                    Toasts.success(h.name() + " was removed.");
                    refresh();
                });
    }

    private void exportReport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export horse report");
        fc.setInitialFileName("rapid-run-report.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        File f = fc.showSaveDialog(root().getScene().getWindow());
        if (f == null) return;
        try {
            ServiceRegistry.getFileExportService().exportReport(service.getAllHorses(),
                    ServiceRegistry.getRaceHistoryService().latest().orElse(null), f.toPath());
            Toasts.success("Report saved to " + f.getName());
        } catch (IOException ex) {
            Toasts.error("Couldn't save report: " + ex.getMessage());
        }
    }
}
