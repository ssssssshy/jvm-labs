package ru.npi.workouttracker.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class AppDirectories {
    public static final String ENV_HOME = "WORKOUT_HOME";

    private AppDirectories() {
    }

    public static Path userDataRoot() {
        return resolve(System.getenv(), System.getProperty("user.home"));
    }

    public static Path resolve(Map<String, String> environment, String userHome) {
        Objects.requireNonNull(environment, "Переменные среды обязательны");
        String override = environment.get(ENV_HOME);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        if (userHome == null || userHome.isBlank()) {
            throw new ConfigurationException("Не определён пользовательский каталог");
        }
        return Path.of(userHome, ".workout-tracker").toAbsolutePath().normalize();
    }
}
