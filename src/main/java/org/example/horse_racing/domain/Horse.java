package org.example.horse_racing.domain;

import java.util.Objects;

/**
 * Immutable domain model representing a horse in the racing system.
 */
public record Horse(
        String id,
        String name,
        String jockey,
        int age,
        String breed,
        String raceRecord,
        HorseGroup group,
        int seconds,
        String imagePath
) {
    public Horse {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(jockey, "jockey cannot be null");
        Objects.requireNonNull(breed, "breed cannot be null");
        Objects.requireNonNull(raceRecord, "raceRecord cannot be null");
        Objects.requireNonNull(group, "group cannot be null");
    }

    public Horse(String id, String name, String jockey, int age, String breed, String raceRecord, HorseGroup group, int seconds) {
        this(id, name, jockey, age, breed, raceRecord, group, seconds, "");
    }

    public Horse withRaceTime(int raceTime) {
        return new Horse(id, name, jockey, age, breed, raceRecord, group, raceTime, imagePath);
    }

    public Horse withUpdatedDetails(String name, String jockey, int age, String breed,
                                    String raceRecord, HorseGroup group, String imagePath) {
        return new Horse(id, name, jockey, age, breed, raceRecord, group, seconds,
                imagePath != null ? imagePath : this.imagePath);
    }
}
