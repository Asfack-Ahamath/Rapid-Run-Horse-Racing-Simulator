package org.example.horse_racing.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.example.horse_racing.ui.components.*;
import org.example.horse_racing.ui.theme.ThemeManager;
import org.example.horse_racing.ui.theme.ThemeManager.Mode;
import org.example.horse_racing.ui.views.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * The main window layout: a sidebar (expandable / collapsible to an icon rail), a content area that
 * hosts the current {@link View}, and overlay layers for dialogs and toasts.
 * <p>
 * The sidebar collapses automatically when the window becomes narrow and returns to the user's
 * chosen state when there is room again. The expand/collapse button (or Ctrl+B) toggles it at any time.
 */
public class AppShell {

    private static final double AUTO_COLLAPSE_BELOW = 980;
    private static final double SHORT_BELOW = 620;
    private static final double SIDEBAR_WIDE = 232;
    private static final double SIDEBAR_RAIL = 72;
    private static final String PREF_COLLAPSED = "sidebarCollapsed";

    private final Preferences prefs = Preferences.userNodeForPackage(AppShell.class);
    private final StackPane root = new StackPane();
    private final BorderPane frame = new BorderPane();
    private final StackPane content = new StackPane();
    private final VBox sidebar = new VBox();
    private final Map<Page, View> views = new EnumMap<>(Page.class);
    private final Map<Page, ToggleButton> navButtons = new EnumMap<>(Page.class);
    private final ToggleGroup navGroup = new ToggleGroup();
    private final VBox brandText = new VBox(1);
    private final HBox brand = new HBox(10);
    private final HBox themeSwitch = new HBox();
    private final Button themeToggle = new Button();
    private final Button collapseButton = new Button();
    private final Label navCaption = Ui.label("MENU", "nav-caption");
    private final Label versionLabel = Ui.label("v2.1 · JavaFX", "side-meta");
    private final ToggleGroup themeGroup = new ToggleGroup();
    private ToggleButton aboutButton;
    private Page current;

    private boolean collapsed;
    private boolean userCollapsed;   // the user's own preference (persisted)
    private boolean narrow;          // window currently below the auto-collapse width
    private Timeline widthAnim;

    public AppShell() {
        StackPane overlayLayer = new StackPane();
        VBox toastLayer = new VBox();
        Overlay.install(overlayLayer);
        Toasts.install(toastLayer);
        StackPane.setAlignment(toastLayer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toastLayer, new Insets(16));

        content.getStyleClass().add("app-content");
        content.setMinWidth(0);
        frame.setLeft(sidebar);
        frame.setCenter(content);
        root.getChildren().addAll(frame, overlayLayer, toastLayer);

        Navigator nav = this::navigate;
        views.put(Page.DASHBOARD, new DashboardView(nav));
        views.put(Page.HORSES, new HorsesView());
        views.put(Page.RACE, new RaceView(nav));
        views.put(Page.STATS, new StatsView(nav));

        buildSidebar();

        userCollapsed = prefs.getBoolean(PREF_COLLAPSED, false);
        applyCollapsed(userCollapsed, false);
        root.widthProperty().addListener((o, a, w) -> onWidthChanged(w.doubleValue()));
        root.heightProperty().addListener((o, a, h) -> applyShort(h.doubleValue() < SHORT_BELOW));
        navigate(Page.DASHBOARD);
    }

    public StackPane root() {
        return root;
    }

