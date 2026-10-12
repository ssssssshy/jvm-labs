package ru.npi.workouttracker.application;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutRepository;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class WorkoutService implements WorkoutOperations {
    private final WorkoutRepository repository;

    public WorkoutService(WorkoutRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Репозиторий обязателен");
    }

    @Override
    public List<Workout> findAll() {
        return repository.findAll();
    }

    @Override
    public Workout create(String type, LocalDate date, WorkoutStatus status, int durationMinutes) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Введите тип тренировки, например «Кардио».");
        }
        if (type.strip().length() > Workout.MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException(
                    "Сократите тип тренировки до " + Workout.MAX_TYPE_LENGTH + " символов."
            );
        }
        if (date == null) {
            throw new IllegalArgumentException("Выберите дату тренировки.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Выберите состояние тренировки.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Введите длительность больше нуля.");
        }

        long nextId = repository.findAll().stream()
                .mapToLong(Workout::id)
                .max()
                .orElse(0L) + 1L;
        return repository.save(new Workout(nextId, type, date, status, durationMinutes));
    }

    @Override
    public Workout changeStatus(long id, WorkoutStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Выберите состояние тренировки.");
        }
        Workout current = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Тренировка с id=" + id + " не найдена"));
        return repository.replace(current.withStatus(status));
    }

    @Override
    public Workout remove(long id) {
        Workout current = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Тренировка с id=" + id + " не найдена"));
        repository.deleteById(id);
        return current;
    }
}
