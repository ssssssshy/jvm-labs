package ru.npi.workouttracker.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

class SqliteWorkoutRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void duplicateInBatchRollsBackEveryRow() {
        SqliteWorkoutRepository repository = new SqliteWorkoutRepository(
                temporaryDirectory.resolve("rollback.db")
        );
        Workout first = new Workout(1, "TR-0001", "Бег",
                LocalDate.of(2026, 10, 4), WorkoutStatus.PLANNED);
        Workout duplicateCode = new Workout(2, "TR-0001", "Йога",
                LocalDate.of(2026, 10, 5), WorkoutStatus.PLANNED);

        assertThrows(DuplicateWorkoutException.class,
                () -> repository.saveAll(List.of(first, duplicateCode)));

        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void newTemporaryDatabaseReceivesCurrentSchema() {
        SqliteWorkoutRepository repository = new SqliteWorkoutRepository(
                temporaryDirectory.resolve("schema.db")
        );

        assertEquals(3, repository.schemaVersion());
    }
}
