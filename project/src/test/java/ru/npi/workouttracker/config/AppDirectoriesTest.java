package ru.npi.workouttracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppDirectoriesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void environmentOverrideKeepsDataOutsideInstallation() {
        Path installation = temporaryDirectory.resolve("package/WorkoutTracker");
        Path userData = temporaryDirectory.resolve("clean-user");

        Path resolved = AppDirectories.resolve(
                Map.of(AppDirectories.ENV_HOME, userData.toString()),
                installation.toString()
        );

        assertEquals(userData.toAbsolutePath(), resolved);
        assertTrue(!resolved.startsWith(installation.toAbsolutePath()));
    }

    @Test
    void firstRunCreatesReusableExternalConfiguration() throws IOException {
        Path userData = temporaryDirectory.resolve("user-data");

        Path first = AppConfigBootstrap.ensureDefault(userData);
        String initial = Files.readString(first);
        Path second = AppConfigBootstrap.ensureDefault(userData);

        assertEquals(first, second);
        assertEquals(initial, Files.readString(second));
        AppConfig config = AppConfigLoader.load(second);
        assertEquals(userData.resolve("data/workouts.db"), config.databasePath());
        assertEquals(userData.resolve("logs"), config.logDirectory());
    }
}
