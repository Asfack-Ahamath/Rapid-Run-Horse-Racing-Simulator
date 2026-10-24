package org.example.horse_racing.repository;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of HorseRepository.
 */
public class InMemoryHorseRepository implements HorseRepository {

    private final List<Horse> horses = new CopyOnWriteArrayList<>();

    @Override
    public List<Horse> findAll() {
        return new ArrayList<>(horses);
    }

    @Override
    public Optional<Horse> findById(String id) {
        return horses.stream()
                .filter(h -> h.id().equalsIgnoreCase(id))
                .findFirst();
    }

    @Override
    public List<Horse> findByGroup(HorseGroup group) {
        return horses.stream()
                .filter(h -> h.group() == group)
                .toList();
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public Horse save(Horse horse) {
        deleteById(horse.id());
        horses.add(horse);
        return horse;
    }

    @Override
    public void deleteById(String id) {
        horses.removeIf(h -> h.id().equalsIgnoreCase(id));
    }

    @Override
    public void clear() {
        horses.clear();
    }
}
