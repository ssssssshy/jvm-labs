package ru.npi.workouttracker.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public record AppConfig(
        Path databasePath,
        URI serviceUri,
        Duration requestTimeout,
        LogLevel logLevel,
        Path logDirectory
) {
}
