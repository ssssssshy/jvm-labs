package ru.npi.workouttracker.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;

class ReleaseAcceptanceTest {
    @Test
    void allFiveMiniSpecificationScenariosAreObservable() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());
        Workout run = service.create("Интервальный бег", LocalDate.of(2026, 10, 1),
                WorkoutStatus.PLANNED, 45);
        Workout yoga = service.create("Йога", LocalDate.of(2026, 10, 3),
                WorkoutStatus.COMPLETED, 30);

        assertEquals(2, service.findAll().size());
        assertEquals(WorkoutStatus.COMPLETED,
                service.changeStatus(run.id(), WorkoutStatus.COMPLETED).status());
        assertEquals(1, service.filterByType("бег").size());
        assertEquals(75, service.totalMinutes(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertEquals(yoga, service.remove(yoga.id()));
        assertEquals(1, service.findAll().size());
    }

    @Test
    void invalidDurationAndPeriodPreserveData() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());

        assertThrows(IllegalArgumentException.class, () -> service.create("Бег",
                LocalDate.of(2026, 10, 1), WorkoutStatus.PLANNED, 0));
        assertThrows(IllegalArgumentException.class, () -> service.totalMinutes(
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)));
        assertEquals(0, service.findAll().size());
    }
}
