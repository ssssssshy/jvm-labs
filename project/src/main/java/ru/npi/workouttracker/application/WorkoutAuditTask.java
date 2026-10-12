package ru.npi.workouttracker.application;

import java.util.List;
import java.util.Objects;

import javafx.concurrent.Task;
import ru.npi.workouttracker.domain.Workout;

public final class WorkoutAuditTask extends Task<WorkoutAuditResult> {
    private final WorkoutHistoryAuditor auditor;
    private final List<Workout> snapshot;

    public WorkoutAuditTask(WorkoutHistoryAuditor auditor, List<Workout> records) {
        this.auditor = Objects.requireNonNull(auditor, "Аудитор обязателен");
        this.snapshot = List.copyOf(records);
    }

    @Override
    protected WorkoutAuditResult call() throws Exception {
        int total = snapshot.size();
        updateProgress(0, total);
        updateMessage("Проверено тренировок: 0 из " + total);
        return auditor.audit(snapshot, this::isCancelled, completed -> {
            updateProgress(completed, total);
            updateMessage("Проверено тренировок: " + completed + " из " + total);
        });
    }
}
