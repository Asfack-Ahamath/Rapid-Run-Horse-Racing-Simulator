package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceEntry;
import org.example.horse_racing.domain.RaceRecord;
import org.example.horse_racing.domain.RaceResult;
import org.example.horse_racing.repository.HorseRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Race selection and simulation.
 * <p>
 * Finishing times are driven by each horse's <em>rating</em> (form from its race record and
 * an age curve that peaks around 4–7 years) plus random race-day variance, so stronger horses
 * win more often without the result ever being certain.
 */
public class RaceService {

    public static final int DISTANCE_METERS = 1200;

    private static final int RACE_TIME_MIN = 1;
    private static final int RACE_TIME_MAX = 91;
    private static final List<HorseGroup> REQUIRED_GROUPS = List.of(HorseGroup.values());

    private static final double BASE_TIME = 71.0;      // seconds for an average horse over 1200m
    private static final double RATING_SPREAD = 3.6;   // seconds gained by a perfect-rated horse
    private static final double VARIANCE = 1.15;       // race-day noise (std-dev, seconds)
    private static final double BOOK_MARGIN = 0.92;    // bookmaker overround applied to fair odds

    private final HorseRepository repository;
    private final Random random;

    public RaceService(HorseRepository repository) {
        this(repository, new Random());
    }

    public RaceService(HorseRepository repository, Random random) {
        this.repository = repository;
        this.random = random;
    }

    /** Selects one random horse from each group, in group order A–D. */
    public Map<HorseGroup, Horse> selectHorsesForRace() {
        Map<HorseGroup, Horse> selected = new EnumMap<>(HorseGroup.class);
        for (HorseGroup group : REQUIRED_GROUPS) {
            List<Horse> horsesInGroup = repository.findByGroup(group);
            if (horsesInGroup.isEmpty()) {
                throw new IllegalStateException("Group " + group + " has no horses. Add at least one horse per group.");
            }
            selected.put(group, horsesInGroup.get(random.nextInt(horsesInGroup.size())));
        }
        return selected;
    }

    /** Rating in 0..1 combining smoothed win rate and an age curve. */
    public double rating(Horse h) {
        RaceRecord r = RaceRecord.parse(h.raceRecord());
        double form = (r.wins() + 1.0) / (r.starts() + 2.0);          // Laplace-smoothed win rate
        double age = 1.0 - Math.min(1.0, Math.abs(h.age() - 5.5) / 8.0);
        return clamp(0.7 * form + 0.3 * age, 0, 1);
    }

    /** Decimal odds for each runner in the field. */
    public Map<HorseGroup, Double> odds(Map<HorseGroup, Horse> field) {
        Map<HorseGroup, Double> strength = new EnumMap<>(HorseGroup.class);
        double total = 0;
        for (var e : field.entrySet()) {
            double s = Math.exp(rating(e.getValue()) * 4.5);
            strength.put(e.getKey(), s);
            total += s;
        }
        Map<HorseGroup, Double> odds = new EnumMap<>(HorseGroup.class);
        for (var e : strength.entrySet()) {
            double p = e.getValue() / total;
            double o = Math.max(1.10, (1.0 / p) * BOOK_MARGIN);
            odds.put(e.getKey(), Math.round(o * 20) / 20.0); // nearest 0.05
        }
        return odds;
    }

    /** Runs a full race and returns the ranked result. */
    public RaceResult runRace(Map<HorseGroup, Horse> field) {
        Map<HorseGroup, Double> odds = odds(field);
        record Run(HorseGroup g, Horse h, double t) {}
        List<Run> runs = new ArrayList<>();
        for (var e : field.entrySet()) {
            double t = BASE_TIME - RATING_SPREAD * rating(e.getValue()) + random.nextGaussian() * VARIANCE;
            t = Math.round(Math.max(64.0, t) * 100) / 100.0;
            runs.add(new Run(e.getKey(), e.getValue(), t));
        }
        // Dead heats are broken by a photo finish (random nudge of 0.01s).
        runs.sort(Comparator.comparingDouble(Run::t));
        List<RaceEntry> entries = new ArrayList<>();
        double prev = -1;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            double t = r.t() <= prev ? prev + 0.01 : r.t();
            prev = t;
            entries.add(new RaceEntry(r.h().withRaceTime((int) Math.round(t)), r.g().ordinal(), t, i + 1, odds.get(r.g())));
        }
        return new RaceResult(LocalDateTime.now(), DISTANCE_METERS, entries);
    }

    /** Legacy simple simulation kept for compatibility: integer times, uniform random. */
    public List<Horse> simulateRace(Map<HorseGroup, Horse> selectedHorses) {
        return selectedHorses.values().stream()
                .map(h -> h.withRaceTime(random.nextInt(RACE_TIME_MAX - RACE_TIME_MIN) + RACE_TIME_MIN))
                .sorted(Comparator.comparingInt(Horse::seconds))
                .toList();
    }

    public List<Horse> getTopWinners(List<Horse> raceResults, int topCount) {
        return raceResults.stream().limit(topCount).toList();
    }

    public Set<HorseGroup> getEmptyGroups() {
        return REQUIRED_GROUPS.stream()
                .filter(g -> repository.findByGroup(g).isEmpty())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(HorseGroup.class)));
    }

    public boolean canSelectHorses() {
        return getEmptyGroups().isEmpty();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
