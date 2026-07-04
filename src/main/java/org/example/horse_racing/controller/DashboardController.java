package org.example.horse_racing.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.horse_racing.config.ServiceRegistry;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.util.List;

public class DashboardController {

    @FXML private Label statTotal, statGroupA, statGroupB, statGroupC, statGroupD, statRaceReady, statRaceReadyDetail;

    private Runnable refreshCallback;

    @FXML
    public void initialize() {
        refresh();
    }

    public void setRefreshCallback(Runnable r) {
        this.refreshCallback = r;
    }

    public void refresh() {
        List<Horse> all = ServiceRegistry.getHorseService().getAllHorses();
        long a = all.stream().filter(h -> h.group() == HorseGroup.A).count();
        long b = all.stream().filter(h -> h.group() == HorseGroup.B).count();
        long c = all.stream().filter(h -> h.group() == HorseGroup.C).count();
        long d = all.stream().filter(h -> h.group() == HorseGroup.D).count();

        if (statTotal != null) statTotal.setText(String.valueOf(all.size()));
        if (statGroupA != null) statGroupA.setText(String.valueOf(a));
        if (statGroupB != null) statGroupB.setText(String.valueOf(b));
        if (statGroupC != null) statGroupC.setText(String.valueOf(c));
        if (statGroupD != null) statGroupD.setText(String.valueOf(d));

        boolean ready = a > 0 && b > 0 && c > 0 && d > 0;
        if (statRaceReady != null) {
            statRaceReady.setText(ready ? "Ready" : "Not ready");
            statRaceReady.setStyle(ready
                    ? "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12; -fx-background-color: #dcfce7; -fx-text-fill: #166534;"
                    : "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12; -fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }
        if (statRaceReadyDetail != null) {
            if (ready) {
                statRaceReadyDetail.setText("All four groups have horses. Go to Simulate Race to start a race.");
            } else {
                var empty = new java.util.ArrayList<String>();
                if (a == 0) empty.add("A");
                if (b == 0) empty.add("B");
                if (c == 0) empty.add("C");
                if (d == 0) empty.add("D");
                statRaceReadyDetail.setText("Add at least one horse to group(s): " + String.join(", ", empty) + ". Use View Horses to manage your roster.");
            }
        }
    }
}
