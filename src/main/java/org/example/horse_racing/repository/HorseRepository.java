package org.example.horse_racing.repository;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for horse data persistence.
 */
public interface HorseRepository {

    List<Horse> findAll();

    Optional<Horse> findById(String id);

    List<Horse> findByGroup(HorseGroup group);

    boolean existsById(String id);

    Horse save(Horse horse);

    void deleteById(String id);

    void clear();
}
