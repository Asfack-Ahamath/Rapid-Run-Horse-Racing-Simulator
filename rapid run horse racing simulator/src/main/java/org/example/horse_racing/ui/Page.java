package org.example.horse_racing.ui;

import org.example.horse_racing.ui.components.Icons;

/** Top-level navigation destinations. */
public enum Page {
    DASHBOARD("Dashboard", Icons.HOME),
    HORSES("Horses", Icons.HORSES),
    RACE("Race Day", Icons.FLAG),
    STATS("Statistics", Icons.CHART);

    public final String title;
    public final String icon;

    Page(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }
}
