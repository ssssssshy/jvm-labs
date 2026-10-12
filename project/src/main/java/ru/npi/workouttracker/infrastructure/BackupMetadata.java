package ru.npi.workouttracker.infrastructure;

import java.nio.file.Path;
import java.time.Instant;

public record BackupMetadata(
        Path path,
        Instant verifiedAt,
        int schemaVersion,
        int workoutCount,
        String contentSha256
) {
}
