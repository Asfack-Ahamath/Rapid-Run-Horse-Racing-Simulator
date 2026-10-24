package org.example.horse_racing.ui;

import javafx.scene.Node;

/** A top-level page shown in the app shell's content area. */
public interface View {

    Node root();

    /** Called every time the view becomes visible; refresh data here. */
    default void onShow() {}

    /** Called when the view is about to be hidden. */
    default void onHide() {}
}
