package org.example.horse_racing.repository;

import org.example.horse_racing.domain.Horse;
import org.example.horse_racing.domain.HorseGroup;
import org.example.horse_racing.service.HorseJsonPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Horse repository that persists to JSON on every save and delete.
 */
public class JsonBackedHorseRepository implements HorseRepository {

    private final HorseJsonPersistence persistence;
    private final List<Horse> horses = new CopyOnWriteArrayList<>();

    public JsonBackedHorseRepository() {
        this.persistence = new HorseJsonPersistence();
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            horses.clear();
            horses.addAll(persistence.load());
        } catch (IOException e) {
            // Keep empty on load failure
        }
    }

    private void persist() {
        try {
            persistence.save(new ArrayList<>(horses));
        } catch (IOException ignored) {
            // Log in production; for now silently ignore
        }
    }

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
        horses.removeIf(h -> h.id().equalsIgnoreCase(horse.id()));
        horses.add(horse);
        persist();
        return horse;
    }

    @Override
    public void deleteById(String id) {
        horses.removeIf(h -> h.id().equalsIgnoreCase(id));
        persist();
    }

    @Override
    public void clear() {
        horses.clear();
        persist();
    }
}
