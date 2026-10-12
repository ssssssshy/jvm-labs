package ru.npi.workouttracker.lesson;

import java.time.LocalDate;
import java.util.Objects;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class DomainDemo {
    private DomainDemo() {
    }

    public static void main(String[] args) {
        Workout planned = new Workout(
                17,
                "  Силовая тренировка  ",
                LocalDate.of(2026, 9, 15),
                WorkoutStatus.PLANNED
        );
        Workout inProgress = planned.withStatus(WorkoutStatus.IN_PROGRESS);

        verify("пробелы по краям удалены", "Силовая тренировка", planned.type());
        verify("исходное состояние сохранено", WorkoutStatus.PLANNED, planned.status());
        verify("новое состояние установлено", WorkoutStatus.IN_PROGRESS, inProgress.status());
        verify("идентификатор сохранён", planned.id(), inProgress.id());
        verify("тип сохранён", planned.type(), inProgress.type());
        verify("дата сохранена", planned.date(), inProgress.date());
        if (planned == inProgress) {
            throw new AssertionError("withStatus должен возвращать новую запись");
        }
        System.out.println("[OK] withStatus вернул новую запись");

        expectFailure("отрицательный id", IllegalArgumentException.class,
                () -> new Workout(-1, "Кардио", LocalDate.of(2026, 9, 15), WorkoutStatus.PLANNED));
        expectFailure("пустой тип", IllegalArgumentException.class,
                () -> new Workout(1, "   ", LocalDate.of(2026, 9, 15), WorkoutStatus.PLANNED));
        expectFailure("отсутствующая дата", NullPointerException.class,
                () -> new Workout(1, "Кардио", null, WorkoutStatus.PLANNED));
        expectFailure("отсутствующее состояние", NullPointerException.class,
                () -> new Workout(1, "Кардио", LocalDate.of(2026, 9, 15), null));

        System.out.println("Исходная запись: " + planned);
        System.out.println("Передана в работу: " + inProgress);
        System.out.println("Все проверки P03 пройдены.");
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    private static void expectFailure(
            String scenario,
            Class<? extends RuntimeException> expectedType,
            Runnable action
    ) {
        try {
            action.run();
            throw new AssertionError(scenario + ": ожидалась ошибка " + expectedType.getSimpleName());
        } catch (RuntimeException exception) {
            if (!expectedType.isInstance(exception)) {
                throw new AssertionError(scenario + ": получена неожиданная ошибка", exception);
            }
            System.out.println("[OK] " + scenario + ": получена "
                    + exception.getClass().getSimpleName());
        }
    }
}
