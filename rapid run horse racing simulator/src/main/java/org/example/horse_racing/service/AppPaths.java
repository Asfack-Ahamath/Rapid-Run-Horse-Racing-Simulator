package org.example.horse_racing.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves where the application keeps its writable data.
 * <p>
 * Data lives in a {@code data} folder inside the current working directory — the project
 * folder when started with {@code mvn javafx:run}, or the folder next to the jar.
 * Override with the system property {@code rapidrun.dataDir}.
 */
public final class AppPaths {

    private AppPaths() {}

    public static Path dataDir() {
        String override = System.getProperty("rapidrun.dataDir");
        Path dir = override != null && !override.isBlank()
                ? Path.of(override)
                : Path.of(System.getProperty("user.dir"), "data");
        dir = dir.toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            migrateFromHomeFolder(dir);
        } catch (IOException ignored) {
            // Fall through; callers will surface write errors.
        }
        return dir;
    }

    /** Location used by version 1.0 of the app (inside the source tree). */
    public static Path legacyHorsesFile() {
        return Path.of(System.getProperty("user.dir"))
                .resolve("src").resolve("main").resolve("java")
                .resolve("org").resolve("example").resolve("horse_racing")
                .resolve("data").resolve("horses.json");
    }

    /** Version 2.0 kept data in ~/.rapid-run; bring it across once so nothing is lost. */
    private static void migrateFromHomeFolder(Path dir) throws IOException {
        Path old = Path.of(System.getProperty("user.home"), ".rapid-run");
        if (!Files.isDirectory(old) || old.toAbsolutePath().normalize().equals(dir)) return;
        for (String name : new String[]{"horses.json", "races.json"}) {
            Path src = old.resolve(name), dst = dir.resolve(name);
            if (Files.exists(src) && !Files.exists(dst)) Files.copy(src, dst);
        }
    }
}
