package org.example.horse_racing.domain;

/**
 * Represents the competitive group classification for horses.
 */
public enum HorseGroup {
    A("Group A"),
    B("Group B"),
    C("Group C"),
    D("Group D");

    private final String displayName;

    HorseGroup(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static HorseGroup fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim().toUpperCase();
        return switch (trimmed) {
            case "A", "GROUP A" -> A;
            case "B", "GROUP B" -> B;
            case "C", "GROUP C" -> C;
            case "D", "GROUP D" -> D;
            default -> null;
        };
    }
}
