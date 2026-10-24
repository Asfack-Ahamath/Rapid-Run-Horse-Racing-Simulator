package org.example.horse_racing.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed form of a horse's free-text race record such as {@code "8 wins, 5 losses"}.
 */
public record RaceRecord(int wins, int losses) {

    private static final Pattern WINS = Pattern.compile("(\\d+)\\s*win", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOSSES = Pattern.compile("(\\d+)\\s*loss", Pattern.CASE_INSENSITIVE);

    public static RaceRecord parse(String text) {
        if (text == null) return new RaceRecord(0, 0);
        return new RaceRecord(find(WINS, text), find(LOSSES, text));
    }

    private static int find(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    public int starts() {
        return wins + losses;
    }

    /** Win rate in 0..1, or 0 when the horse has no starts. */
    public double winRate() {
        return starts() == 0 ? 0 : (double) wins / starts();
    }

    public String format() {
        return wins + (wins == 1 ? " win, " : " wins, ") + losses + (losses == 1 ? " loss" : " losses");
    }
}
