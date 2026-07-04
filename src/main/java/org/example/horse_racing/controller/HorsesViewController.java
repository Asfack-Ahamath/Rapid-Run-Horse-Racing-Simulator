package org.example.horse_racing.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.service.HorseService;

import java.io.IOException;
import java.util.Comparator;

public class HorsesViewController {

    @FXML private TableView<Horse> horseTableView;
    @FXML private GridPane horseCardsPane;
    @FXML private ToggleButton viewTableBtn, viewBoxBtn;
    @FXML private Button addButton;
    @FXML private ScrollPane tablePane, cardsPane;

    private final HorseService horseService = ServiceRegistry.getHorseService();
    private final javafx.collections.ObservableList<Horse> horses = javafx.collections.FXCollections.observableArrayList();
    private ToggleGroup viewToggleGroup;
    private Runnable refreshCallback;

    @FXML
    public void initialize() {
        viewToggleGroup = new ToggleGroup();
        viewTableBtn.setToggleGroup(viewToggleGroup);
        viewBoxBtn.setToggleGroup(viewToggleGroup);
        viewTableBtn.setSelected(true);
        viewToggleGroup.selectedToggleProperty().addListener((o, old, n) -> {
            boolean table = n == viewTableBtn;
            tablePane.setVisible(table);
            tablePane.setManaged(table);
            cardsPane.setVisible(!table);
            cardsPane.setManaged(!table);
            if (!table) buildCards();
        });
        // Responsive: 3 columns at min size, 4 columns at full screen; cards fill full width
        horseCardsPane.getColumnConstraints().clear();
        updateGridColumns();
        cardsPane.widthProperty().addListener((o, old, w) -> updateGridColumns());
        setupTableView();
        refresh();
    }

    public void setRefreshCallback(Runnable r) {
        this.refreshCallback = r;
    }

