package ru.npi.workouttracker.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;

class WorkoutServiceJUnitTest {
    @Test
    void invalidCreateDoesNotChangeRepository() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create("Кардио", null, WorkoutStatus.PLANNED));

        assertEquals("Выберите дату тренировки.", error.getMessage());
        assertEquals(0, service.findAll().size());
    }

    @Test
    void eachTestOwnsItsRepositoryFixture() {
        WorkoutService first = new WorkoutService(new MemoryWorkoutRepository());
        WorkoutService second = new WorkoutService(new MemoryWorkoutRepository());

        first.create("Йога", LocalDate.of(2026, 10, 4), WorkoutStatus.PLANNED);

        assertEquals(1, first.findAll().size());
        assertEquals(0, second.findAll().size());
    }
}
