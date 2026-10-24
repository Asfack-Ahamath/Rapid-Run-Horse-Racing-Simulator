package org.example.horse_racing;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.repository.InMemoryHorseRepository;
import org.example.horse_racing.service.HorseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HorseServiceTest {

    private HorseService horseService;

    @BeforeEach
    void setUp() {
        horseService = new HorseService(new InMemoryHorseRepository());
    }

    @Test
    void addHorse_validInput_horseAdded() {
        Horse horse = horseService.addHorse("001", "Silver", "Peter", 5, "Thoroughbred", "5 wins", HorseGroup.A, "");

        assertNotNull(horse);
        assertEquals("001", horse.id());
        assertEquals("Silver", horse.name());
        assertEquals("Peter", horse.jockey());
        assertEquals(5, horse.age());
        assertEquals(HorseGroup.A, horse.group());
        assertTrue(horseService.existsById("001"));
    }

    @Test
    void addHorse_duplicateId_throwsOrOverwrites() {
        horseService.addHorse("001", "Silver", "Peter", 5, "Thoroughbred", "5 wins", HorseGroup.A, "");
        Horse updated = horseService.addHorse("001", "Gold", "John", 6, "Arabian", "3 wins", HorseGroup.B, "");

        assertEquals("Gold", updated.name());
        assertEquals(1, horseService.getAllHorses().size());
    }

    @Test
    void deleteHorse_existingId_removesHorse() {
        horseService.addHorse("001", "Silver", "Peter", 5, "Thoroughbred", "5 wins", HorseGroup.A, "");
        horseService.deleteHorse("001");

        assertFalse(horseService.existsById("001"));
    }

    @Test
    void deleteHorse_nonExistingId_throws() {
        assertThrows(IllegalArgumentException.class, () -> horseService.deleteHorse("999"));
    }

    @Test
    void validateId_invalid_throws() {
        assertThrows(HorseService.ValidationException.class, () -> HorseService.validateId(""));
        assertThrows(HorseService.ValidationException.class, () -> HorseService.validateId("abc"));
    }

    @Test
    void nextGeneratedId_empty_isOne() {
        assertEquals("1", horseService.nextGeneratedId());
    }

    @Test
    void nextGeneratedId_oneGreaterThanMaxNumeric() {
        horseService.addHorse("7", "A", "J", 5, "B", "R", HorseGroup.A, "");
        horseService.addHorse("12", "B", "J", 5, "B", "R", HorseGroup.B, "");
        assertEquals("13", horseService.nextGeneratedId());
    }
}