    public void installShortcuts(Scene scene) {
        var acc = scene.getAccelerators();
        Page[] pages = Page.values();
        KeyCode[] digits = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4};
        for (int i = 0; i < pages.length; i++) {
            Page p = pages[i];
            acc.put(new KeyCodeCombination(digits[i], KeyCombination.SHORTCUT_DOWN), () -> navigate(p));
        }
        acc.put(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> ThemeManager.get().toggle());
        acc.put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::toggleSidebar);
        acc.put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> {
            navigate(Page.HORSES);
            ((HorsesView) views.get(Page.HORSES)).openAddDialog();
        });
        acc.put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
            navigate(Page.HORSES);
            ((HorsesView) views.get(Page.HORSES)).focusSearch();
        });
        acc.put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), () -> {
            navigate(Page.RACE);
            ((RaceView) views.get(Page.RACE)).startOrStop();
        });
    }

    // ------------------------------------------------------------------ navigation

    public void navigate(Page page) {
        if (Overlay.isOpen() && page != current) return;
        if (page == current) {
            navButtons.get(page).setSelected(true);
            return;
        }
        if (current != null) views.get(current).onHide();
        current = page;
        navButtons.get(page).setSelected(true);
        View v = views.get(page);
        Node n = v.root();
        content.getChildren().setAll(n);
        v.onShow();
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), n);
        ft.setToValue(1);
        ft.play();
    }

    // ------------------------------------------------------------------ sidebar

    private void buildSidebar() {
        sidebar.getStyleClass().add("sidebar");

        // Brand
        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        ImageView horse = new ImageView(Images.tinted(Images.HORSE, Color.web("#0F3D2A")));
        horse.setFitWidth(24);
        horse.setFitHeight(24);
        horse.setPreserveRatio(true);
        horse.setSmooth(true);
        mark.getChildren().add(horse);
        Label title = Ui.label("Rapid Run", "brand-title");
        Label sub = Ui.label("Horse Racing Simulator", "brand-sub");
        title.setMinWidth(0);
        sub.setMinWidth(0);
        brandText.getChildren().addAll(title, sub);
        brandText.setMinWidth(0);
        brand.getChildren().addAll(mark, brandText);
        brand.getStyleClass().add("brand");

        // Navigation (scrolls on very short windows so the footer never gets pushed off-screen)
        VBox navList = new VBox(4, navCaption);
        for (Page p : Page.values()) {
            ToggleButton b = navButton(p.title, p.icon);
            b.setOnAction(e -> navigate(p));
            navButtons.put(p, b);
            navList.getChildren().add(b);
        }
        Ui.requireSelection(navGroup);
        ScrollPane navScroll = new ScrollPane(navList);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.getStyleClass().add("nav-scroll");
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        // About (opens a dialog)
        aboutButton = navButton("About", Icons.INFO);
        aboutButton.setToggleGroup(null);
        aboutButton.setOnAction(e -> {
            aboutButton.setSelected(false);
            AboutDialog.show();
        });

        // Expand / collapse
        collapseButton.getStyleClass().add("nav-item");
        collapseButton.setMaxWidth(Double.MAX_VALUE);
        collapseButton.setOnAction(e -> toggleSidebar());

        // Theme: two-button Light | Dark switch when expanded, a single toggle when collapsed
        ToggleButton light = themeOption("Light", Icons.SUN, Mode.LIGHT);
        ToggleButton dark = themeOption("Dark", Icons.MOON, Mode.DARK);
        Ui.requireSelection(themeGroup);
        themeSwitch.getChildren().addAll(light, dark);
        themeSwitch.getStyleClass().add("theme-switch");
        HBox.setHgrow(light, Priority.ALWAYS);
        HBox.setHgrow(dark, Priority.ALWAYS);

        themeToggle.getStyleClass().add("nav-item");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        themeToggle.setOnAction(e -> ThemeManager.get().toggle());
        ThemeManager.get().modeProperty().addListener((o, a, m) -> syncThemeControls());
        syncThemeControls();

        VBox footer = new VBox(6, aboutButton, collapseButton, themeSwitch, themeToggle, versionLabel);
        footer.setPadding(new Insets(10, 0, 0, 0));
        versionLabel.setPadding(new Insets(4, 12, 0, 12));

        sidebar.getChildren().addAll(brand, navScroll, footer);
    }

    private ToggleButton navButton(String text, String icon) {
        ToggleButton b = new ToggleButton(text);
        b.setGraphic(Icons.of(icon, 20));
        b.getStyleClass().setAll("nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setToggleGroup(navGroup);
        b.setMinWidth(0);
        Ui.tip(b, text);
        return b;
    }

    private ToggleButton themeOption(String text, String icon, Mode mode) {
        ToggleButton t = new ToggleButton(text);
        t.setGraphic(Icons.of(icon, 16));
        t.setUserData(mode);
        t.setToggleGroup(themeGroup);
        t.setMaxWidth(Double.MAX_VALUE);
        t.setMinWidth(0);
        t.setOnAction(e -> ThemeManager.get().setMode(mode));
        Ui.tip(t, text + " mode");
        return t;
    }

    private void syncThemeControls() {
        Mode m = ThemeManager.get().getMode();
        themeGroup.getToggles().stream().filter(t -> t.getUserData() == m).findFirst().ifPresent(t -> t.setSelected(true));
        boolean dark = m == Mode.DARK;
        themeToggle.setGraphic(Icons.of(dark ? Icons.SUN : Icons.MOON, 20));
        Ui.tip(themeToggle, dark ? "Switch to light mode" : "Switch to dark mode");
    }

    // ------------------------------------------------------------------ collapse logic

    /** Expand/collapse button and Ctrl+B. */
    public void toggleSidebar() {
        boolean next = !collapsed;
        if (!narrow) {
            userCollapsed = next;
            prefs.putBoolean(PREF_COLLAPSED, next);
        }
        applyCollapsed(next, true);
    }

    private void onWidthChanged(double w) {
        boolean nowNarrow = w < AUTO_COLLAPSE_BELOW;
        if (nowNarrow == narrow) return;
        narrow = nowNarrow;
        // Entering a narrow window collapses; leaving it restores the user's own choice.
        applyCollapsed(narrow || userCollapsed, true);
    }

    private void applyCollapsed(boolean c, boolean animate) {
        collapsed = c;
        sidebar.getStyleClass().remove("collapsed");
        if (c) sidebar.getStyleClass().add("collapsed");

        double target = c ? SIDEBAR_RAIL : SIDEBAR_WIDE;
        if (widthAnim != null) widthAnim.stop();
        if (animate && sidebar.getWidth() > 0) {
            widthAnim = new Timeline(new KeyFrame(Duration.millis(170),
                    new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.minWidthProperty(), target, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.maxWidthProperty(), target, Interpolator.EASE_BOTH)));
            widthAnim.play();
        } else {
            sidebar.setMinWidth(target);
            sidebar.setPrefWidth(target);
            sidebar.setMaxWidth(target);
        }

        ContentDisplay cd = c ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT;
        navButtons.values().forEach(b -> b.setContentDisplay(cd));
        aboutButton.setContentDisplay(cd);
        collapseButton.setContentDisplay(cd);
        collapseButton.setText(c ? "Expand" : "Collapse");
        collapseButton.setGraphic(Icons.of(c ? Icons.CHEVRON_RIGHT : Icons.CHEVRON_LEFT, 20));
        Ui.tip(collapseButton, (c ? "Expand sidebar" : "Collapse sidebar") + " (Ctrl+B)");

        show(brandText, !c);
        show(themeSwitch, !c);
        show(themeToggle, c);
        applyShort(root.getHeight() > 0 && root.getHeight() < SHORT_BELOW);
    }

    /** On short windows hide decorative labels so all controls stay reachable. */
    private void applyShort(boolean isShort) {
        show(navCaption, !collapsed && !isShort);
        show(versionLabel, !collapsed && !isShort);
        sidebar.getStyleClass().remove("short");
        if (isShort) sidebar.getStyleClass().add("short");
    }

    private static void show(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    /** Lets views move the user between pages. */
    @FunctionalInterface
    public interface Navigator {
        void go(Page page);
    }
}
