package org.example.horse_racing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and saves horse data to/from a writable JSON file.
 * Uses src/main/java/org/example/horse_racing/data/horses.json for persistence.
 */
public class HorseJsonPersistence {

    private static final String DATA_FILE = "horses.json";

    private final Path dataPath;
    private final ObjectMapper mapper;

    public HorseJsonPersistence() {
        this.dataPath = Path.of(System.getProperty("user.dir"))  // project root
                .resolve("src").resolve("main").resolve("java")
                .resolve("org").resolve("example").resolve("horse_racing")
                .resolve("data").resolve(DATA_FILE);
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path getDataPath() {
        return dataPath;
    }

    /**
     * Load horses from the writable JSON file. If the file does not exist,
     * returns an empty list.
     */
    public List<Horse> load() throws IOException {
        if (Files.exists(dataPath)) {
            return loadFromFile(dataPath);
        }
        return List.of();
    }

    /**
     * Save horses to the writable JSON file.
     */
    public void save(List<Horse> horses) throws IOException {
        Files.createDirectories(dataPath.getParent());
        List<Map<String, Object>> items = horses.stream()
                .map(this::horseToMap)
                .toList();
        mapper.writeValue(dataPath.toFile(), items);
    }

    private List<Horse> loadFromFile(Path path) throws IOException {
        List<?> raw = mapper.readValue(path.toFile(), List.class);
        List<Horse> horses = new ArrayList<>();
        for (Object item : raw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;
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
            String imagePath = map.containsKey("imagePath") ? String.valueOf(map.get("imagePath")) : "";
            return new Horse(id, name, jockey, age, breed, raceRecord, group, 0, imagePath != null ? imagePath : "");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> horseToMap(Horse h) {
        return Map.of(
                "id", h.id(),
                "name", h.name(),
                "jockey", h.jockey(),
                "age", h.age(),
                "breed", h.breed(),
                "raceRecord", h.raceRecord(),
                "group", h.group().name(),
                "imagePath", h.imagePath() != null ? h.imagePath() : ""
        );
    }
}
