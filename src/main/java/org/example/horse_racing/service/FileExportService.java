package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting horse data to text files.
 */
public class FileExportService {

    public void exportToTextFile(List<Horse> horses, Path outputPath) throws IOException {
        List<Horse> sorted = horses.stream()
                .sorted(Comparator.comparing((Horse h) -> h.group().name()).thenComparing(h -> Integer.parseInt(h.id())))
                .toList();

        Map<HorseGroup, List<Horse>> grouped = sorted.stream().collect(Collectors.groupingBy(Horse::group));

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            writer.println("=============================================================================================");
            writer.println("                                    HORSE DETAILS TABLE");
            writer.println("                                    Rapid Run - Industry Standard");
            writer.println("=============================================================================================\n");

            for (HorseGroup group : List.of(HorseGroup.A, HorseGroup.B, HorseGroup.C, HorseGroup.D)) {
                List<Horse> horsesInGroup = grouped.getOrDefault(group, List.of());
                if (horsesInGroup.isEmpty()) continue;

                writer.printf("%38s%s\n", "", "=".repeat(14));
                writer.printf("%41s%s\n", "", group);
                writer.printf("%38s%s\n\n", "", "=".repeat(14));

                writer.printf("%-10s %-20s %-20s %-5s %-15s %-20s %-7s\n",
                        "ID", "Name", "Jockey", "Age", "Breed", "Race Record", "Seconds");
                writer.println("─".repeat(104));

                for (Horse horse : horsesInGroup) {
                    writer.printf("%-10s %-20s %-20s %-5s %-15s %-20s %-7s\n",
                            horse.id(), horse.name(), horse.jockey(),
                            horse.age(), horse.breed(), horse.raceRecord(),
                            horse.seconds());
                    writer.println("─".repeat(104));
                }
                writer.println();
            }
        }
    }
}
