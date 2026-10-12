package ru.npi.workouttracker.infrastructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutRepository;

public final class MemoryWorkoutRepository implements WorkoutRepository {
    private final Map<Long, Workout> workoutsById = new LinkedHashMap<>();

    @Override
    public List<Workout> findAll() {
        return List.copyOf(new ArrayList<>(workoutsById.values()));
    }

    @Override
    public Optional<Workout> findById(long id) {
        return Optional.ofNullable(workoutsById.get(id));
    }

    @Override
    public Workout save(Workout workout) {
        Objects.requireNonNull(workout, "Тренировка обязательна");
        ensureUnique(workoutsById, workout);
        workoutsById.put(workout.id(), workout);
        return workout;
    }

    @Override
    public List<Workout> saveAll(List<Workout> workouts) {
        Objects.requireNonNull(workouts, "Пакет тренировок обязателен");
        Map<Long, Workout> candidate = new LinkedHashMap<>(workoutsById);
        for (Workout workout : List.copyOf(workouts)) {
            Objects.requireNonNull(workout, "Тренировка в пакете обязательна");
            ensureUnique(candidate, workout);
            candidate.put(workout.id(), workout);
        }
        workoutsById.clear();
        workoutsById.putAll(candidate);
        return List.copyOf(workouts);
    }

    @Override
    public Workout replace(Workout workout) {
        Objects.requireNonNull(workout, "Тренировка обязательна");
        if (!workoutsById.containsKey(workout.id())) {
            throw new NoSuchElementException("Тренировка с id=" + workout.id() + " не найдена");
        }
        workoutsById.put(workout.id(), workout);
        return workout;
    }

    @Override
    public boolean deleteById(long id) {
        return workoutsById.remove(id) != null;
    }

    private static void ensureUnique(Map<Long, Workout> existing, Workout workout) {
        if (existing.containsKey(workout.id())) {
            throw new DuplicateWorkoutException(workout.id());
        }
        if (existing.values().stream().anyMatch(saved -> saved.code().equals(workout.code()))) {
            throw new DuplicateWorkoutException("Тренировка с кодом " + workout.code() + " уже существует");
        }
    }
}
