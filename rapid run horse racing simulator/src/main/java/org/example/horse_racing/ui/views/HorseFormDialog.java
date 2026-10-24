package org.example.horse_racing.ui.views;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceRecord;
import org.example.horse_racing.service.HorseService;
import org.example.horse_racing.ui.components.*;

import java.io.File;
import java.util.List;

/** Add / edit horse form shown as an in-window dialog. */
final class HorseFormDialog {

    private static final List<String> BREEDS = List.of("Thoroughbred", "Arabian", "Quarter Horse", "Standardbred",
            "Appaloosa", "Andalusian", "Morgan", "Mustang", "Friesian", "Paint");

    private final HorseService service = ServiceRegistry.getHorseService();
    private final Horse editing;
    private final Runnable onSaved;

    private final TextField name = new TextField();
    private final TextField jockey = new TextField();
    private final ComboBox<String> breed = new ComboBox<>();
    private final Spinner<Integer> age = new Spinner<>(1, 30, 5);
    private final Spinner<Integer> wins = new Spinner<>(0, 999, 0);
    private final Spinner<Integer> losses = new Spinner<>(0, 999, 0);
    private final ToggleGroup groupToggle = new ToggleGroup();
    private final Label nameErr = err(), jockeyErr = err(), breedErr = err(), groupErr = err();
    private final StackPane avatarHolder = new StackPane();
    private String imageUrl = "";
    private Overlay.Handle handle;

    private HorseFormDialog(Horse editing, Runnable onSaved) {
        this.editing = editing;
        this.onSaved = onSaved;
    }

    static void open(Horse editing, Runnable onSaved) {
        new HorseFormDialog(editing, onSaved).show();
    }

