package ru.npi.workouttracker.extension;

import java.util.List;

import ru.npi.workouttracker.domain.Workout;

public interface WorkoutReportProvider {
    String code();

    String title();

    String render(List<Workout> workouts);
}
