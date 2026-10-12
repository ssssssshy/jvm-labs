package ru.npi.workouttracker.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutRepository;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class SqliteWorkoutRepository implements WorkoutRepository {
    private static final String SELECT_FIELDS = "id, code, type, workout_date, status, duration_minutes";
    private final Path database;

    public SqliteWorkoutRepository(Path database) {
        this.database = Objects.requireNonNull(database, "Путь к базе обязателен").toAbsolutePath().normalize();
        try {
            Path parent = this.database.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection connection = open()) {
                SqliteMigrator.apply(connection);
            }
        } catch (IOException | SQLException error) {
            throw new IllegalStateException("Не удалось подготовить SQLite", error);
        }
    }

    @Override
    public List<Workout> findAll() {
        String sql = "SELECT " + SELECT_FIELDS + " FROM workouts ORDER BY id";
        List<Workout> result = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(map(rows));
            }
            return List.copyOf(result);
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось прочитать тренировки", error);
        }
    }

    @Override
    public Optional<Workout> findById(long id) {
        String sql = "SELECT " + SELECT_FIELDS + " FROM workouts WHERE id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось найти тренировку", error);
        }
    }

    @Override
    public Workout save(Workout workout) {
        Objects.requireNonNull(workout, "Тренировка обязательна");
        String sql = "INSERT INTO workouts(id, code, type, workout_date, status, duration_minutes) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, workout);
            statement.executeUpdate();
            return workout;
        } catch (SQLException error) {
            throw translateWriteError(workout, error);
        }
    }

    @Override
    public List<Workout> saveAll(List<Workout> workouts) {
        Objects.requireNonNull(workouts, "Пакет тренировок обязателен");
        List<Workout> batch = List.copyOf(workouts);
        batch.forEach(workout -> Objects.requireNonNull(
                workout,
                "Тренировка в пакете обязательна"
        ));
        if (batch.isEmpty()) {
            return batch;
        }

        String sql = "INSERT INTO workouts(id, code, type, workout_date, status, duration_minutes) VALUES(?, ?, ?, ?, ?, ?)";
        Workout current = batch.getFirst();
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Workout workout : batch) {
                    current = workout;
                    bind(statement, workout);
                    statement.executeUpdate();
                }
                connection.commit();
                return batch;
            } catch (SQLException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw translateWriteError(current, error);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось выполнить пакетное сохранение", error);
        }
    }

    @Override
    public Workout replace(Workout workout) {
        Objects.requireNonNull(workout, "Тренировка обязательна");
        String sql = "UPDATE workouts SET code=?, type=?, workout_date=?, status=?, duration_minutes=? WHERE id=?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workout.code());
            statement.setString(2, workout.type());
            statement.setString(3, workout.date().toString());
            statement.setString(4, workout.status().name());
            statement.setInt(5, workout.durationMinutes());
            statement.setLong(6, workout.id());
            if (statement.executeUpdate() != 1) {
                throw new java.util.NoSuchElementException(
                        "Тренировка с id=" + workout.id() + " не найдена"
                );
            }
            return workout;
        } catch (SQLException error) {
            throw translateWriteError(workout, error);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM workouts WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось удалить тренировку", error);
        }
    }

    public int schemaVersion() {
        try (Connection connection = open()) {
            return SqliteMigrator.currentVersion(connection);
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось прочитать версию схемы", error);
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database);
    }

    private static void bind(PreparedStatement statement, Workout workout) throws SQLException {
        statement.setLong(1, workout.id());
        statement.setString(2, workout.code());
        statement.setString(3, workout.type());
        statement.setString(4, workout.date().toString());
        statement.setString(5, workout.status().name());
        statement.setInt(6, workout.durationMinutes());
    }

    private static Workout map(ResultSet rows) throws SQLException {
        return new Workout(
                rows.getLong("id"),
                rows.getString("code"),
                rows.getString("type"),
                java.time.LocalDate.parse(rows.getString("workout_date")),
                WorkoutStatus.valueOf(rows.getString("status")),
                rows.getInt("duration_minutes")
        );
    }

    private static RuntimeException translateWriteError(Workout workout, SQLException error) {
        if (error.getErrorCode() == 19
                || String.valueOf(error.getMessage()).contains("UNIQUE constraint failed")) {
            return new DuplicateWorkoutException(
                    "Тренировка с id=" + workout.id() + " или кодом " + workout.code() + " уже существует"
            );
        }
        return new IllegalStateException("Не удалось сохранить тренировку", error);
    }
}
