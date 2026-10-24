package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and saves horse data to/from a writable JSON file.
 * <p>
 * The file lives in {@link AppPaths#dataDir()}. On first launch it is seeded from the
 * legacy in-source file (if present) or from the bundled sample data.
 */
public class HorseJsonPersistence {

    private static final String DATA_FILE = "horses.json";
    private static final String BUNDLED_SEED = "/org/example/horse_racing/data/horses.json";

    private final Path dataPath;

    public HorseJsonPersistence() {
        this(AppPaths.dataDir().resolve(DATA_FILE));
    }

    public HorseJsonPersistence(Path dataPath) {
        this.dataPath = dataPath;
    }

    public Path getDataPath() {
        return dataPath;
    }

    public List<Horse> load() throws IOException {
        seedIfMissing();
        if (Files.exists(dataPath)) {
            return loadFromFile(dataPath);
        }
        return List.of();
    }

    public void save(List<Horse> horses) throws IOException {
        Files.createDirectories(dataPath.getParent());
        List<Map<String, Object>> items = horses.stream().map(this::horseToMap).toList();
        // Write atomically so a crash mid-write never corrupts the roster.
        Path tmp = dataPath.resolveSibling(DATA_FILE + ".tmp");
        Json.write(tmp, items);
        Files.move(tmp, dataPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void seedIfMissing() {
        if (Files.exists(dataPath)) return;
        try {
            Files.createDirectories(dataPath.getParent());
            Path legacy = AppPaths.legacyHorsesFile();
            if (Files.exists(legacy)) {
                Files.copy(legacy, dataPath);
                return;
            }
            try (InputStream in = HorseJsonPersistence.class.getResourceAsStream(BUNDLED_SEED)) {
                if (in != null) Files.copy(in, dataPath);
            }
        } catch (IOException ignored) {
            // Start with an empty roster if seeding fails.
        }
    }

    private List<Horse> loadFromFile(Path path) throws IOException {
        Object parsed;
        try {
            parsed = Json.read(path);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid JSON in " + path + ": " + e.getMessage(), e);
        }
        if (!(parsed instanceof List<?> raw)) return List.of();
        List<Horse> horses = new ArrayList<>();
        for (Object item : raw) {
            if (!(item instanceof Map<?, ?> m)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) m;
            Horse h = mapToHorse(map);
            if (h != null) horses.add(h);
        }
        return horses;
    }

    private Horse mapToHorse(Map<String, Object> map) {
        try {
            String id = String.valueOf(map.get("id"));
            String name = String.valueOf(map.get("name"));
            String jockey = String.valueOf(map.get("jockey"));
            int age = map.get("age") instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(map.get("age")));
            String breed = String.valueOf(map.get("breed"));
            String raceRecord = map.containsKey("raceRecord") ? String.valueOf(map.get("raceRecord")) : "";
            HorseGroup group = HorseGroup.fromString(String.valueOf(map.get("group")));
            if (group == null) return null;
            Object img = map.get("imagePath");
            String imagePath = img == null ? "" : String.valueOf(img);
            return new Horse(id, name, jockey, age, breed, raceRecord, group, 0, imagePath);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> horseToMap(Horse h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.id());
        m.put("name", h.name());
        m.put("jockey", h.jockey());
        m.put("age", h.age());
        m.put("breed", h.breed());
        m.put("raceRecord", h.raceRecord());
        m.put("group", h.group().name());
        m.put("imagePath", h.imagePath() != null ? h.imagePath() : "");
        return m;
    }
}
