package ru.npi.workouttracker.domain;

public final class DuplicateWorkoutException extends IllegalArgumentException {
    public DuplicateWorkoutException(long id) {
        super("Тренировка с id=" + id + " уже существует");
    }

    public DuplicateWorkoutException(String message) {
        super(message);
    }
}
