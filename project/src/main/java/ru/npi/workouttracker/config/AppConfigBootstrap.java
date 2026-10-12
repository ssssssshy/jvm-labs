package ru.npi.workouttracker.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppConfigBootstrap {
    private AppConfigBootstrap() {
    }

    public static Path ensureDefault(Path userDataRoot) {
        Path config = userDataRoot.resolve("config/application.properties");
        if (Files.exists(config)) {
            return config;
        }
        try {
            Files.createDirectories(config.getParent());
            Files.writeString(config, """
                    # Пользовательские настройки трекера тренировок.
                    database.path=../data/workouts.db
                    service.url=http://127.0.0.1:8080/directory
                    request.timeout.seconds=3
                    log.level=INFO
                    log.directory=../logs
                    """, StandardCharsets.UTF_8);
            return config;
        } catch (IOException error) {
            throw new ConfigurationException("Не удалось создать настройки: " + config, error);
        }
    }
}
