package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.repository.HorseRepository;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Service layer for horse management operations.
 */
public class HorseService {

    private static final int MIN_AGE = 1;
    private static final int MAX_AGE = 30;
    private static final int RANDOM_SECONDS_MIN = 1;
    private static final int RANDOM_SECONDS_MAX = 90;

    private final HorseRepository repository;
    private final Random random = new Random();

    public HorseService(HorseRepository repository) {
        this.repository = repository;
    }

    public List<Horse> getAllHorses() {
        return repository.findAll();
    }

    public Optional<Horse> getHorseById(String id) {
        return repository.findById(id);
    }

    public Horse addHorse(String id, String name, String jockey, int age, String breed,
                         String raceRecord, HorseGroup group, String imagePath) {
        int seconds = random.nextInt(RANDOM_SECONDS_MAX - RANDOM_SECONDS_MIN + 1) + RANDOM_SECONDS_MIN;
        Horse horse = new Horse(id.trim().toUpperCase(), name, jockey, age, breed, raceRecord, group, seconds,
                imagePath != null ? imagePath : "");
        return repository.save(horse);
    }

    public Horse updateHorse(String id, String name, String jockey, int age, String breed,
                            String raceRecord, HorseGroup group, String imagePath) {
        Horse existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horse not found with ID: " + id));
        Horse updated = existing.withUpdatedDetails(name, jockey, age, breed, raceRecord, group, imagePath);
        return repository.save(updated);
    }

    public void deleteHorse(String id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Horse not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Next numeric ID: max existing numeric id + 1, or {@code "1"} if there are no horses.
     * Non-digit characters in stored ids are stripped when computing the max.
     */
    public String nextGeneratedId() {
        int max = repository.findAll().stream()
                .mapToInt(h -> numericIdFromString(h.id()))
                .max()
                .orElse(0);
        if (max >= Integer.MAX_VALUE) {
            throw new IllegalStateException("Cannot allocate horse ID: limit reached.");
        }
        return String.valueOf(max + 1);
    }

    private static int numericIdFromString(String id) {
        if (id == null || id.isBlank()) {
            return 0;
        }
        String digits = id.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("ID is required.");
        }
        if (!id.matches("\\d+") || Integer.parseInt(id.trim()) <= 0) {
            throw new ValidationException("ID must be a positive integer.");
        }
    }

    public static void validateAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException("Age must be between " + MIN_AGE + " and " + MAX_AGE + " years.");
        }
    }

    public static void validateRequiredFields(String name, String jockey, String breed, String raceRecord) {
        if (name == null || name.isBlank()) throw new ValidationException("Name is required.");
        if (jockey == null || jockey.isBlank()) throw new ValidationException("Jockey is required.");
        if (breed == null || breed.isBlank()) throw new ValidationException("Breed is required.");
        if (raceRecord == null || raceRecord.isBlank()) throw new ValidationException("Race record is required.");
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
