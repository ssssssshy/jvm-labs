package ru.npi.workouttracker.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

class SqliteBackupServiceTest {
    private static final Instant VERIFIED_AT = Instant.parse("2026-10-04T18:00:00Z");

    @TempDir
    Path temporaryDirectory;
    private Path source;
    private SqliteWorkoutRepository sourceRepository;
    private SqliteBackupService backups;

    @BeforeEach
    void prepareSourceDatabase() {
        source = temporaryDirectory.resolve("working/workouts.db");
        sourceRepository = new SqliteWorkoutRepository(source);
        sourceRepository.save(new Workout(1, "Бег", LocalDate.of(2026, 10, 1), WorkoutStatus.COMPLETED));
        sourceRepository.save(new Workout(2, "Йога", LocalDate.of(2026, 10, 2), WorkoutStatus.PLANNED));
        backups = new SqliteBackupService(
                source,
                Clock.fixed(VERIFIED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void backupIsReturnedOnlyAfterIntegrityAndReadCheck() {
        Path target = temporaryDirectory.resolve("backup/workouts-backup.db");

        BackupMetadata metadata = backups.createVerifiedBackup(target);

        assertTrue(Files.isRegularFile(target));
        assertEquals(target.toAbsolutePath(), metadata.path());
        assertEquals(VERIFIED_AT, metadata.verifiedAt());
        assertEquals(3, metadata.schemaVersion());
        assertEquals(2, metadata.workoutCount());
        assertEquals(64, metadata.contentSha256().length());
    }

    @Test
    void restoreUsesSeparateCopyAndMatchesBackupRecords() {
        Path backup = temporaryDirectory.resolve("backup/snapshot.db");
        BackupMetadata original = backups.createVerifiedBackup(backup);
        sourceRepository.save(new Workout(3, "Плавание",
                LocalDate.of(2026, 10, 3), WorkoutStatus.PLANNED));
        Path restoredPath = temporaryDirectory.resolve("restore/check.db");

        BackupMetadata restored = backups.restoreToSeparateCopy(backup, restoredPath);
        SqliteWorkoutRepository restoredRepository = new SqliteWorkoutRepository(restoredPath);

        assertEquals(original.schemaVersion(), restored.schemaVersion());
        assertEquals(original.workoutCount(), restored.workoutCount());
        assertEquals(original.contentSha256(), restored.contentSha256());
        assertEquals(2, restoredRepository.findAll().size());
        assertEquals(3, sourceRepository.findAll().size());
        assertFalse(source.equals(restoredPath.toAbsolutePath()));
    }

    @Test
    void corruptBackupIsRejectedBeforeRestoreTargetAppears() throws IOException {
        Path corrupt = temporaryDirectory.resolve("backup/corrupt.db");
        Files.createDirectories(corrupt.getParent());
        Files.writeString(corrupt, "not a sqlite database", StandardCharsets.UTF_8);
        Path target = temporaryDirectory.resolve("restore/from-corrupt.db");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> backups.restoreToSeparateCopy(corrupt, target));

        assertTrue(error.getMessage().contains("не прошла проверку"));
        assertFalse(Files.exists(target));
    }

    @Test
    void restoreCannotOverwriteWorkingDatabase() {
        Path backup = temporaryDirectory.resolve("backup/snapshot.db");
        backups.createVerifiedBackup(backup);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> backups.restoreToSeparateCopy(backup, source));

        assertTrue(error.getMessage().contains("рабочей базы"));
        assertEquals(2, sourceRepository.findAll().size());
    }
}
