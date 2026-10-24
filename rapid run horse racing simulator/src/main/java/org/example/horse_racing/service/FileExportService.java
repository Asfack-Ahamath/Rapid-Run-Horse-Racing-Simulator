package org.example.horse_racing.service;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceEntry;
import org.example.horse_racing.domain.RaceResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports the roster (and optionally the latest race) to a plain-text report.
 */
public class FileExportService {

    private static final int WIDTH = 104;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void exportToTextFile(List<Horse> horses, Path outputPath) throws IOException {
        exportReport(horses, null, outputPath);
    }

    public void exportReport(List<Horse> horses, RaceResult latestRace, Path outputPath) throws IOException {
        List<Horse> sorted = horses.stream()
                .sorted(Comparator.comparing((Horse h) -> h.group().name()).thenComparingInt(h -> numeric(h.id())))
                .toList();
        Map<HorseGroup, List<Horse>> grouped = sorted.stream().collect(Collectors.groupingBy(Horse::group));

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            w.println("=".repeat(WIDTH));
            w.println(center("RAPID RUN - HORSE DETAILS REPORT"));
            w.println("=".repeat(WIDTH));
            w.println();

            for (HorseGroup group : HorseGroup.values()) {
                List<Horse> inGroup = grouped.getOrDefault(group, List.of());
                if (inGroup.isEmpty()) continue;
                w.println(center("[ " + group + " ]"));
                w.println();
                w.printf("%-8s %-20s %-20s %-5s %-16s %-22s%n", "ID", "Name", "Jockey", "Age", "Breed", "Race Record");
                w.println("-".repeat(WIDTH));
                for (Horse h : inGroup) {
                    w.printf("%-8s %-20s %-20s %-5d %-16s %-22s%n",
                            h.id(), cut(h.name(), 20), cut(h.jockey(), 20), h.age(), cut(h.breed(), 16), cut(h.raceRecord(), 22));
                }
                w.println();
            }

            if (latestRace != null) {
                w.println("=".repeat(WIDTH));
                w.println(center("LATEST RACE — " + latestRace.distanceMeters() + "m — " + WHEN.format(latestRace.runAt())));
                w.println("=".repeat(WIDTH));
                w.printf("%-6s %-22s %-20s %-8s %-10s %-10s %-6s%n", "Pos", "Horse", "Jockey", "Group", "Time", "Gap", "Odds");
                w.println("-".repeat(WIDTH));
                for (RaceEntry e : latestRace.entries()) {
                    double gap = latestRace.gapToWinner(e);
                    w.printf("%-6s %-22s %-20s %-8s %-10s %-10s %-6.2f%n",
                            ordinal(e.rank()), cut(e.horse().name(), 22), cut(e.horse().jockey(), 20),
                            e.horse().group().name(), e.formattedTime(), gap == 0 ? "—" : String.format("+%.2fs", gap), e.odds());
                }
            }
        }
    }

    public static String ordinal(int n) {
        return n + switch (n % 100 >= 11 && n % 100 <= 13 ? 0 : n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private static String center(String s) {
        int pad = Math.max(0, (WIDTH - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    private static String cut(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    private static int numeric(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