    private void updateGridColumns() {
        int cols = cardsPane.getWidth() >= 960 ? 4 : 3;
        horseCardsPane.getColumnConstraints().clear();
        double pct = 100.0 / cols;
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(pct);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            horseCardsPane.getColumnConstraints().add(cc);
        }
    }

    private void setupTableView() {
        horseTableView.setItems(horses);
        horseTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        horseTableView.setPlaceholder(new Label("No horses. Add one to get started."));
        horseTableView.getColumns().clear();
        addTableColumn("ID", h -> h.id(), javafx.geometry.Pos.CENTER);
        addTableColumn("Name", h -> h.name(), javafx.geometry.Pos.CENTER_LEFT);
        addTableColumn("Jockey", h -> h.jockey(), javafx.geometry.Pos.CENTER_LEFT);
        addTableColumn("Age", h -> String.valueOf(h.age()), javafx.geometry.Pos.CENTER);
        addTableColumn("Breed", h -> h.breed(), javafx.geometry.Pos.CENTER_LEFT);
        addTableColumn("Race Record", h -> h.raceRecord() != null ? h.raceRecord() : "", javafx.geometry.Pos.CENTER_LEFT);
        addTableColumn("Group", h -> h.group().name(), javafx.geometry.Pos.CENTER);
        addActionsColumn();
    }

    private void addActionsColumn() {
        TableColumn<Horse, Horse> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.getStyleClass().add("btn-primary");
                editBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11px;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-padding: 6 12; -fx-font-size: 11px;");
                box.setAlignment(javafx.geometry.Pos.CENTER);
                box.getChildren().addAll(editBtn, deleteBtn);
            }

            @Override
            protected void updateItem(Horse horse, boolean empty) {
                super.updateItem(horse, empty);
                if (empty || horse == null) {
                    setGraphic(null);
                } else {
                    editBtn.setOnAction(e -> openEditDialog(horse));
                    deleteBtn.setOnAction(e -> deleteHorse(horse));
                    setGraphic(box);
                }
            }
        });
        horseTableView.getColumns().add(actionsCol);
    }

    private int parseIdForSort(Horse h) {
        try {
            return Integer.parseInt(h.id().replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addTableColumn(String title, java.util.function.Function<Horse, String> extractor, javafx.geometry.Pos alignment) {
        TableColumn<Horse, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(extractor.apply(cd.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(alignment);
            }
        });
        horseTableView.getColumns().add(col);
    }

    public void refresh() {
        horses.clear();
        horses.addAll(horseService.getAllHorses().stream()
                .sorted(Comparator.comparing(this::parseIdForSort))
                .toList());
        horseTableView.refresh();
        if (!viewTableBtn.isSelected()) buildCards();
        if (refreshCallback != null) refreshCallback.run();
    }

    private void buildCards() {
        horseCardsPane.getChildren().clear();
        updateGridColumns();
        int cols = horseCardsPane.getColumnConstraints().size();
        if (cols < 1) cols = 3;
        for (int i = 0; i < horses.size(); i++) {
            Horse h = horses.get(i);
            int col = i % cols;
            int row = i / cols;
            VBox card = createHorseCard(h);
            GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            horseCardsPane.add(card, col, row);
        }
    }

    private static final String[] GROUP_COLORS = {"#e8c97a", "#7eb8e8", "#e8a0a0", "#6ed4a0"};

    private VBox createHorseCard(Horse h) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-min-width: 180; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setUserData(h);

        Region accentBar = new Region();
        int groupIdx = h.group().ordinal();
        accentBar.setStyle("-fx-background-color: " + GROUP_COLORS[groupIdx] + "; -fx-background-radius: 12 12 0 0; -fx-pref-height: 4; -fx-min-height: 4;");

        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 16 16 8 16;");
        ImageView iv = new ImageView();
        iv.setFitWidth(56);
        iv.setFitHeight(56);
        iv.setStyle("-fx-background-color: #f0ebe0; -fx-background-radius: 8;");
        if (h.imagePath() != null && !h.imagePath().isEmpty()) {
            try { iv.setImage(new Image(h.imagePath(), 56, 56, true, true)); } catch (Exception ignored) {}
        }
        StackPane imgWrap = new StackPane(iv);
        imgWrap.setStyle("-fx-background-color: #f0ebe0; -fx-background-radius: 8;");
        imgWrap.setMinSize(56, 56);
        imgWrap.setPrefSize(56, 56);

        VBox titleBox = new VBox(2);
        Label nameLbl = new Label(h.name());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0d4a2e;");
        Label idLbl = new Label("ID: " + h.id());
        idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
        VBox.setVgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);
        titleBox.getChildren().addAll(nameLbl, idLbl);

        Label groupBadge = new Label(h.group().name());
        groupBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-background-color: " + GROUP_COLORS[groupIdx] + "; -fx-background-radius: 8; -fx-padding: 4 10;");
        header.getChildren().addAll(imgWrap, titleBox, groupBadge);
        HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);

        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-padding: 8 16 16 16;");
        infoBox.getChildren().addAll(
                infoRow("Jockey", h.jockey()),
                infoRow("Breed", h.breed()),
                infoRow("Record", h.raceRecord() != null ? h.raceRecord() : "—")
        );

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        btnBox.setStyle("-fx-padding: 12 16 16 16;");
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setStyle("-fx-padding: 6 16; -fx-font-size: 12px;");
        editBtn.setOnAction(e -> openEditDialog(h));
        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setStyle("-fx-padding: 6 16; -fx-font-size: 12px;");
        delBtn.setOnAction(e -> deleteHorse(h));
        btnBox.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(accentBar, header, infoBox, btnBox);
        return card;
    }

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096; -fx-min-width: 50;");
        Label val = new Label(value.length() > 24 ? value.substring(0, 21) + "…" : value);
        val.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c2416;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    @FXML
    private void openAddDialog() {
        openManageDialog(null);
    }

    private void openEditDialog(Horse h) {
        openManageDialog(h);
    }

    private void openManageDialog(Horse horse) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/ManageHorseDialog.fxml"));
            VBox root = loader.load();
            ManageHorseController ctrl = loader.getController();
            ctrl.setEditingHorse(horse);
            ctrl.setOnSaved(this::refresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            javafx.stage.Window owner = addButton != null && addButton.getScene() != null
                    ? addButton.getScene().getWindow() : null;
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setTitle(horse == null ? "Add Horse" : "Edit Horse");
            stage.setResizable(false);
            stage.setMaximized(false);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 420, 580);
            java.net.URL css = getClass().getResource("/org/example/horse_racing/styles/theme.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    private void deleteHorse(Horse h) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + h.name() + "?");
        confirm.setHeaderText("Confirm Delete");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            horseService.deleteHorse(h.id());
            refresh();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(title);
        a.showAndWait();
    }
}
