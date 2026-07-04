package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.repository.HorseRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for race simulation and horse selection logic.
 */
public class RaceService {

    private static final int RACE_TIME_MIN = 1;
    private static final int RACE_TIME_MAX = 91;
    private static final Set<HorseGroup> REQUIRED_GROUPS = Set.of(HorseGroup.A, HorseGroup.B, HorseGroup.C, HorseGroup.D);

    private final HorseRepository repository;
    private final Random random = new Random();

    public RaceService(HorseRepository repository) {
        this.repository = repository;
    }

    /**
     * Selects one horse from each group for the race.
     */
    public Map<HorseGroup, Horse> selectHorsesForRace() {
        Map<HorseGroup, Horse> selected = new HashMap<>();
        for (HorseGroup group : REQUIRED_GROUPS) {
            List<Horse> horsesInGroup = repository.findByGroup(group);
            if (horsesInGroup.isEmpty()) {
                throw new IllegalStateException("Group " + group + " has no horses. Add at least one horse per group.");
            }
            Horse chosen = horsesInGroup.get(random.nextInt(horsesInGroup.size()));
            selected.put(group, chosen);
        }
        return selected;
    }

    /**
     * Simulates a race and returns horses ranked by finish time.
     */
    public List<Horse> simulateRace(Map<HorseGroup, Horse> selectedHorses) {
        List<Horse> participants = new ArrayList<>(selectedHorses.values());
        List<Horse> withTimes = participants.stream()
                .map(h -> h.withRaceTime(random.nextInt(RACE_TIME_MAX - RACE_TIME_MIN) + RACE_TIME_MIN))
                .toList();
        return withTimes.stream()
                .sorted(Comparator.comparingInt(Horse::seconds))
                .toList();
    }

    /**
     * Returns the top 3 winners from race results.
     */
    public List<Horse> getTopWinners(List<Horse> raceResults, int topCount) {
        return raceResults.stream().limit(topCount).toList();
    }

    /**
     * Checks if all groups have at least one horse.
     */
    public Set<HorseGroup> getEmptyGroups() {
        return REQUIRED_GROUPS.stream()
                .filter(g -> repository.findByGroup(g).isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean canSelectHorses() {
        return getEmptyGroups().isEmpty();
    }
}
