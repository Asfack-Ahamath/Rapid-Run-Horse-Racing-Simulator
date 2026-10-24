package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceEntry;
import org.example.horse_racing.domain.RaceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps a persisted log of completed races (most recent first) and derives simple stats.
 */
public class RaceHistoryService {

    private static final int MAX_RACES = 100;

    private final Path file;
    private final List<RaceResult> races = new CopyOnWriteArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public RaceHistoryService() {
        this(AppPaths.dataDir().resolve("races.json"));
    }

    public RaceHistoryService(Path file) {
        this.file = file;
        load();
    }

    public List<RaceResult> all() {
        return List.copyOf(races);
    }

    public Optional<RaceResult> latest() {
        return races.isEmpty() ? Optional.empty() : Optional.of(races.get(0));
    }

    public int count() {
        return races.size();
    }

    public void record(RaceResult result) {
        races.add(0, result);
        while (races.size() > MAX_RACES) races.remove(races.size() - 1);
        save();
        listeners.forEach(Runnable::run);
    }

    public void clear() {
        races.clear();
        save();
        listeners.forEach(Runnable::run);
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    /** Wins per horse name, highest first. */
    public List<Map.Entry<String, Integer>> leaderboard() {
        Map<String, Integer> wins = new HashMap<>();
        for (RaceResult r : races) wins.merge(r.winner().horse().name(), 1, Integer::sum);
        return wins.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .toList();
    }

    /** Wins per group across all recorded races. */
    public Map<HorseGroup, Integer> winsByGroup() {
        Map<HorseGroup, Integer> m = new EnumMap<>(HorseGroup.class);
        for (HorseGroup g : HorseGroup.values()) m.put(g, 0);
        for (RaceResult r : races) m.merge(r.winner().horse().group(), 1, Integer::sum);
        return m;
    }

    public OptionalDouble fastestTime() {
        return races.stream().mapToDouble(r -> r.winner().time()).min();
    }

    // ---------------------------------------------------------------- persistence

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<?> raw = (List<?>) Json.read(file);
            for (Object o : raw) {
                RaceResult r = fromMap((Map<?, ?>) o);
                if (r != null) races.add(r);
            }
        } catch (Exception ignored) {
            // Corrupt history is not fatal; start fresh.
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            List<Map<String, Object>> out = races.stream().map(this::toMap).toList();
            Json.write(file, out);
        } catch (IOException ignored) {
            // History is best-effort.
        }
    }

    private Map<String, Object> toMap(RaceResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runAt", r.runAt().toString());
        m.put("distance", r.distanceMeters());
        List<Map<String, Object>> entries = new ArrayList<>();
        for (RaceEntry e : r.entries()) {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("id", e.horse().id());
            em.put("name", e.horse().name());
            em.put("jockey", e.horse().jockey());
            em.put("group", e.horse().group().name());
            em.put("lane", e.lane());
            em.put("time", e.time());
            em.put("rank", e.rank());
            em.put("odds", e.odds());
            entries.add(em);
        }
        m.put("entries", entries);
        return m;
    }

    private RaceResult fromMap(Map<?, ?> m) {
        try {
            LocalDateTime at = LocalDateTime.parse(String.valueOf(m.get("runAt")));
            int distance = ((Number) m.get("distance")).intValue();
            List<RaceEntry> entries = new ArrayList<>();
            for (Object o : (List<?>) m.get("entries")) {
                Map<?, ?> em = (Map<?, ?>) o;
                HorseGroup g = HorseGroup.fromString(String.valueOf(em.get("group")));
                double time = ((Number) em.get("time")).doubleValue();
                Horse h = new Horse(String.valueOf(em.get("id")), String.valueOf(em.get("name")),
                        String.valueOf(em.get("jockey")), 0, "", "", g, (int) Math.round(time));
                entries.add(new RaceEntry(h, ((Number) em.get("lane")).intValue(), time,
                        ((Number) em.get("rank")).intValue(), ((Number) em.get("odds")).doubleValue()));
            }
            return entries.isEmpty() ? null : new RaceResult(at, distance, entries);
        } catch (Exception e) {
            return null;
        }
    }
}
