package ru.npi.workouttracker.lesson;

import java.time.LocalDate;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.interop.WorkoutCaptions;

public final class KotlinInteropDemo {
    private KotlinInteropDemo() {
    }

    public static void main(String[] args) {
        Workout workout = new Workout(
                23,
                "Интервальный бег",
                LocalDate.of(2026, 10, 5),
                WorkoutStatus.COMPLETED
        );
        String caption = WorkoutCaptions.caption(workout);
        if (!"Интервальный бег — выполнена".equals(caption)) {
            throw new AssertionError("Неожиданная подпись: " + caption);
        }
        System.out.println("[OK] Java вызвала Kotlin: " + caption);
        System.out.println("[OK] Kotlin прочитал Java record и enum: "
                + workout.getClass().getName() + ", " + workout.status());
        System.out.println("Все проверки P23 пройдены.");
    }
}
