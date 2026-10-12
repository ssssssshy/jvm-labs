package ru.npi.workouttracker.integration;

import java.time.Instant;
import java.util.List;

public record DirectorySnapshot(
        List<WorkoutTypeReference> entries,
        DirectorySource source,
        Instant receivedAt
) {
    public DirectorySnapshot {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Справочник не должен быть пустым");
        }
    }

    public String originLabel() {
        return source.title() + ", получено " + receivedAt;
    }
}
