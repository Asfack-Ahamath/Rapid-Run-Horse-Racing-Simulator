module org.example.horse_racing {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    opens org.example.horse_racing to javafx.fxml;
    opens org.example.horse_racing.controller to javafx.fxml;
    exports org.example.horse_racing;
}