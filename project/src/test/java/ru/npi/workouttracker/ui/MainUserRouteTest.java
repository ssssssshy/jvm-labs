package ru.npi.workouttracker.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.npi.workouttracker.application.WorkoutService;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.SqliteWorkoutRepository;

class MainUserRouteTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        if (!Platform.isFxApplicationThread()) {
            FutureTask<Void> started = new FutureTask<>(() -> null);
            try {
                Platform.startup(started);
            } catch (IllegalStateException alreadyStarted) {
                Platform.runLater(started);
            }
            started.get();
        }
    }

    @Test
    void userAddsWorkoutThroughFxmlFormWithoutSleep() throws Exception {
        RouteResult result = onFxThread(() -> {
            URL fxml = Objects.requireNonNull(
                    getClass().getResource("/ru/npi/workouttracker/ui/main-view.fxml")
            );
            FXMLLoader loader = DesktopApp.createInMemoryLoader(fxml);
            loader.load();
            MainController controller = loader.getController();
            int before = controller.workoutTable().getItems().size();

            controller.typeField().setText("Плавание");
            controller.dateField().setValue(LocalDate.of(2026, 10, 4));
            controller.statusField().setValue(WorkoutStatus.COMPLETED);
            controller.addButton().fire();

            return new RouteResult(
                    before,
                    controller.workoutTable().getItems().size(),
                    controller.messageLabel().getText()
            );
        });

        assertEquals(result.before() + 1, result.after());
        assertTrue(result.message().contains("Плавание"));
    }

    @Test
    void fxmlAndCssResourcesLoadFromClasspath() throws Exception {
        ResourceResult result = onFxThread(() -> {
            URL fxml = getClass().getResource("/ru/npi/workouttracker/ui/main-view.fxml");
            URL css = getClass().getResource("/ru/npi/workouttracker/ui/style.css");
            assertNotNull(fxml);
            assertNotNull(css);
            FXMLLoader loader = DesktopApp.createInMemoryLoader(fxml);
            return new ResourceResult(loader.load() != null, css.toExternalForm());
        });

        assertTrue(result.fxmlLoaded());
        assertTrue(result.cssUrl().endsWith("style.css"));
    }

    @Test
    void userInputTravelsFromFxmlToTemporarySqlite() throws Exception {
        Path database = temporaryDirectory.resolve("user-route.db");
        RouteResult result = onFxThread(() -> {
            URL fxml = Objects.requireNonNull(
                    getClass().getResource("/ru/npi/workouttracker/ui/main-view.fxml")
            );
            SqliteWorkoutRepository repository = new SqliteWorkoutRepository(database);
            FXMLLoader loader = DesktopApp.createLoader(fxml, new WorkoutService(repository));
            loader.load();
            MainController controller = loader.getController();
            int before = controller.workoutTable().getItems().size();

            controller.typeField().setText("Интервальный бег");
            controller.dateField().setValue(LocalDate.of(2026, 10, 6));
            controller.statusField().setValue(WorkoutStatus.COMPLETED);
            controller.addButton().fire();

            return new RouteResult(
                    before,
                    controller.workoutTable().getItems().size(),
                    controller.messageLabel().getText()
            );
        });

        SqliteWorkoutRepository reopened = new SqliteWorkoutRepository(database);
        assertEquals(result.before() + 1, result.after());
        assertTrue(result.message().contains("Интервальный бег"));
        assertEquals(1L, reopened.findAll().stream()
                .filter(workout -> workout.type().equals("Интервальный бег"))
                .count());
    }

    @Test
    void userFiltersHistoryAndCalculatesDurationForPeriod() throws Exception {
        FilterResult result = onFxThread(() -> {
            URL fxml = Objects.requireNonNull(
                    getClass().getResource("/ru/npi/workouttracker/ui/main-view.fxml")
            );
            FXMLLoader loader = DesktopApp.createInMemoryLoader(fxml);
            loader.load();
            MainController controller = loader.getController();

            controller.typeField().setText("Интервальный бег");
            controller.dateField().setValue(LocalDate.of(2026, 10, 4));
            controller.durationField().setText("45");
            controller.statusField().setValue(WorkoutStatus.COMPLETED);
            controller.addButton().fire();

            controller.filterField().setText("бег");
            int filtered = controller.workoutTable().getItems().size();
            controller.periodFromField().setValue(LocalDate.of(2026, 10, 1));
            controller.periodToField().setValue(LocalDate.of(2026, 10, 31));
            controller.totalButton().fire();
            return new FilterResult(filtered, controller.totalLabel().getText());
        });

        assertEquals(1, result.filtered());
        assertEquals("Суммарная длительность: 45 мин.", result.total());
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get();
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw error;
        }
    }

    private record RouteResult(int before, int after, String message) {
    }

    private record ResourceResult(boolean fxmlLoaded, String cssUrl) {
    }

    private record FilterResult(int filtered, String total) {
    }
}