    private void show() {
        boolean isNew = editing == null;
        String id = isNew ? service.nextGeneratedId() : editing.id();

        name.setPromptText("e.g. Thunder Strike");
        jockey.setPromptText("e.g. Maria Garcia");
        breed.getItems().setAll(BREEDS);
        breed.setEditable(true);
        breed.setPromptText("Choose or type");
        breed.setMaxWidth(Double.MAX_VALUE);
        for (Spinner<Integer> s : List.of(age, wins, losses)) {
            s.setEditable(true);
            s.setMaxWidth(Double.MAX_VALUE);
            commitOnFocusLost(s);
        }

        // Group picker
        ToggleButton[] groupButtons = new ToggleButton[4];
        for (HorseGroup g : HorseGroup.values()) {
            ToggleButton t = new ToggleButton(g.toString());
            t.setGraphic(Ui.dot(g));
            t.setUserData(g);
            t.setToggleGroup(groupToggle);
            t.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(t, Priority.ALWAYS);
            groupButtons[g.ordinal()] = t;
        }
        HBox groupPicker = new HBox(2, groupButtons);
        groupPicker.getStyleClass().add("segmented");
        Ui.requireSelection(groupToggle);

        if (!isNew) {
            name.setText(editing.name());
            jockey.setText(editing.jockey());
            breed.setValue(editing.breed());
            age.getValueFactory().setValue(editing.age());
            RaceRecord r = RaceRecord.parse(editing.raceRecord());
            wins.getValueFactory().setValue(r.wins());
            losses.getValueFactory().setValue(r.losses());
            groupButtons[editing.group().ordinal()].setSelected(true);
            imageUrl = editing.imagePath() == null ? "" : editing.imagePath();
        }

        // Photo row
        Button choose = Ui.button("Choose photo", Icons.IMAGE, "btn-secondary", "btn-small");
        Button remove = Ui.button("Remove", null, "btn-ghost", "btn-small");
        choose.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose a photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File f = fc.showOpenDialog(choose.getScene().getWindow());
            if (f != null) {
                imageUrl = f.toURI().toString();
                refreshAvatar();
            }
        });
        remove.setOnAction(e -> {
            imageUrl = "";
            refreshAvatar();
        });
        Label idLabel = Ui.label(isNew ? "New horse · ID " + id : "ID " + id, "text-strong");
        Label photoHint = Ui.label("PNG, JPG or GIF. Optional.", "field-hint");
        VBox photoText = new VBox(6, idLabel, Ui.hbox(8, choose, remove), photoHint);
        HBox photoRow = Ui.hbox(16, avatarHolder, photoText);
        name.textProperty().addListener(o -> refreshAvatar());
        groupToggle.selectedToggleProperty().addListener(o -> refreshAvatar());
        refreshAvatar();

        ResponsiveGrid fields = new ResponsiveGrid(220, 14,
                field("Name", name, nameErr),
                field("Jockey", jockey, jockeyErr),
                field("Breed", breed, breedErr),
                field("Age (years)", age, null),
                field("Wins", wins, null),
                field("Losses", losses, null));
        fields.setMaxColumns(2);

        VBox groupBlock = new VBox(6, Ui.label("Group", "field-label"), groupPicker, groupErr);

        VBox body = new VBox(18, photoRow, Ui.divider(), fields, groupBlock);

        Button cancel = Ui.button("Cancel", null, "btn-secondary");
        Button save = Ui.button(isNew ? "Add horse" : "Save changes", Icons.CHECK, "btn-primary");
        save.setDefaultButton(true);
        cancel.setOnAction(e -> handle.close());
        save.setOnAction(e -> save(id));

        handle = Overlay.dialog(isNew ? "Add horse" : "Edit " + editing.name(), isNew ? Icons.ADD : Icons.EDIT,
                body, List.of(cancel, save), 600);
        Platform.runLater(name::requestFocus);
    }

    private void refreshAvatar() {
        Toggle t = groupToggle.getSelectedToggle();
        HorseGroup g = t == null ? HorseGroup.A : (HorseGroup) t.getUserData();
        String n = name.getText() == null || name.getText().isBlank() ? "?" : name.getText();
        Horse preview = new Horse("0", n, "", 1, "", "", g, 0, imageUrl);
        avatarHolder.getChildren().setAll(Ui.avatar(preview, 64));
    }

    private void save(String id) {
        clearErrors();
        boolean ok = true;
        String n = trim(name.getText()), j = trim(jockey.getText());
        String b = trim(breed.getEditor().getText());
        if (b.isEmpty() && breed.getValue() != null) b = breed.getValue().trim();
        if (n.isEmpty()) ok = fail(name, nameErr, "Name is required.");
        else if (n.length() > 40) ok = fail(name, nameErr, "Keep it under 40 characters.");
        if (j.isEmpty()) ok = fail(jockey, jockeyErr, "Jockey is required.");
        if (b.isEmpty()) ok = fail(breed, breedErr, "Breed is required.");
        Toggle t = groupToggle.getSelectedToggle();
        if (t == null) {
            groupErr.setText("Pick a group.");
            show(groupErr, true);
            ok = false;
        }
        if (!ok) return;

        HorseGroup g = (HorseGroup) t.getUserData();
        String record = new RaceRecord(wins.getValue(), losses.getValue()).format();
        try {
            HorseService.validateAge(age.getValue());
            HorseService.validateId(id);
            if (editing == null) {
                service.addHorse(id, n, j, age.getValue(), b, record, g, imageUrl);
                Toasts.success(n + " joined " + g + ".");
            } else {
                service.updateHorse(id, n, j, age.getValue(), b, record, g, imageUrl);
                Toasts.success("Saved changes to " + n + ".");
            }
            handle.close();
            if (onSaved != null) onSaved.run();
        } catch (IllegalArgumentException ex) {
            Toasts.error(ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ helpers

    private static VBox field(String label, Control input, Label error) {
        VBox v = new VBox(6, Ui.label(label, "field-label"), input);
        input.setMaxWidth(Double.MAX_VALUE);
        if (error != null) v.getChildren().add(error);
        return v;
    }

    private static Label err() {
        Label l = Ui.label("", "field-error");
        show(l, false);
        return l;
    }

    private boolean fail(Control c, Label err, String msg) {
        c.getStyleClass().add("invalid");
        err.setText(msg);
        show(err, true);
        return false;
    }

    private void clearErrors() {
        for (Control c : List.of(name, jockey, breed)) c.getStyleClass().remove("invalid");
        for (Label l : List.of(nameErr, jockeyErr, breedErr, groupErr)) show(l, false);
    }

    private static void show(Node n, boolean v) {
        n.setVisible(v);
        n.setManaged(v);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** Spinners only commit typed text on Enter by default; also commit when focus leaves. */
    private static void commitOnFocusLost(Spinner<Integer> s) {
        s.focusedProperty().addListener((o, was, is) -> {
            if (!is) {
                try {
                    int v = Integer.parseInt(s.getEditor().getText().trim());
                    s.getValueFactory().setValue(v);
                } catch (NumberFormatException e) {
                    s.getEditor().setText(String.valueOf(s.getValue()));
                }
            }
        });
    }
}
