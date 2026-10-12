package ru.npi.workouttracker.extension;

import java.util.List;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class StatusWorkoutReportProvider implements WorkoutReportProvider {
    public StatusWorkoutReportProvider() {
    }

    @Override
    public String code() {
        return "status-summary";
    }

    @Override
    public String title() {
        return "Сводка по состояниям";
    }

    @Override
    public String render(List<Workout> workouts) {
        long planned = workouts.stream()
                .filter(workout -> workout.status() == WorkoutStatus.PLANNED)
                .count();
        long completed = workouts.stream()
                .filter(workout -> workout.status() == WorkoutStatus.COMPLETED)
                .count();
        return "Запланировано: %d, выполнено: %d".formatted(planned, completed);
    }
}
