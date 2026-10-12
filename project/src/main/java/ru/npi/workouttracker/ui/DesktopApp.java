package ru.npi.workouttracker.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.npi.workouttracker.application.WorkoutService;
import ru.npi.workouttracker.config.AppConfig;
import ru.npi.workouttracker.config.AppConfigBootstrap;
import ru.npi.workouttracker.config.AppConfigLoader;
import ru.npi.workouttracker.config.AppDirectories;
import ru.npi.workouttracker.diagnostics.DiagnosticLogger;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;
import ru.npi.workouttracker.infrastructure.SqliteWorkoutRepository;

public final class DesktopApp extends Application {
    private DiagnosticLogger diagnostics;

    @Override
    public void start(Stage primaryWindow) throws IOException {
        String explicitConfig = System.getProperty("workout.config");
        Path configFile = explicitConfig == null || explicitConfig.isBlank()
                ? AppConfigBootstrap.ensureDefault(AppDirectories.userDataRoot())
                : Path.of(explicitConfig);
        AppConfig config = AppConfigLoader.load(configFile);
        diagnostics = DiagnosticLogger.open(config.logDirectory(), config.logLevel());
        UUID startupOperation = UUID.randomUUID();
        diagnostics.configurationLoaded(
                startupOperation,
                config.databasePath(),
                config.serviceUri().getHost()
        );
        URL fxml = Objects.requireNonNull(
                getClass().getResource("/ru/npi/workouttracker/ui/main-view.fxml"),
                "Не найден ресурс main-view.fxml"
        );
        URL css = Objects.requireNonNull(
                getClass().getResource("/ru/npi/workouttracker/ui/style.css"),
                "Не найден ресурс style.css"
        );

        FXMLLoader loader = createLoader(
                fxml,
                new WorkoutService(new SqliteWorkoutRepository(config.databasePath()))
        );
        Parent root = loader.load();
        MainController controller = loader.getController();
        Scene scene = new Scene(root, 920, 580);
        scene.getStylesheets().add(css.toExternalForm());

        primaryWindow.setTitle("Трекер тренировок");
        primaryWindow.setMinWidth(760);
        primaryWindow.setMinHeight(500);
        primaryWindow.setScene(scene);
        primaryWindow.show();
        diagnostics.event(java.util.logging.Level.INFO, startupOperation,
                "application_start", "success");
        if (getParameters().getRaw().contains("--smoke-test")) {
            System.out.println("SMOKE_OK config=" + configFile.toAbsolutePath().normalize()
                    + " database=" + config.databasePath()
                    + " records=" + controller.workoutTable().getItems().size()
                    + " fxml=" + fxml.getProtocol());
            Platform.runLater(primaryWindow::close);
        }
    }

    @Override
    public void stop() {
        if (diagnostics != null) {
            diagnostics.event(java.util.logging.Level.INFO, UUID.randomUUID(),
                    "application_stop", "success");
            diagnostics.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    static FXMLLoader createLoader(URL fxml) {
        return createLoader(
                fxml,
                new WorkoutService(new SqliteWorkoutRepository(Path.of("data", "workouts.db")))
        );
    }

    static FXMLLoader createInMemoryLoader(URL fxml) {
        return createLoader(fxml, new WorkoutService(new MemoryWorkoutRepository()));
    }

    static FXMLLoader createLoader(URL fxml, ru.npi.workouttracker.application.WorkoutOperations service) {
        FXMLLoader loader = new FXMLLoader(fxml);
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(service);
            }
            throw new IllegalArgumentException("Неизвестный тип контроллера: " + type.getName());
        });
        return loader;
    }
}
