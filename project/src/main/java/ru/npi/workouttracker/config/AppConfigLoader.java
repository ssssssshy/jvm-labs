package ru.npi.workouttracker.config;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

public final class AppConfigLoader {
    private AppConfigLoader() {
    }

    public static AppConfig load(Path file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException error) {
            throw new ConfigurationException("Не удалось прочитать файл настроек: " + file, error);
        }

        Path base = file.toAbsolutePath().normalize().getParent();
        Path database = resolve(base, required(properties, "database.path"), "database.path");
        Path logs = resolve(base, required(properties, "log.directory"), "log.directory");
        URI service = parseServiceUri(required(properties, "service.url"));
        Duration timeout = parseTimeout(required(properties, "request.timeout.seconds"));
        LogLevel level = parseLogLevel(required(properties, "log.level"));
        return new AppConfig(database, service, timeout, level, logs);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Параметр " + key + " обязателен");
        }
        return value.strip();
    }

    private static Path resolve(Path base, String value, String key) {
        try {
            Path path = Path.of(value);
            return (path.isAbsolute() ? path : base.resolve(path)).normalize();
        } catch (RuntimeException error) {
            throw new ConfigurationException("Параметр " + key + " содержит неверный путь", error);
        }
    }

    private static URI parseServiceUri(String value) {
        try {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new URISyntaxException(value, "требуется абсолютный HTTP(S) URL");
            }
            return uri;
        } catch (URISyntaxException error) {
            throw new ConfigurationException(
                    "Параметр service.url должен быть абсолютным HTTP(S) URL", error
            );
        }
    }

    private static Duration parseTimeout(String value) {
        try {
            int seconds = Integer.parseInt(value);
            if (seconds < 1 || seconds > 30) {
                throw new NumberFormatException("вне диапазона");
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException error) {
            throw new ConfigurationException(
                    "Параметр request.timeout.seconds должен быть целым числом от 1 до 30", error
            );
        }
    }

    private static LogLevel parseLogLevel(String value) {
        try {
            return LogLevel.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new ConfigurationException(
                    "Параметр log.level должен быть DEBUG, INFO, WARNING или ERROR", error
            );
        }
    }
}
