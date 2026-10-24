package org.example.horse_racing.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * The outcome of a single race. Entries are ordered by finishing position.
 */
public record RaceResult(LocalDateTime runAt, int distanceMeters, List<RaceEntry> entries) {

    public RaceResult {
        entries = entries.stream().sorted(Comparator.comparingInt(RaceEntry::rank)).toList();
    }

    public RaceEntry winner() {
        return entries.get(0);
    }

    /** Seconds behind the winner. */
    public double gapToWinner(RaceEntry e) {
        return e.time() - winner().time();
    }

    /** Entries in lane order (A, B, C, D). */
    public List<RaceEntry> byLane() {
        return entries.stream().sorted(Comparator.comparingInt(RaceEntry::lane)).toList();
    }
}
