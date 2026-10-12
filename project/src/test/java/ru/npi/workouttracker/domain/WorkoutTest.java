package ru.npi.workouttracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkoutTest {
    @Test
    void rejectsBlankType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Workout(1, "  ", LocalDate.of(2026, 10, 4), WorkoutStatus.PLANNED));

        assertEquals("Тип тренировки не может быть пустым", error.getMessage());
    }

    @Test
    void statusChangeKeepsIdentityAndOtherFields() {
        Workout planned = new Workout(17, "Бег", LocalDate.of(2026, 10, 4), WorkoutStatus.PLANNED);

        Workout completed = planned.withStatus(WorkoutStatus.COMPLETED);

        assertEquals(planned.id(), completed.id());
        assertEquals(planned.code(), completed.code());
        assertEquals(planned.type(), completed.type());
        assertEquals(planned.date(), completed.date());
        assertEquals(WorkoutStatus.COMPLETED, completed.status());
    }
}
