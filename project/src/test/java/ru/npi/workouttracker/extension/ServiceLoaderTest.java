package ru.npi.workouttracker.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

class ServiceLoaderTest {
    @Test
    void discoversTwoProvidersAndExecutesTheirContracts() {
        Map<String, WorkoutReportProvider> providers = ServiceLoader.load(WorkoutReportProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toUnmodifiableMap(
                        WorkoutReportProvider::code,
                        Function.identity()
                ));
        List<Workout> workouts = List.of(
                new Workout(1, "Бег", LocalDate.of(2026, 10, 1), WorkoutStatus.COMPLETED),
                new Workout(2, "Йога", LocalDate.of(2026, 10, 2), WorkoutStatus.PLANNED),
                new Workout(3, "Бег", LocalDate.of(2026, 10, 3), WorkoutStatus.PLANNED)
        );

        assertEquals(2, providers.size());
        assertEquals("Запланировано: 2, выполнено: 1",
                providers.get("status-summary").render(workouts));
        assertEquals("Бег: 2, Йога: 1",
                providers.get("type-summary").render(workouts));
    }
}
