package ru.npi.workouttracker.application;

import java.time.LocalDate;
import java.util.List;

import ru.npi.workouttracker.diagnostics.OperationTitle;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public interface WorkoutOperations {
    List<Workout> findAll();

    @OperationTitle("Добавить тренировку")
    Workout create(String type, LocalDate date, WorkoutStatus status, int durationMinutes);

    default Workout create(String type, LocalDate date, WorkoutStatus status) {
        return create(type, date, status, 1);
    }

    @OperationTitle("Изменить состояние тренировки")
    Workout changeStatus(long id, WorkoutStatus status);

    Workout remove(long id);

    default List<Workout> filterByType(String fragment) {
        String query = fragment == null ? "" : fragment.strip().toLowerCase();
        return findAll().stream()
                .filter(workout -> workout.type().toLowerCase().contains(query))
                .toList();
    }

    default int totalMinutes(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Укажите корректный период");
        }
        return findAll().stream()
                .filter(workout -> !workout.date().isBefore(from) && !workout.date().isAfter(to))
                .mapToInt(Workout::durationMinutes)
                .sum();
    }
}
