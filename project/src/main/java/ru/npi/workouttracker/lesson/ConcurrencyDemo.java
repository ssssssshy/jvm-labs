package ru.npi.workouttracker.lesson;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class ConcurrencyDemo {
    private ConcurrencyDemo() {
    }

    public static void main(String[] args) throws Exception {
        demonstrateRaceAndFix();
        readDiagnosticsThroughFutures();
        cancelWaitingTask();
        System.out.println("Все проверки P14 пройдены.");
    }

    private static void demonstrateRaceAndFix() throws Exception {
        int[] unsafeCompleted = {0};
        AtomicInteger safeCompleted = new AtomicInteger();
        CyclicBarrier bothRead = new CyclicBarrier(2);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var increment = (java.util.concurrent.Callable<Void>) () -> {
                int oldValue = unsafeCompleted[0];
                bothRead.await(5, TimeUnit.SECONDS);
                unsafeCompleted[0] = oldValue + 1;
                safeCompleted.incrementAndGet();
                return null;
            };
            Future<Void> first = executor.submit(increment);
            Future<Void> second = executor.submit(increment);
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        verify("принудительная гонка потеряла обновление", 1, unsafeCompleted[0]);
        verify("AtomicInteger сохранил оба обновления", 2, safeCompleted.get());
    }

    private static void readDiagnosticsThroughFutures() throws Exception {
        List<Workout> snapshot = List.of(
                new Workout(1, "TR-0001", "Бег", LocalDate.of(2026, 10, 20), WorkoutStatus.PLANNED),
                new Workout(2, "TR-0002", "Йога", LocalDate.of(2026, 10, 21), WorkoutStatus.COMPLETED),
                new Workout(3, "TR-0003", "Кардио", LocalDate.of(2026, 10, 22), WorkoutStatus.COMPLETED)
        );
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        try (executor) {
            Future<Long> planned = executor.submit(() -> snapshot.stream()
                    .filter(item -> item.status() == WorkoutStatus.PLANNED).count());
            Future<Long> completed = executor.submit(() -> snapshot.stream()
                    .filter(item -> item.status() == WorkoutStatus.COMPLETED).count());
            Future<String> failing = executor.submit(() -> {
                throw new IllegalStateException("Повреждённая диагностика TR-0003");
            });

            verify("Future вернул запланированные", 1L, planned.get(5, TimeUnit.SECONDS));
            verify("Future вернул завершённые", 2L, completed.get(5, TimeUnit.SECONDS));
            try {
                failing.get(5, TimeUnit.SECONDS);
                throw new AssertionError("Исключение задачи потеряно");
            } catch (ExecutionException expected) {
                verify("исключение Future не потеряно", true,
                        expected.getCause().getMessage().contains("TR-0003"));
            }
        }
        verify("исполнитель дождался всех Future и закрылся", true, executor.isTerminated());
    }

    private static void cancelWaitingTask() throws Exception {
        CountDownLatch neverOpens = new CountDownLatch(1);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> waiting = executor.submit(() -> {
            neverOpens.await();
            return "не должно появиться";
        });
        verify("ожидающая задача приняла отмену", true, waiting.cancel(true));
        try {
            waiting.get();
            throw new AssertionError("Отмена не отразилась в Future");
        } catch (CancellationException expected) {
            verify("Future отличает отмену", true, waiting.isCancelled());
        } finally {
            executor.close();
        }
        verify("исполнитель закрыт", true, executor.isTerminated());
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
