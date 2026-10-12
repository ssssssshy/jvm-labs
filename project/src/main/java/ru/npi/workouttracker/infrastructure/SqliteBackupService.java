package ru.npi.workouttracker.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

public final class SqliteBackupService {
    private final Path source;
    private final Clock clock;

    public SqliteBackupService(Path source) {
        this(source, Clock.systemUTC());
    }

    public SqliteBackupService(Path source, Clock clock) {
        this.source = normalize(source);
        this.clock = Objects.requireNonNull(clock, "Часы обязательны");
    }

    public BackupMetadata createVerifiedBackup(Path target) {
        Path backup = normalize(target);
        requireExistingDatabase(source, "Исходная база");
        requireDifferent(source, backup, "Резервная копия не может заменять рабочую базу");
        requireNewTarget(backup);
        createParent(backup);
        try {
            vacuumInto(source, backup);
            DatabaseSnapshot snapshot = inspect(backup);
            return snapshot.metadata(backup, clock);
        } catch (RuntimeException error) {
            deleteFailedTarget(backup, error);
            throw error;
        }
    }

    public BackupMetadata restoreToSeparateCopy(Path backupFile, Path target) {
        Path backup = normalize(backupFile);
        Path restored = normalize(target);
        requireExistingDatabase(backup, "Резервная копия");
        requireDifferent(source, restored, "Нельзя восстанавливать поверх рабочей базы");
        requireDifferent(backup, restored, "Цель восстановления должна быть отдельным файлом");
        requireNewTarget(restored);
        DatabaseSnapshot expected = inspect(backup);
        createParent(restored);
        try {
            vacuumInto(backup, restored);
            DatabaseSnapshot actual = inspect(restored);
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Восстановленные записи не совпадают с резервной копией");
            }
            return actual.metadata(restored, clock);
        } catch (RuntimeException error) {
            deleteFailedTarget(restored, error);
            throw error;
        }
    }

    public BackupMetadata verify(Path database) {
        Path checked = normalize(database);
        requireExistingDatabase(checked, "Файл проверки");
        return inspect(checked).metadata(checked, clock);
    }

    private static void vacuumInto(Path source, Path target) {
        try (Connection connection = open(source);
             PreparedStatement statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, target.toString());
            statement.execute();
        } catch (SQLException error) {
            throw new IllegalStateException("SQLite не смогла создать согласованный снимок", error);
        }
    }

    private static DatabaseSnapshot inspect(Path database) {
        try (Connection connection = open(database)) {
            verifyIntegrity(connection);
            int version = readSchemaVersion(connection);
            return readWorkouts(connection, version);
        } catch (SQLException error) {
            throw new IllegalStateException("База не прошла проверку чтением: " + database.getFileName(), error);
        }
    }

    private static void verifyIntegrity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
            if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                throw new SQLException("integrity_check не вернул ok");
            }
        }
    }

    private static int readSchemaVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT version FROM schema_version WHERE id = 1");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Не найдена версия схемы");
            }
            return result.getInt(1);
        }
    }

    private static DatabaseSnapshot readWorkouts(Connection connection, int version) throws SQLException {
        MessageDigest digest = sha256();
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, code, type, workout_date, status FROM workouts ORDER BY id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                count++;
                update(digest, rows.getLong("id"));
                update(digest, rows.getString("code"));
                update(digest, rows.getString("type"));
                update(digest, rows.getString("workout_date"));
                update(digest, rows.getString("status"));
            }
        }
        return new DatabaseSnapshot(version, count, HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 недоступен", error);
        }
    }

    private static void update(MessageDigest digest, Object value) {
        digest.update(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static Connection open(Path database) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database);
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "Путь обязателен").toAbsolutePath().normalize();
    }

    private static void requireExistingDatabase(Path database, String title) {
        if (!Files.isRegularFile(database)) {
            throw new IllegalArgumentException(title + " не найдена: " + database);
        }
    }

    private static void requireDifferent(Path first, Path second, String message) {
        if (first.equals(second)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNewTarget(Path target) {
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Целевой файл уже существует: " + target);
        }
    }

    private static void createParent(Path target) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось создать каталог: " + target.getParent(), error);
        }
    }

    private static void deleteFailedTarget(Path target, RuntimeException mainError) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException cleanupError) {
            mainError.addSuppressed(cleanupError);
        }
    }

    private record DatabaseSnapshot(int schemaVersion, int workoutCount, String contentSha256) {
        BackupMetadata metadata(Path path, Clock clock) {
            return new BackupMetadata(path, clock.instant(), schemaVersion, workoutCount, contentSha256);
        }
    }
}
