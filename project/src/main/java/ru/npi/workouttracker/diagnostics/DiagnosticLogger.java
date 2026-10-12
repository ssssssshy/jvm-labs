package ru.npi.workouttracker.diagnostics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import ru.npi.workouttracker.config.LogLevel;

public final class DiagnosticLogger implements AutoCloseable {
    public static final String APPLICATION_VERSION = "1.0.0-p19";
    private static final int FILE_LIMIT_BYTES = 8 * 1024;
    private static final int FILE_COUNT = 3;

    private final Logger logger;
    private final FileHandler handler;

    private DiagnosticLogger(Logger logger, FileHandler handler) {
        this.logger = logger;
        this.handler = handler;
    }

    public static DiagnosticLogger open(Path directory, LogLevel level) {
        try {
            Files.createDirectories(directory);
            Logger logger = Logger.getLogger("workout-tracker-" + UUID.randomUUID());
            logger.setUseParentHandlers(false);
            logger.setLevel(level.julLevel());
            FileHandler handler = new FileHandler(
                    directory.resolve("workout-%g.log").toString(),
                    FILE_LIMIT_BYTES,
                    FILE_COUNT,
                    true
            );
            handler.setLevel(level.julLevel());
            handler.setFormatter(new SafeFormatter());
            logger.addHandler(handler);
            return new DiagnosticLogger(logger, handler);
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось открыть диагностический журнал", error);
        }
    }

    public void event(Level level, UUID operationId, String event, String outcome) {
        Objects.requireNonNull(operationId, "Идентификатор операции обязателен");
        logger.log(level, () -> "version=" + APPLICATION_VERSION
                + " operation=" + operationId
                + " event=" + safeToken(event)
                + " outcome=" + safeToken(outcome));
    }

    public void configurationLoaded(UUID operationId, Path database, String serviceHost) {
        String databaseFile = database.getFileName() == null ? "unknown" : database.getFileName().toString();
        logger.log(Level.INFO, () -> "version=" + APPLICATION_VERSION
                + " operation=" + operationId
                + " event=configuration_loaded outcome=success"
                + " databaseFile=" + safeToken(databaseFile)
                + " serviceHost=" + safeToken(serviceHost));
    }

    public void failure(UUID operationId, String event, Throwable error) {
        String errorType = error == null ? "Unknown" : error.getClass().getSimpleName();
        logger.log(Level.WARNING, () -> "version=" + APPLICATION_VERSION
                + " operation=" + operationId
                + " event=" + safeToken(event)
                + " outcome=failure errorType=" + safeToken(errorType));
    }

    @Override
    public void close() {
        handler.flush();
        handler.close();
        logger.removeHandler(handler);
    }

    private static String safeToken(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
            return "redacted";
        }
        return value;
    }

    private static final class SafeFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return "%1$tFT%1$tTZ level=%2$s %3$s%n".formatted(
                    record.getMillis(), record.getLevel().getName(), record.getMessage()
            );
        }
    }
}
