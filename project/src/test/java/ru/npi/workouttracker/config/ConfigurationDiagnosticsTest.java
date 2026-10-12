package ru.npi.workouttracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.npi.workouttracker.diagnostics.DiagnosticLogger;

class ConfigurationDiagnosticsTest {
    private static final String SECRET = "token-super-secret-17";

    @TempDir
    Path temporaryDirectory;

    @Test
    void validExternalConfigurationResolvesRelativePaths() throws IOException {
        Path configFile = writeConfig("3", "INFO", "http://127.0.0.1:8080/directory");

        AppConfig config = AppConfigLoader.load(configFile);

        assertEquals(temporaryDirectory.resolve("data/workouts.db"), config.databasePath());
        assertEquals(URI.create("http://127.0.0.1:8080/directory"), config.serviceUri());
        assertEquals(Duration.ofSeconds(3), config.requestTimeout());
        assertEquals(LogLevel.INFO, config.logLevel());
        assertEquals(temporaryDirectory.resolve("logs"), config.logDirectory());
    }

    @Test
    void invalidTimeoutNamesParameterAndAllowedRange() throws IOException {
        Path configFile = writeConfig("0", "INFO", "http://127.0.0.1:8080/directory");

        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> AppConfigLoader.load(configFile));

        assertTrue(error.getMessage().contains("request.timeout.seconds"));
        assertTrue(error.getMessage().contains("от 1 до 30"));
    }

    @Test
    void invalidServiceUrlIsRejectedBeforeNetworkCall() throws IOException {
        Path configFile = writeConfig("3", "INFO", "not-a-url");

        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> AppConfigLoader.load(configFile));

        assertTrue(error.getMessage().contains("service.url"));
        assertTrue(error.getMessage().contains("HTTP(S)"));
    }

    @Test
    void rotatedLogHasContextButNeverWritesSecretOrExceptionMessage() throws IOException {
        Path configFile = writeConfig("3", "DEBUG", "http://127.0.0.1:8080/directory");
        AppConfig config = AppConfigLoader.load(configFile);
        UUID operation = UUID.fromString("00000000-0000-0000-0000-000000000017");

        try (DiagnosticLogger logger = DiagnosticLogger.open(config.logDirectory(), config.logLevel())) {
            for (int index = 0; index < 300; index++) {
                logger.event(Level.FINE, operation, "diagnostic_probe", "success");
            }
            logger.configurationLoaded(operation, config.databasePath(), config.serviceUri().getHost());
            logger.failure(operation, "directory_refresh", new IOException("Ответ содержал " + SECRET));
        }

        String journal = readAllLogs(config.logDirectory());
        assertTrue(journal.contains(DiagnosticLogger.APPLICATION_VERSION));
        assertTrue(journal.contains(operation.toString()));
        assertTrue(journal.contains("event=directory_refresh"));
        assertTrue(journal.contains("errorType=IOException"));
        assertFalse(journal.contains(SECRET));
        assertFalse(journal.contains("Ответ содержал"));
        try (Stream<Path> files = Files.list(config.logDirectory())) {
            assertTrue(files.count() >= 2, "Журнал должен ротироваться по размеру");
        }
    }

    private Path writeConfig(String timeout, String level, String serviceUrl) throws IOException {
        Path configFile = temporaryDirectory.resolve("application.properties");
        Files.writeString(configFile, """
                database.path=data/workouts.db
                service.url=%s
                request.timeout.seconds=%s
                log.level=%s
                log.directory=logs
                service.token=%s
                """.formatted(serviceUrl, timeout, level, SECRET), StandardCharsets.UTF_8);
        return configFile;
    }

    private static String readAllLogs(Path directory) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.sorted().toList()) {
                content.append(Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return content.toString();
    }
}
