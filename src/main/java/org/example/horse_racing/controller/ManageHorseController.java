package org.example.horse_racing.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.service.HorseService;

import java.io.File;

public class ManageHorseController {

    @FXML private Label dialogTitleLabel;
    @FXML private TextField idField, nameField, jockeyField, breedField, raceRecordField;
    @FXML private Spinner<Integer> ageSpinner;
    @FXML private RadioButton groupARadioButton, groupBRadioButton, groupCRadioButton, groupDRadioButton;
    @FXML private ImageView imageView;
    @FXML private Button browseButton, saveButton, cancelButton;

    private ToggleGroup groupToggleGroup;
    private HorseService horseService = ServiceRegistry.getHorseService();
    private Horse editingHorse; // null = add mode
    private Runnable onSaved;

    @FXML
    public void initialize() {
        groupToggleGroup = new ToggleGroup();
        groupARadioButton.setToggleGroup(groupToggleGroup);
        groupBRadioButton.setToggleGroup(groupToggleGroup);
        groupCRadioButton.setToggleGroup(groupToggleGroup);
        groupDRadioButton.setToggleGroup(groupToggleGroup);
        ageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
    }

    public void setEditingHorse(Horse horse) {
        this.editingHorse = horse;
        if (dialogTitleLabel != null) {
            dialogTitleLabel.setText(horse == null ? "Add Horse" : "Edit Horse");
        }
        if (horse != null) {
            idField.setText(horse.id());
            idField.setEditable(false);
            idField.setFocusTraversable(false);
            nameField.setText(horse.name());
            jockeyField.setText(horse.jockey());
            ageSpinner.getValueFactory().setValue(horse.age());
            breedField.setText(horse.breed());
            raceRecordField.setText(horse.raceRecord());
            selectGroup(horse.group());
            if (horse.imagePath() != null && !horse.imagePath().isEmpty()) {
                try { imageView.setImage(new Image(horse.imagePath(), 64, 64, true, true)); } catch (Exception ignored) {}
            }
            saveButton.setText("Update");
        } else {
            clearForm();
            idField.setText(horseService.nextGeneratedId());
            idField.setEditable(false);
            idField.setFocusTraversable(false);
            saveButton.setText("Save");
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    private void selectGroup(HorseGroup group) {
        if (group == HorseGroup.A) groupARadioButton.setSelected(true);
        else if (group == HorseGroup.B) groupBRadioButton.setSelected(true);
        else if (group == HorseGroup.C) groupCRadioButton.setSelected(true);
        else if (group == HorseGroup.D) groupDRadioButton.setSelected(true);
    }

    private HorseGroup getSelectedGroup() {
        Toggle t = groupToggleGroup.getSelectedToggle();
        if (t == null) return null;
        return HorseGroup.fromString(((RadioButton) t).getText());
    }

    private void clearForm() {
        nameField.clear();
        jockeyField.clear();
        ageSpinner.getValueFactory().setValue(5);
        breedField.clear();
        raceRecordField.clear();
        groupToggleGroup.selectToggle(null);
        imageView.setImage(null);
    }

    @FXML
    private void save() {
        try {
            HorseGroup group = getSelectedGroup();
            if (group == null) {
                showAlert(Alert.AlertType.WARNING, "Select a group.");
                return;
            }
            HorseService.validateRequiredFields(nameField.getText(), jockeyField.getText(), breedField.getText(), raceRecordField.getText());
            int age = ageSpinner.getValue();
            HorseService.validateAge(age);

            String imagePath = imageView.getImage() != null ? imageView.getImage().getUrl() : "";

            if (editingHorse == null) {
                String id = horseService.nextGeneratedId();
                HorseService.validateId(id);
                horseService.addHorse(id, nameField.getText().trim(), jockeyField.getText().trim(),
                        age, breedField.getText().trim(), raceRecordField.getText().trim(), group, imagePath);
            } else {
                String id = editingHorse.id();
                HorseService.validateId(id);
                horseService.updateHorse(id, nameField.getText().trim(), jockeyField.getText().trim(),
                        age, breedField.getText().trim(), raceRecordField.getText().trim(), group, imagePath);
            }

            if (onSaved != null) onSaved.run();
            ((Stage) saveButton.getScene().getWindow()).close();
        } catch (HorseService.ValidationException ex) {
            showAlert(Alert.AlertType.WARNING, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    private void browseImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(browseButton.getScene().getWindow());
        if (f != null) imageView.setImage(new Image(f.toURI().toString()));
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
