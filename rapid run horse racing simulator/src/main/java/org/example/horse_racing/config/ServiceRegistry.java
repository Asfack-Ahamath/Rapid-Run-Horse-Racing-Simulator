package org.example.horse_racing.config;

import org.example.horse_racing.repository.HorseRepository;
import org.example.horse_racing.repository.JsonBackedHorseRepository;
import org.example.horse_racing.service.FileExportService;
import org.example.horse_racing.service.HorseService;
import org.example.horse_racing.service.RaceHistoryService;
import org.example.horse_racing.service.RaceService;

/**
 * Central registry for application services (simple DI).
 */
public final class ServiceRegistry {

    private static final HorseRepository horseRepository = new JsonBackedHorseRepository();
    private static final HorseService horseService = new HorseService(horseRepository);
    private static final RaceService raceService = new RaceService(horseRepository);
    private static final RaceHistoryService raceHistoryService = new RaceHistoryService();
    private static final FileExportService fileExportService = new FileExportService();

    private ServiceRegistry() {}

    public static HorseService getHorseService() {
        return horseService;
    }

    public static RaceService getRaceService() {
        return raceService;
    }

    public static RaceHistoryService getRaceHistoryService() {
        return raceHistoryService;
    }

    public static FileExportService getFileExportService() {
        return fileExportService;
    }

    public static HorseRepository getHorseRepository() {
        return horseRepository;
    }
}
