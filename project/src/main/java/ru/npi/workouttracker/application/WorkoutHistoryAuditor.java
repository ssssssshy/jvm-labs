package ru.npi.workouttracker.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class WorkoutHistoryAuditor {
    private final long delayMillis;

    public WorkoutHistoryAuditor(long delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Задержка не может быть отрицательной");
        }
        this.delayMillis = delayMillis;
    }

    public WorkoutAuditResult audit(
            List<Workout> records,
            BooleanSupplier cancellationRequested,
            IntConsumer progress
    ) throws InterruptedException {
        List<Workout> snapshot = List.copyOf(records);
        Set<String> codes = new HashSet<>();
        int planned = 0;
        int completed = 0;
        for (int index = 0; index < snapshot.size(); index++) {
            if (cancellationRequested.getAsBoolean()) {
                return null;
            }
            Workout workout = snapshot.get(index);
            if (!codes.add(workout.code())) {
                throw new IllegalStateException("Повторяется код тренировки: " + workout.code());
            }
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
            if (cancellationRequested.getAsBoolean()) {
                return null;
            }
            if (workout.status() == WorkoutStatus.PLANNED) {
                planned++;
            }
            if (workout.status() == WorkoutStatus.COMPLETED) {
                completed++;
            }
            progress.accept(index + 1);
        }
        return new WorkoutAuditResult(snapshot.size(), planned, completed);
    }
}
