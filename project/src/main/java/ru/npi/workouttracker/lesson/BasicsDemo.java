package ru.npi.workouttracker.lesson;

import java.time.LocalDate;
import java.util.Objects;

public final class BasicsDemo {
    private static final LocalDate CONTROL_DATE = LocalDate.of(2026, 9, 9);

    private BasicsDemo() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--show-index-error")) {
            demonstrateWrongLoopBoundary();
            return;
        }

        runChecks();

        String[] workoutNames = {"Кардио", "Силовая", "Растяжка"};
        LocalDate[] plannedDates = {
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 9),
                LocalDate.of(2026, 9, 10)
        };

        System.out.println("Контрольная дата: " + CONTROL_DATE);
        printMissedWorkouts(workoutNames, plannedDates, CONTROL_DATE);
        System.out.println("Количество пропущенных: " + countMissed(plannedDates, CONTROL_DATE));
        System.out.printf("Доля пропущенных: %.3f%n", missedShare(plannedDates, CONTROL_DATE));
        System.out.println("Все проверки P02 пройдены.");
    }

    public static boolean isMissed(LocalDate plannedDate, LocalDate controlDate) {
        Objects.requireNonNull(plannedDate, "Плановая дата обязательна");
        Objects.requireNonNull(controlDate, "Контрольная дата обязательна");
        return plannedDate.isBefore(controlDate);
    }

    public static int countMissed(LocalDate[] plannedDates, LocalDate controlDate) {
        Objects.requireNonNull(plannedDates, "Массив дат обязателен");
        Objects.requireNonNull(controlDate, "Контрольная дата обязательна");

        int count = 0;
        for (LocalDate plannedDate : plannedDates) {
            if (isMissed(plannedDate, controlDate)) {
                count++;
            }
        }
        return count;
    }

    public static double missedShare(LocalDate[] plannedDates, LocalDate controlDate) {
        Objects.requireNonNull(plannedDates, "Массив дат обязателен");
        if (plannedDates.length == 0) {
            return 0.0;
        }
        return (double) countMissed(plannedDates, controlDate) / plannedDates.length;
    }

    public static void printMissedWorkouts(
            String[] workoutNames,
            LocalDate[] plannedDates,
            LocalDate controlDate
    ) {
        Objects.requireNonNull(workoutNames, "Массив названий обязателен");
        Objects.requireNonNull(plannedDates, "Массив дат обязателен");
        if (workoutNames.length != plannedDates.length) {
            throw new IllegalArgumentException("Количество названий и дат должно совпадать");
        }

        for (int i = 0; i < plannedDates.length; i++) {
            if (isMissed(plannedDates[i], controlDate)) {
                System.out.println("Пропущена тренировка: " + workoutNames[i]
                        + ", плановая дата: " + plannedDates[i]);
            }
        }
    }

    private static void runChecks() {
        verify("три даты вокруг границы", 1,
                countMissed(new LocalDate[]{
                        LocalDate.of(2026, 9, 8),
                        LocalDate.of(2026, 9, 9),
                        LocalDate.of(2026, 9, 10)
                }, CONTROL_DATE));

        verify("пустой массив", 0, countMissed(new LocalDate[0], CONTROL_DATE));
        verify("одна дата на границе", 0,
                countMissed(new LocalDate[]{CONTROL_DATE}, CONTROL_DATE));
        verify("одна дата до границы", 1,
                countMissed(new LocalDate[]{CONTROL_DATE.minusDays(1)}, CONTROL_DATE));
        verify("две одинаковые пропущенные даты", 2,
                countMissed(new LocalDate[]{
                        CONTROL_DATE.minusDays(1),
                        CONTROL_DATE.minusDays(1),
                        CONTROL_DATE.plusDays(1)
                }, CONTROL_DATE));
        verify("доля для пустого массива", 0.0, missedShare(new LocalDate[0], CONTROL_DATE));
        verify("доля для трёх дат с двумя пропусками", 2.0 / 3.0,
                missedShare(new LocalDate[]{
                        CONTROL_DATE.minusDays(1),
                        CONTROL_DATE.minusDays(1),
                        CONTROL_DATE.plusDays(1)
                }, CONTROL_DATE));
        expectFailure("отсутствующая дата", NullPointerException.class,
                () -> countMissed(new LocalDate[]{null}, CONTROL_DATE));
        expectFailure("разное количество названий и дат", IllegalArgumentException.class,
                () -> printMissedWorkouts(
                        new String[]{"Кардио"},
                        new LocalDate[]{CONTROL_DATE, CONTROL_DATE.plusDays(1)},
                        CONTROL_DATE));
    }

    private static void verify(String scenario, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": ожидалось " + expected + ", получено " + actual);
    }

    private static void verify(String scenario, double expected, double actual) {
        if (Double.compare(expected, actual) != 0) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": ожидалось " + expected + ", получено " + actual);
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
            System.out.println("[OK] " + scenario + ": получена " + exception.getClass().getSimpleName());
        }
    }

    private static void demonstrateWrongLoopBoundary() {
        LocalDate[] dates = {CONTROL_DATE.minusDays(1)};
        for (int i = 0; i <= dates.length; i++) {
            System.out.println(dates[i]);
        }
    }
}
