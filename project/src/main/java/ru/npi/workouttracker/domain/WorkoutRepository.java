package ru.npi.workouttracker.domain;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository {
    List<Workout> findAll();

    Optional<Workout> findById(long id);

    Workout save(Workout workout);

    List<Workout> saveAll(List<Workout> workouts);

    Workout replace(Workout workout);

    boolean deleteById(long id);
}
