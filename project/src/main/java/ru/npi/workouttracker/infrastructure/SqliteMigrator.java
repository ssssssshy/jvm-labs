package ru.npi.workouttracker.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

final class SqliteMigrator {
    static final int LATEST_VERSION = 3;

    private SqliteMigrator() {
    }

    static void apply(Connection connection) throws SQLException, IOException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            createVersionTable(connection);
            int version = currentVersion(connection);
            for (int next = version + 1; next <= LATEST_VERSION; next++) {
                executeResource(connection, "/db/migration/V%03d__%s.sql".formatted(
                        next,
                        switch (next) {
                            case 1 -> "initial_schema";
                            case 2 -> "add_comment";
                            case 3 -> "add_duration";
                            default -> throw new IllegalStateException("Неизвестная миграция " + next);
                        }
                ));
                recordVersion(connection, next);
            }
            connection.commit();
        } catch (SQLException | IOException | RuntimeException error) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw error;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    static int currentVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT version FROM schema_version WHERE id = 1");
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getInt(1) : 0;
        }
    }

    private static void createVersionTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    version INTEGER NOT NULL CHECK (version >= 0)
                )
                """)) {
            statement.executeUpdate();
        }
    }

    private static void executeResource(Connection connection, String resource) throws IOException, SQLException {
        String sql;
        try (InputStream input = Objects.requireNonNull(
                SqliteMigrator.class.getResourceAsStream(resource),
                "Не найдена миграция " + resource
        )) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private static void recordVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO schema_version(id, version) VALUES(1, ?)
                ON CONFLICT(id) DO UPDATE SET version = excluded.version
                """)) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }
}
