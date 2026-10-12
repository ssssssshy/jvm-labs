package ru.npi.workouttracker.lesson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import ru.npi.workouttracker.application.WorkoutService;
import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.SqliteWorkoutRepository;

public final class SqlitePersistenceDemo {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 14);
    private static final String TYPE_WITH_APOSTROPHE = "Интервальная тренировка О'Нила";

    private SqlitePersistenceDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path database = Path.of(System.getProperty("p11.database", "build/p11/workouts.db"));
        String mode = System.getProperty("p11.mode", "first");
        if (mode.equals("first")) {
            firstRun(database);
        } else if (mode.equals("second")) {
            secondRun(database);
        } else {
            throw new IllegalArgumentException("Неизвестный режим: " + mode);
        }
    }

    private static void firstRun(Path database) throws Exception {
        Files.deleteIfExists(database);
        SqliteWorkoutRepository repository = new SqliteWorkoutRepository(database);
        WorkoutService service = new WorkoutService(repository);
        Workout created = service.create(TYPE_WITH_APOSTROPHE, DATE, WorkoutStatus.PLANNED);

        verify("вставка и чтение вернули все поля", created, repository.findById(created.id()).orElseThrow());
        verify("апостроф сохранён целиком", TYPE_WITH_APOSTROPHE, created.type());
        verify("код сформирован", "TR-0001", created.code());

        List<Workout> beforeDuplicate = repository.findAll();
        try {
            repository.save(new Workout(
                    2,
                    created.code(),
                    "Дубликат",
                    DATE.plusDays(1),
                    WorkoutStatus.PLANNED
            ));
            throw new AssertionError("дубликат кода должен быть отклонён");
        } catch (DuplicateWorkoutException expected) {
            verify("дубликат кода преобразован в понятный отказ", true,
                    expected.getMessage().contains(created.code()));
        }
        verify("отказ не изменил базу", beforeDuplicate, repository.findAll());
        System.out.println("Первый запуск завершён; файл SQLite оставлен для повторного запуска.");
    }

    private static void secondRun(Path database) {
        SqliteWorkoutRepository repository = new SqliteWorkoutRepository(database);
        List<Workout> stored = repository.findAll();
        verify("после перезапуска найдена одна запись", 1, stored.size());
        Workout restored = stored.getFirst();
        verify("текст с апострофом пережил перезапуск", TYPE_WITH_APOSTROPHE, restored.type());
        verify("дата восстановлена из SQLite TEXT", DATE, restored.date());
        verify("код и состояние восстановлены", "TR-0001/PLANNED", restored.code() + "/" + restored.status());
        System.out.println("Второй запуск подтвердил сохранность данных.");
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
