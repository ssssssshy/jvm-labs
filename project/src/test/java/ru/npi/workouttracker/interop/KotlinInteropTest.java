package ru.npi.workouttracker.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

class KotlinInteropTest {
    @Test
    void javaCallsKotlinFunctionWithStableSignature() throws Exception {
        Workout workout = new Workout(
                23,
                "Йога",
                LocalDate.of(2026, 10, 5),
                WorkoutStatus.PLANNED
        );

        String caption = WorkoutCaptions.caption(workout);
        Method method = WorkoutCaptions.class.getMethod("caption", Workout.class);

        assertEquals("Йога — запланирована", caption);
        assertEquals(String.class, method.getReturnType());
        assertEquals(true, Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void kotlinNonNullContractRejectsJavaNull() {
        assertThrows(NullPointerException.class, () -> WorkoutCaptions.caption(null));
    }
}
