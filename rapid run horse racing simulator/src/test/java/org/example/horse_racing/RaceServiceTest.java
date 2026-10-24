package org.example.horse_racing;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.domain.RaceRecord;
import org.example.horse_racing.domain.RaceResult;
import org.example.horse_racing.repository.InMemoryHorseRepository;
import org.example.horse_racing.service.RaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RaceServiceTest {

    private InMemoryHorseRepository repo;
    private RaceService raceService;

    @BeforeEach
    void setUp() {
        repo = new InMemoryHorseRepository();
        raceService = new RaceService(repo, new Random(42));
    }

    private void seedAllGroups() {
        repo.save(new Horse("1", "Ace", "J1", 5, "Arabian", "16 wins, 0 losses", HorseGroup.A, 0));
        repo.save(new Horse("2", "Bolt", "J2", 6, "Arabian", "8 wins, 8 losses", HorseGroup.B, 0));
        repo.save(new Horse("3", "Cloud", "J3", 14, "Arabian", "1 win, 15 losses", HorseGroup.C, 0));
        repo.save(new Horse("4", "Dash", "J4", 4, "Arabian", "5 wins, 5 losses", HorseGroup.D, 0));
    }

    @Test
    void raceRecord_parsesWinsAndLosses() {
        RaceRecord r = RaceRecord.parse("14 wins, 1 loss");
        assertEquals(14, r.wins());
        assertEquals(1, r.losses());
        assertEquals("14 wins, 1 loss", r.format());
        assertEquals(0, RaceRecord.parse("no data").starts());
    }

    @Test
    void selectHorses_missingGroup_throws() {
        repo.save(new Horse("1", "Ace", "J1", 5, "Arabian", "1 win, 0 losses", HorseGroup.A, 0));
        assertThrows(IllegalStateException.class, () -> raceService.selectHorsesForRace());
        assertEquals(3, raceService.getEmptyGroups().size());
    }

    @Test
    void runRace_ranksAreSequentialAndTimesAscending() {
        seedAllGroups();
        RaceResult result = raceService.runRace(raceService.selectHorsesForRace());
        assertEquals(4, result.entries().size());
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, result.entries().get(i).rank());
            if (i > 0) assertTrue(result.entries().get(i).time() > result.entries().get(i - 1).time());
        }
        assertEquals(0.0, result.gapToWinner(result.winner()));
    }

    @Test
    void strongerHorse_hasShorterOdds_andWinsMoreOften() {
        seedAllGroups();
        Map<HorseGroup, Horse> field = raceService.selectHorsesForRace();
        Map<HorseGroup, Double> odds = raceService.odds(field);
        assertTrue(odds.get(HorseGroup.A) < odds.get(HorseGroup.C));

        int aceWins = 0, cloudWins = 0;
        for (int i = 0; i < 500; i++) {
            String winner = raceService.runRace(field).winner().horse().name();
            if (winner.equals("Ace")) aceWins++;
            if (winner.equals("Cloud")) cloudWins++;
        }
        assertTrue(aceWins > cloudWins, "Ace " + aceWins + " vs Cloud " + cloudWins);
    }
}
