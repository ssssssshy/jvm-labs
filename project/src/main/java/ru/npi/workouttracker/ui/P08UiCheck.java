package ru.npi.workouttracker.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
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
import javafx.scene.input.KeyCode;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class P08UiCheck extends Application {
    private MainController controller;
    private Scene scene;
    private int sizeBeforeKeyboardScenario;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = DesktopApp.createInMemoryLoader(requireResource("main-view.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            scene = new Scene(root, 980, 650);
            scene.getStylesheets().add(requireResource("style.css").toExternalForm());

            stage.setTitle("Проверка P08 — валидация и доступность");
            stage.setScene(scene);
            stage.show();

            PauseTransition initialFocusPause = new PauseTransition(Duration.millis(350));
            initialFocusPause.setOnFinished(event -> runInputMatrix());
            initialFocusPause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void runInputMatrix() {
        try {
            verify("начальный фокус", controller.typeField(), scene.getFocusOwner());
            verify("кнопка Enter по умолчанию", true, controller.addButton().isDefaultButton());
            verify("доступное имя поля", true,
                    controller.typeField().getAccessibleText().contains("Тип тренировки"));

            int originalSize = controller.workoutTable().getItems().size();
            controller.typeField().clear();
            controller.addButton().fire();
            verify("пустая строка не сохранена", originalSize,
                    controller.workoutTable().getItems().size());
            verify("текстовое сообщение об ошибке", true,
                    controller.messageLabel().getText().startsWith("Введите тип"));

            controller.typeField().setText("   ");
            controller.addButton().fire();
            verify("пробелы не сохранены", originalSize, controller.workoutTable().getItems().size());
            verify("ошибка не только цветом", true,
                    controller.messageLabel().getAccessibleText().startsWith("Ошибка:"));

            controller.typeField().setText("X".repeat(Workout.MAX_TYPE_LENGTH + 1));
            controller.addButton().fire();
            verify("слишком длинное значение не сохранено", originalSize,
                    controller.workoutTable().getItems().size());
            verify("ограничение 60 символов объяснено", true,
                    controller.messageLabel().getText().contains("60"));

            controller.typeField().setText("Кардио");
            controller.dateField().setValue(null);
            controller.statusField().setValue(WorkoutStatus.PLANNED);
            controller.addButton().fire();
            verify("без даты запись не сохранена", originalSize,
                    controller.workoutTable().getItems().size());
            verify("верный тип не очищен", "Кардио", controller.typeField().getText());
            verify("сообщение о дате", "Выберите дату тренировки.",
                    controller.messageLabel().getText());

            controller.dateField().setValue(LocalDate.of(2026, 10, 4));
            controller.addButton().fire();
            verify("исправленный ввод сохранён без перезапуска", originalSize + 1,
                    controller.workoutTable().getItems().size());
            verify("дубль после ошибки не создан", 1L,
                    controller.workoutTable().getItems().stream()
                            .filter(workout -> workout.type().equals("Кардио"))
                            .count());

            controller.typeField().setText("Тест");
            controller.dateField().setValue(LocalDate.of(2026, 10, 5));
            controller.statusField().setValue(null);
            controller.addButton().fire();
            verify("без состояния запись не сохранена", originalSize + 1,
                    controller.workoutTable().getItems().size());

            try {
                new Workout(100, "   ", LocalDate.now(), WorkoutStatus.PLANNED);
                throw new AssertionError("доменное исключение не возникло");
            } catch (IllegalArgumentException expected) {
                verify("доменное исключение", "Тип тренировки не может быть пустым",
                        expected.getMessage());
            }

            controller.typeField().clear();
            controller.dateField().setValue(LocalDate.of(2026, 10, 6));
            controller.statusField().setValue(WorkoutStatus.PLANNED);
            controller.typeField().requestFocus();
            sizeBeforeKeyboardScenario = controller.workoutTable().getItems().size();

            PauseTransition keyboardPause = new PauseTransition(Duration.millis(150));
            keyboardPause.setOnFinished(event -> runForwardTraversal());
            keyboardPause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void runForwardTraversal() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyCode.TAB);
            robot.keyRelease(KeyCode.TAB);
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(event -> {
                try {
                    verify("Tab перевёл фокус к дате", controller.dateField(), scene.getFocusOwner());
                    runBackwardTraversal();
                } catch (Throwable error) {
                    fail(error);
                }
            });
            pause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void runBackwardTraversal() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyCode.SHIFT);
            robot.keyPress(KeyCode.TAB);
            robot.keyRelease(KeyCode.TAB);
            robot.keyRelease(KeyCode.SHIFT);
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(event -> {
                try {
                    verify("Shift+Tab вернул фокус к типу", controller.typeField(), scene.getFocusOwner());
                    runKeyboardScenario();
                } catch (Throwable error) {
                    fail(error);
                }
            });
            pause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void runKeyboardScenario() {
        try {
            Robot robot = new Robot();
            for (KeyCode key : new KeyCode[]{KeyCode.C, KeyCode.A, KeyCode.R, KeyCode.D, KeyCode.I, KeyCode.O}) {
                robot.keyPress(key);
                robot.keyRelease(key);
            }
            robot.keyPress(KeyCode.ENTER);
            robot.keyRelease(KeyCode.ENTER);

            PauseTransition resultPause = new PauseTransition(Duration.millis(350));
            resultPause.setOnFinished(event -> finishCheck());
            resultPause.play();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void finishCheck() {
        try {
            verify("сценарий только с клавиатуры", sizeBeforeKeyboardScenario + 1,
                    controller.workoutTable().getItems().size());
            verify("кнопка осталась доступной", false, controller.addButton().isDisabled());

            controller.typeField().setText("Силовая");
            controller.dateField().setValue(null);
            controller.statusField().setValue(WorkoutStatus.PLANNED);
            controller.addButton().fire();
            saveScreenshot();

            System.out.println("Все проверки P08 пройдены.");
            Platform.exit();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void saveScreenshot() throws Exception {
        WritableImage image = scene.snapshot(null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        File output = new File(System.getProperty("p08.screenshot"));
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Не удалось создать каталог снимка: " + parent);
        }
        ImageIO.write(bufferedImage, "png", output);
        System.out.println("[OK] снимок сохранён: " + output.getPath());
    }

    private URL requireResource(String name) {
        return Objects.requireNonNull(
                getClass().getResource("/ru/npi/workouttracker/ui/" + name),
                "Не найден ресурс " + name
        );
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    private static void fail(Throwable error) {
        error.printStackTrace();
        System.exit(1);
    }
}
