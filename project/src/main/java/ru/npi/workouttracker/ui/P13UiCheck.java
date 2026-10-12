package ru.npi.workouttracker.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.npi.workouttracker.application.WorkoutAuditTask;
import ru.npi.workouttracker.application.WorkoutHistoryAuditor;
import ru.npi.workouttracker.application.WorkoutService;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;

public final class P13UiCheck extends Application {
    private MainController controller;
    private Stage stage;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        try {
            stage = primaryStage;
            WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());
            FXMLLoader loader = new FXMLLoader(requireResource("main-view.fxml"));
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    return new MainController(service, new WorkoutHistoryAuditor(350));
                }
                throw new IllegalArgumentException("Неизвестный контроллер: " + type.getName());
            });
            Parent root = loader.load();
            controller = loader.getController();
            scene = new Scene(root, 1050, 700);
            scene.getStylesheets().add(requireResource("style.css").toExternalForm());
            stage.setTitle("Проверка P13 — фоновая задача");
            stage.setScene(scene);
            stage.show();

            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(event -> startAndCancel());
            pause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void startAndCancel() {
        try {
            controller.startAuditButton().fire();
            verify("повторный запуск запрещён", true, controller.startAuditButton().isDisabled());
            verify("отмена доступна", false, controller.cancelAuditButton().isDisabled());
            stage.setWidth(stage.getWidth() + 80);

            PauseTransition responsivePause = new PauseTransition(Duration.millis(80));
            responsivePause.setOnFinished(event -> {
                try {
                    verify("поток JavaFX обработал импульс во время Task", true,
                            controller.startAuditButton().isDisabled());
                    controller.cancelAuditButton().fire();
                    waitForCancellation();
                } catch (Throwable error) {
                    fail(error);
                }
            });
            responsivePause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void waitForCancellation() {
        PauseTransition pause = new PauseTransition(Duration.millis(180));
        pause.setOnFinished(event -> {
            try {
                verify("отмена отличается от ошибки", true,
                        controller.auditMessageLabel().getText().contains("отменена"));
                verify("неполный результат не опубликован", null, controller.lastAuditResult());
                verify("повтор после отмены доступен", false,
                        controller.startAuditButton().isDisabled());
                controller.startAuditButton().fire();
                waitForSuccess();
            } catch (Throwable error) {
                fail(error);
            }
        });
        pause.play();
    }

    private void waitForSuccess() {
        PauseTransition pause = new PauseTransition(Duration.millis(520));
        pause.setOnFinished(event -> {
            try {
                verify("успех отличается от отмены", true,
                        controller.auditMessageLabel().getText().contains("завершена"));
                verify("прогресс завершён", 1.0, controller.auditProgress().getProgress());
                verify("полный результат опубликован", 1,
                        controller.lastAuditResult().total());
                verify("кнопка восстановлена после успеха", false,
                        controller.startAuditButton().isDisabled());
                saveScreenshot();
                verifyFailureOutcome();
            } catch (Throwable error) {
                fail(error);
            }
        });
        pause.play();
    }

    private void verifyFailureOutcome() {
        List<Workout> corrupted = List.of(
                new Workout(10, "TR-0099", "Бег", LocalDate.of(2026, 10, 20), WorkoutStatus.PLANNED),
                new Workout(11, "TR-0099", "Йога", LocalDate.of(2026, 10, 21), WorkoutStatus.COMPLETED)
        );
        WorkoutAuditTask failingTask = new WorkoutAuditTask(new WorkoutHistoryAuditor(0), corrupted);
        failingTask.setOnFailed(event -> {
            try {
                verify("отказ указывает проблемный код", true,
                        failingTask.getException().getMessage().contains("TR-0099"));
                verify("при отказе результат не опубликован", null, failingTask.getValue());
                System.out.println("Все проверки P13 пройдены.");
                Platform.exit();
            } catch (Throwable error) {
                fail(error);
            }
        });
        Thread worker = new Thread(failingTask, "p13-failure-check");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveScreenshot() throws Exception {
        WritableImage image = scene.snapshot(null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        File output = new File(System.getProperty("p13.screenshot"));
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Не удалось создать каталог снимка: " + parent);
        }
        ImageIO.write(bufferedImage, "png", output);
        System.out.println("[OK] снимок сохранён: " + output.getPath());
    }

    private URL requireResource(String name) {
        return Objects.requireNonNull(getClass().getResource(name), "Не найден ресурс " + name);
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    private static void fail(Throwable error) {
        error.printStackTrace(System.err);
        Platform.exit();
        throw new RuntimeException(error);
    }
}
