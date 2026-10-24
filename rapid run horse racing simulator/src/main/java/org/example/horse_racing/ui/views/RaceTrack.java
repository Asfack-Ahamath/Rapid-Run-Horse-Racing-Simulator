package org.example.horse_racing.ui.views;

import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.service.FileExportService;
import org.example.horse_racing.ui.components.Images;
import org.example.horse_racing.ui.theme.Palette;
import org.example.horse_racing.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas-drawn race track. Scales to any width; height follows a fixed aspect ratio within limits.
 * The view mutates {@link Lane} state each frame and calls {@link #redraw()}.
 */
final class RaceTrack extends Region {

    /** Per-lane state the animation updates. */
    static final class Lane {
        final HorseGroup group;
        final String name;
        final String subtitle;
        double progress;      // 0 at the gate, 1 at the finish line (may overshoot slightly)
        int rank;             // 0 until finished
        String timeLabel = "";
        double phase;         // gallop animation phase

        Lane(HorseGroup group, String name, String subtitle) {
            this.group = group;
            this.name = name;
            this.subtitle = subtitle == null ? "" : subtitle;
        }
    }

    private static final double RAIL = 12;
    private static final double MIN_H = 200, MAX_H = 460, RATIO = 0.42;
    private double maxHeight = MAX_H;

    private final Canvas canvas = new Canvas();
    private final List<Lane> lanes = new ArrayList<>();
    private final int distance;
    private boolean running;

    RaceTrack(int distanceMeters) {
        this.distance = distanceMeters;
        getChildren().add(canvas);
        setMinHeight(MIN_H);
        ThemeManager.get().darkProperty().addListener(o -> redraw());
    }

    void setLanes(List<Lane> newLanes) {
        lanes.clear();
        lanes.addAll(newLanes);
        redraw();
    }

    List<Lane> lanes() {
        return lanes;
    }

    /** Caps the track height so the whole race fits in short windows. */
    void setMaxTrackHeight(double h) {
        double v = Math.max(MIN_H, Math.min(MAX_H, h));
        if (v != maxHeight) {
            maxHeight = v;
            requestLayout();
            if (getParent() != null) getParent().requestLayout();
        }
    }

    void setRunning(boolean r) {
        running = r;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width < 0 ? 900 : width;
        return Math.max(MIN_H, Math.min(maxHeight, w * RATIO));
    }

    @Override
    protected double computePrefWidth(double height) {
        return 900;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 300;
    }

    @Override
    protected void layoutChildren() {
        double w = snapSizeX(getWidth()), h = snapSizeY(getHeight());
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            redraw();
        }
    }

    double startX() {
        return getWidth() < 560 ? 54 : 64;
    }

    double finishX() {
        return canvas.getWidth() - (getWidth() < 560 ? 52 : 74);
    }

    void redraw() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;
        Palette pal = ThemeManager.get().palette();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        int n = Math.max(4, lanes.size());
        double top = RAIL, bottom = h - RAIL;
        double laneH = (bottom - top) / n;
        double sx = startX(), fx = finishX();

        // Rails
        g.setFill(pal.rail());
        g.fillRect(0, 0, w, RAIL);
        g.fillRect(0, bottom, w, RAIL);
        g.setFill(pal.rail().darker());
        g.fillRect(0, RAIL - 2, w, 2);
        g.fillRect(0, bottom, w, 2);
        // Rail posts
        g.setFill(pal.rail().deriveColor(0, 1, 0.82, 1));
        for (double x = 20; x < w; x += 48) {
            g.fillRect(x, 0, 3, RAIL - 2);
            g.fillRect(x, bottom + 2, 3, RAIL - 2);
        }

        // Turf with mowing stripes
        g.setFill(pal.turf());
        g.fillRect(0, top, w, bottom - top);
        g.setFill(pal.turfStripe());
        double stripe = Math.max(36, w / 22);
        for (double x = 0, i = 0; x < w; x += stripe, i++) {
            if (((int) i & 1) == 0) g.fillRect(x, top, stripe, bottom - top);
        }

        // Lane dividers
        g.setStroke(pal.laneLine());
        g.setLineWidth(1.2);
        g.setLineDashes(8, 8);
        for (int i = 1; i < n; i++) {
            double y = top + i * laneH;
            g.strokeLine(sx, y, w, y);
        }
        g.setLineDashes((double[]) null);

        // Distance markers
        g.setFont(Font.font("System", FontWeight.BOLD, 10));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        for (int q = 1; q <= 3; q++) {
            double x = sx + (fx - sx) * q / 4.0;
            g.setStroke(Color.color(1, 1, 1, 0.18));
            g.setLineWidth(1);
            g.strokeLine(x, top, x, bottom);
            g.setFill(pal.dark() ? Color.color(0, 0, 0, 0.55) : Color.color(0, 0, 0, 0.45));
            g.fillText((distance - distance * q / 4) + "m", x, 0.5);
        }

        // Start line
        g.setStroke(Color.color(1, 1, 1, 0.85));
        g.setLineWidth(2.5);
        g.strokeLine(sx, top, sx, bottom);

        // Finish line (checkered)
        double sq = 6;
        for (double y = top, r = 0; y < bottom; y += sq, r++) {
            for (int c = 0; c < 2; c++) {
                g.setFill(((int) r + c) % 2 == 0 ? Color.WHITE : Color.web("#111111"));
                g.fillRect(fx + c * sq - sq, y, sq, Math.min(sq, bottom - y));
            }
        }
        g.setFill(pal.dark() ? Color.color(0, 0, 0, 0.55) : Color.color(0, 0, 0, 0.45));
        g.fillText("FINISH", fx, 0.5);

        // Lanes: gate label + runner
        for (int i = 0; i < lanes.size(); i++) {
            Lane lane = lanes.get(i);
            double cy = top + i * laneH + laneH / 2;
            drawRunner(g, pal, lane, sx, fx, cy, laneH);
        }
    }

    private void drawRunner(GraphicsContext g, Palette pal, Lane lane, double sx, double fx, double cy, double laneH) {
        double tokenH = Math.max(20, Math.min(38, laneH * 0.46));
        double tokenW = tokenH * 1.75;
        double p = lane.progress;
        double noseX = sx + p * (fx - sx);
        double x = noseX - tokenW;
        double bob = running && lane.rank == 0 ? Math.sin(lane.phase) * 1.6 : 0;
        double y = cy - tokenH / 2 + bob;
        Color c = pal.group(lane.group);

        // Dust trail while galloping
        if (running && lane.rank == 0 && p > 0.01) {
            for (int k = 1; k <= 4; k++) {
                double a = 0.16 - k * 0.03;
                double rr = tokenH * (0.22 + k * 0.05);
                double ox = x - k * tokenH * 0.42 + Math.sin(lane.phase * 0.7 + k) * 2;
                g.setFill(Color.color(0.92, 0.88, 0.78, Math.max(0, a)));
                g.fillOval(ox - rr, cy + tokenH * 0.18 - rr, rr * 2, rr * 2);
            }
        }

        // Horse name only: trails the runner when there is room, otherwise sits just ahead of it.
        Font nameFont = Font.font("System", FontWeight.BOLD, Math.max(10, Math.min(13, laneH * 0.22)));
        g.setFont(nameFont);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Color.color(1, 1, 1, 0.9));
        if (x - sx > textWidth(lane.name, nameFont) + 14) {
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(lane.name, x - 8, cy + bob);
        } else if (lane.rank == 0) {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(lane.name, x + tokenW + 10, cy + bob);
        }

        // Shadow
        g.setFill(Color.color(0, 0, 0, 0.22));
        g.fillRoundRect(x + 2, y + 3, tokenW, tokenH, tokenH * 0.6, tokenH * 0.6);
        // Saddle cloth
        g.setFill(c);
        g.fillRoundRect(x, y, tokenW, tokenH, tokenH * 0.6, tokenH * 0.6);
        g.setStroke(Color.color(1, 1, 1, 0.55));
        g.setLineWidth(1.2);
        g.strokeRoundRect(x + 0.6, y + 0.6, tokenW - 1.2, tokenH - 1.2, tokenH * 0.6, tokenH * 0.6);
        // Number
        g.setFill(Color.color(1, 1, 1, 0.95));
        g.setFont(Font.font("System", FontWeight.BOLD, tokenH * 0.48));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(String.valueOf(lane.group.ordinal() + 1), x + tokenH * 0.48, y + tokenH / 2 + 0.5);
        // Horse head silhouette at the front
        Image head = Images.tinted(Images.HORSE, Color.color(1, 1, 1, 0.95));
        if (head != null) {
            double s = tokenH * 0.86;
            g.drawImage(head, x + tokenW - s - tokenH * 0.12, y + (tokenH - s) / 2, s, s);
        }

        // Finishing badge past the line
        if (lane.rank > 0) {
            String txt = FileExportService.ordinal(lane.rank);
            double bw = Math.max(34, tokenH * 1.45), bh = tokenH * 0.8;
            double bx = Math.min(canvas.getWidth() - bw - 4, fx + 8);
            double by = cy - bh / 2;
            boolean winner = lane.rank == 1;
            g.setFill(winner ? pal.accent() : Color.color(0, 0, 0, 0.45));
            g.fillRoundRect(bx, by, bw, bh, bh, bh);
            g.setFill(winner ? Color.web("#1B1D1A") : Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, Math.max(10, bh * 0.5)));
            g.fillText(txt, bx + bw / 2, cy + 0.5);
        }
        g.setStroke(null);
        g.setLineCap(StrokeLineCap.SQUARE);
    }

    private static final Text MEASURE = new Text();

    private static double textWidth(String s, Font f) {
        MEASURE.setText(s);
        MEASURE.setFont(f);
        return MEASURE.getLayoutBounds().getWidth();
    }
}
