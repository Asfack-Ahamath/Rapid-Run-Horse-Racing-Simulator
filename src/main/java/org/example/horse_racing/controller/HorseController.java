package org.example.horse_racing.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.service.HorseService;
import org.example.horse_racing.service.RaceService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class HorseController {

    @FXML private TableView<Horse> horseTableView;
    @FXML private TextField idField, nameField, jockeyField, ageField, breedField, raceRecordField;
    @FXML private TextArea outputTextArea;
    @FXML private RadioButton groupARadioButton, groupBRadioButton, groupCRadioButton, groupDRadioButton;
    @FXML private Button addButton, updateButton, deleteButton, viewButton, saveButton, selectButton, simulateButton, visualizeButton;
    @FXML private VBox addUpdateVBox;
    @FXML private HBox buttonsHBox;
    @FXML private ImageView imageView;
    @FXML private Button browseButton;
    @FXML private TabPane tabPane;
    @FXML private ToggleGroup groupToggleGroup;
    @FXML private Button exitButton;
    @FXML private Label statTotal, statGroupA, statGroupB, statGroupC, statGroupD;

    private final HorseService horseService = ServiceRegistry.getHorseService();
    private final RaceService raceService = ServiceRegistry.getRaceService();
    private final javafx.collections.ObservableList<Horse> horses = javafx.collections.FXCollections.observableArrayList();
    private Map<HorseGroup, Horse> selectedHorses = new HashMap<>();
    private List<Horse> winningHorses = new ArrayList<>();
    private boolean dataSaved = false;

    @FXML
    private void handleExit() {
        ((Stage) exitButton.getScene().getWindow()).close();
    }

    @FXML
    public void initialize() {
        setupToggleGroup();
        setupTableView();
        loadHorses();
        bindActions();
        idField.setText(horseService.nextGeneratedId());
    }

    private void setupToggleGroup() {
        groupToggleGroup = new ToggleGroup();
        groupARadioButton.setToggleGroup(groupToggleGroup);
        groupBRadioButton.setToggleGroup(groupToggleGroup);
        groupCRadioButton.setToggleGroup(groupToggleGroup);
        groupDRadioButton.setToggleGroup(groupToggleGroup);
    }

    private void setupTableView() {
        horseTableView.setItems(horses);
        horseTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addColumn("ID", h -> h.id());
        addColumn("Name", h -> h.name());
        addColumn("Jockey", h -> h.jockey());
        addColumn("Age", h -> String.valueOf(h.age()));
        addColumn("Breed", h -> h.breed());
        addColumn("Race Record", h -> h.raceRecord());
        addColumn("Group", h -> h.group().name());
        addColumn("Seconds", h -> String.valueOf(h.seconds()));
        addImageColumn();
    }

    private void addColumn(String title, java.util.function.Function<Horse, String> extractor) {
        TableColumn<Horse, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(extractor.apply(cd.getValue())));
        horseTableView.getColumns().add(col);
    }

    private void addImageColumn() {
        TableColumn<Horse, ImageView> col = new TableColumn<>("Image");
        col.setCellValueFactory(cd -> {
            ImageView iv = new ImageView();
            iv.setFitWidth(50);
            iv.setFitHeight(50);
            String path = cd.getValue().imagePath();
            if (path != null && !path.isEmpty()) {
                try {
                    iv.setImage(new Image(path, 50, 50, true, true));
                } catch (Exception ignored) {}
            }
            return new javafx.beans.property.SimpleObjectProperty<>(iv);
        });
        horseTableView.getColumns().add(col);
    }

    private void loadHorses() {
        horses.clear();
        horses.addAll(horseService.getAllHorses());
        updateStats();
    }

    private void updateStats() {
        if (statTotal == null || statGroupA == null) return;
        List<Horse> all = horseService.getAllHorses();
        statTotal.setText(String.valueOf(all.size()));
        statGroupA.setText(String.valueOf(all.stream().filter(h -> h.group() == HorseGroup.A).count()));
        statGroupB.setText(String.valueOf(all.stream().filter(h -> h.group() == HorseGroup.B).count()));
        statGroupC.setText(String.valueOf(all.stream().filter(h -> h.group() == HorseGroup.C).count()));
        statGroupD.setText(String.valueOf(all.stream().filter(h -> h.group() == HorseGroup.D).count()));
    }

    private void bindActions() {
        addButton.setOnAction(e -> addHorse());
        updateButton.setOnAction(e -> updateHorse());
        deleteButton.setOnAction(e -> deleteHorse());
        viewButton.setOnAction(e -> viewHorses());
        saveButton.setOnAction(e -> saveToFile());
        selectButton.setOnAction(e -> selectHorses());
        simulateButton.setOnAction(e -> simulateRace());
        visualizeButton.setOnAction(e -> visualizeWinners());
        exitButton.setOnAction(e -> handleExit());
    }

    private HorseGroup getSelectedGroup() {
        Toggle t = groupToggleGroup.getSelectedToggle();
        if (t == null) return null;
        String text = ((RadioButton) t).getText();
        return HorseGroup.fromString(text);
    }

    private void clearForm() {
        idField.setText(horseService.nextGeneratedId());
        nameField.clear();
        jockeyField.clear();
        ageField.clear();
        breedField.clear();
        raceRecordField.clear();
        groupToggleGroup.selectToggle(null);
        imageView.setImage(null);
    }

    private void showMessage(String msg) {
        outputTextArea.setText(msg);
    }

    private void appendMessage(String msg) {
        outputTextArea.appendText("\n" + msg);
    }

    private void addHorse() {
        try {
            HorseGroup group = getSelectedGroup();
            if (group == null) {
                showMessage("Please select a group.");
                return;
            }
            HorseService.validateRequiredFields(nameField.getText(), jockeyField.getText(), breedField.getText(), raceRecordField.getText());
            int age = Integer.parseInt(ageField.getText().trim());
            HorseService.validateAge(age);

            String id = horseService.nextGeneratedId();
            HorseService.validateId(id);
            String imagePath = imageView.getImage() != null ? imageView.getImage().getUrl() : "";
            horseService.addHorse(id, nameField.getText().trim(), jockeyField.getText().trim(),
                    age, breedField.getText().trim(), raceRecordField.getText().trim(), group, imagePath);
            loadHorses();
            showMessage("Horse added successfully.");
            clearForm();
            saveToFile();
        } catch (HorseService.ValidationException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Invalid age. Please enter a number between 1 and 30.");
        }
    }

    private void updateHorse() {
        try {
            HorseGroup group = getSelectedGroup();
            if (group == null) {
                showMessage("Please select a group.");
                return;
            }
            HorseService.validateRequiredFields(nameField.getText(), jockeyField.getText(), breedField.getText(), raceRecordField.getText());
            HorseService.validateId(idField.getText());
            int age = Integer.parseInt(ageField.getText().trim());
            HorseService.validateAge(age);

            String imagePath = imageView.getImage() != null ? imageView.getImage().getUrl() : "";
            horseService.updateHorse(idField.getText().trim(), nameField.getText().trim(), jockeyField.getText().trim(),
                    age, breedField.getText().trim(), raceRecordField.getText().trim(), group, imagePath);
            loadHorses();
            showMessage("Horse updated successfully.");
            clearForm();
            horseTableView.refresh();
            saveToFile();
        } catch (HorseService.ValidationException ex) {
            showMessage(ex.getMessage());
        } catch (NumberFormatException ex) {
            showMessage("Invalid age. Please enter a number between 1 and 30.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void deleteHorse() {
        try {
            if (idField.getText().isBlank()) {
                showMessage("Please enter the ID of the horse to delete.");
                return;
            }
            horseService.deleteHorse(idField.getText().trim());
            loadHorses();
            showMessage("Horse deleted successfully.");
            clearForm();
            saveToFile();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void viewHorses() {
        Tab tab = new Tab("Horse Details");
        TableView<Horse> tv = new TableView<>();
        tv.setItems(javafx.collections.FXCollections.observableArrayList(
                horseService.getAllHorses().stream().sorted(Comparator.comparingInt(h -> Integer.parseInt(h.id()))).toList()));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addColumnToTable(tv, "ID", h -> h.id());
        addColumnToTable(tv, "Name", h -> h.name());
        addColumnToTable(tv, "Jockey", h -> h.jockey());
        addColumnToTable(tv, "Age", h -> String.valueOf(h.age()));
        addColumnToTable(tv, "Breed", h -> h.breed());
        addColumnToTable(tv, "Race Record", h -> h.raceRecord());
        addColumnToTable(tv, "Group", h -> h.group().name());
        addColumnToTable(tv, "Image", h -> h.imagePath() != null && !h.imagePath().isEmpty() ? "Yes" : "");

        tab.setContent(tv);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void addColumnToTable(TableView<Horse> tv, String title, java.util.function.Function<Horse, String> extractor) {
        TableColumn<Horse, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(extractor.apply(cd.getValue())));
        tv.getColumns().add(col);
    }

    private void saveToFile() {
        try {
            Path path = Path.of("horse_details.txt");
            ServiceRegistry.getFileExportService().exportToTextFile(horseService.getAllHorses(), path);
            appendMessage("Horse details saved to " + path.toAbsolutePath());
            dataSaved = true;
        } catch (IOException ex) {
            showMessage("Error saving file: " + ex.getMessage());
        }
    }

    private void selectHorses() {
        if (!dataSaved) {
            showMessage("Please run Save to File first.");
            return;
        }
        try {
            selectedHorses = raceService.selectHorsesForRace();
            displaySelectedHorses();
        } catch (IllegalStateException ex) {
            Set<HorseGroup> empty = raceService.getEmptyGroups();
            StringBuilder sb = new StringBuilder(ex.getMessage());
            if (!empty.isEmpty()) {
                sb.append("\nGroups without horses: ");
                empty.forEach(g -> sb.append(g.name()).append(", "));
                sb.setLength(sb.length() - 2);
            }
            showMessage(sb.toString());
        }
    }

    private void displaySelectedHorses() {
        outputTextArea.clear();
        outputTextArea.appendText("Selected Horses for Race\n\n");
        outputTextArea.appendText(String.format("%-7s %-10s %-20s %-20s %-5s %-15s %-20s\n", "Group", "ID", "Horse Name", "Jockey", "Age", "Breed", "Race Record"));
        outputTextArea.appendText("-".repeat(103) + "\n");

        selectedHorses.values().stream()
                .sorted(Comparator.comparing(h -> h.group().name()))
                .forEach(h -> outputTextArea.appendText(String.format("  %-5s %-10s %-20s %-20s %-5s %-15s %-20s\n",
                        h.group().name(), h.id(), h.name(), h.jockey(), h.age(), h.breed(), h.raceRecord())));
    }

    private void simulateRace() {
        if (selectedHorses.isEmpty()) {
            showMessage("Please run Select Horses first.");
            return;
        }
        winningHorses = raceService.simulateRace(selectedHorses);
        outputTextArea.clear();
        outputTextArea.appendText("Race Results — Top 3\n\n");
        outputTextArea.appendText(String.format("%-6s %-7s %-7s %-20s %-5s\n", "Rank", "Group", "ID", "Horse Name", "Time"));
        outputTextArea.appendText("-".repeat(48) + "\n");

        String[] suffixes = {"st", "nd", "rd"};
        for (int i = 0; i < Math.min(3, winningHorses.size()); i++) {
            Horse h = winningHorses.get(i);
            String rank = (i + 1) + suffixes[i];
            outputTextArea.appendText(String.format("%-8s %-5s %-7s %-20s %-3ss\n", rank, h.group().name(), h.id(), h.name(), h.seconds()));
        }
    }

    private void visualizeWinners() {
        if (winningHorses.isEmpty()) {
            showMessage("Please run Simulate Race first.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/horse_racing/BarChart.fxml"));
            javafx.scene.layout.VBox root = loader.load();
            BarChartController ctrl = loader.getController();
            ctrl.setData(raceService.getTopWinners(winningHorses, 3), java.util.List.of());

            Tab tab = new Tab("Bar Chart");
            tab.setContent(root);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (IOException ex) {
            showMessage("Error loading chart: " + ex.getMessage());
        }
    }

    @FXML
    private void browseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Horse Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(browseButton.getScene().getWindow());
        if (f != null) {
            imageView.setImage(new Image(f.toURI().toString()));
        }
    }
}
