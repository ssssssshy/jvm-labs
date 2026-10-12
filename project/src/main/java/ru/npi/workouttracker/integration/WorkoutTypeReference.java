package ru.npi.workouttracker.integration;

public record WorkoutTypeReference(String code, String name, int recommendedMinutes) {
    public WorkoutTypeReference {
        if (code == null || !code.matches("WT-[A-Z]{2,10}")) {
            throw new IllegalArgumentException("Неверный код типа тренировки: " + code);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название типа обязательно");
        }
        name = name.strip();
        if (recommendedMinutes <= 0) {
            throw new IllegalArgumentException("Рекомендуемая длительность должна быть больше нуля");
        }
    }
}
