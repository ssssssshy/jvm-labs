package ru.npi.workouttracker.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Workout(
        long id,
        String code,
        String type,
        LocalDate date,
        WorkoutStatus status,
        int durationMinutes
) {
    public static final int MAX_TYPE_LENGTH = 60;

    public Workout {
        if (id < 0) {
            throw new IllegalArgumentException("Идентификатор не может быть отрицательным");
        }
        code = requireText(code, "Код тренировки");
        if (!code.matches("TR-\\d{4,}")) {
            throw new IllegalArgumentException("Код тренировки должен иметь вид TR-0001");
        }
        type = requireText(type, "Тип тренировки");
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException(
                    "Тип тренировки не может быть длиннее " + MAX_TYPE_LENGTH + " символов"
            );
        }
        Objects.requireNonNull(date, "Дата тренировки обязательна");
        Objects.requireNonNull(status, "Состояние тренировки обязательно");
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Длительность должна быть больше нуля");
        }
    }

    public Workout(long id, String code, String type, LocalDate date, WorkoutStatus status) {
        this(id, code, type, date, status, 1);
    }

    public Workout(long id, String type, LocalDate date, WorkoutStatus status) {
        this(id, codeForId(id), type, date, status, 1);
    }

    public Workout(long id, String type, LocalDate date, WorkoutStatus status, int durationMinutes) {
        this(id, codeForId(id), type, date, status, durationMinutes);
    }

    public Workout withStatus(WorkoutStatus newStatus) {
        return new Workout(id, code, type, date, newStatus, durationMinutes);
    }

    public static String codeForId(long id) {
        return "TR-%04d".formatted(id);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        return value.strip();
    }
}
