package org.example.horse_racing.domain;

/**
 * One runner's result in a race.
 *
 * @param horse    the runner
 * @param lane     0-based lane (A=0 ... D=3)
 * @param time     finishing time in seconds
 * @param rank     1-based finishing position
 * @param odds     pre-race decimal odds (e.g. 2.75)
 */
public record RaceEntry(Horse horse, int lane, double time, int rank, double odds) {

    public String formattedTime() {
        return String.format("%.2fs", time);
    }
}
