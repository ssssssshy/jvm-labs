package ru.npi.workouttracker.extension;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ru.npi.workouttracker.domain.Workout;

public final class TypeWorkoutReportProvider implements WorkoutReportProvider {
    public TypeWorkoutReportProvider() {
    }

    @Override
    public String code() {
        return "type-summary";
    }

    @Override
    public String title() {
        return "Сводка по типам";
    }

    @Override
    public String render(List<Workout> workouts) {
        Map<String, Long> counts = workouts.stream().collect(Collectors.groupingBy(
                Workout::type,
                TreeMap::new,
                Collectors.counting()
        ));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
