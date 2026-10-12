package ru.npi.workouttracker.lesson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import ru.npi.workouttracker.application.WorkoutCsvImporter;
import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.infrastructure.SqliteWorkoutRepository;

public final class TransactionMigrationDemo {
    private TransactionMigrationDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path database = Path.of(System.getProperty("p12.database", "build/p12/workouts.db"));
        Path validCsv = Path.of(System.getProperty("p12.validCsv", "samples/p12-valid.csv"));
        Path duplicateCsv = Path.of(System.getProperty("p12.duplicateCsv", "samples/p12-duplicate.csv"));

        createVersionOneDatabase(database);
        SqliteWorkoutRepository repository = new SqliteWorkoutRepository(database);
        verify("V001 запись сохранена", 1, repository.findAll().size());
        verify("схема обновлена", 2, repository.schemaVersion());

        WorkoutCsvImporter importer = new WorkoutCsvImporter(repository);
        int beforeValid = repository.findAll().size();
        List<Workout> imported = importer.importFile(validCsv);
        verify("корректный пакет целиком добавлен", beforeValid + 2, repository.findAll().size());
        verify("все поля первой строки", new Workout(
                2, "TR-0101", "Бег", LocalDate.of(2026, 10, 15), imported.getFirst().status()
        ), imported.getFirst());

        int beforeDuplicate = repository.findAll().size();
        try {
            importer.importFile(duplicateCsv);
            throw new AssertionError("Дубликат должен отклонять весь пакет");
        } catch (DuplicateWorkoutException expected) {
            verify("ошибка указывает проблемный код", true,
                    expected.getMessage().contains("TR-0201"));
        }
        verify("дубликат откатил весь пакет", beforeDuplicate, repository.findAll().size());

        verifyInvalidDateBeforeTransaction(importer, repository, database.getParent());

        SqliteWorkoutRepository secondStart = new SqliteWorkoutRepository(database);
        verify("повтор миграции оставил версию 2", 2, secondStart.schemaVersion());
        verify("повтор миграции не изменил данные", beforeDuplicate, secondStart.findAll().size());
        System.out.println("Все проверки P12 пройдены.");
    }

    private static void createVersionOneDatabase(Path database) throws Exception {
        Path absolute = database.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Files.deleteIfExists(absolute);
        String schema;
        try (InputStream input = Objects.requireNonNull(
                TransactionMigrationDemo.class.getResourceAsStream("/db/migration/V001__initial_schema.sql")
        )) {
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + absolute);
             var create = connection.prepareStatement(schema)) {
            create.executeUpdate();
            try (var insert = connection.prepareStatement("""
                    INSERT INTO workouts(id, code, type, workout_date, status) VALUES(?, ?, ?, ?, ?)
                    """)) {
                insert.setLong(1, 1);
                insert.setString(2, "TR-0001");
                insert.setString(3, "Старая тренировка");
                insert.setString(4, "2026-10-14");
                insert.setString(5, "PLANNED");
                insert.executeUpdate();
            }
        }
    }

    private static void verifyInvalidDateBeforeTransaction(
            WorkoutCsvImporter importer,
            SqliteWorkoutRepository repository,
            Path directory
    ) throws Exception {
        Path invalid = directory.resolve("invalid-date.csv");
        Files.writeString(invalid, "code;type;date\nTR-0301;Кардио;18.10.2026\n", StandardCharsets.UTF_8);
        int before = repository.findAll().size();
        try {
            importer.importFile(invalid);
            throw new AssertionError("Неверная дата должна быть отклонена");
        } catch (IllegalArgumentException expected) {
            verify("неверная дата связана с кодом", true,
                    expected.getMessage().contains("TR-0301"));
        }
        verify("ошибка CSV не изменила базу", before, repository.findAll().size());
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
