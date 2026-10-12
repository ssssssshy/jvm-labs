package ru.npi.workouttracker.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class P07UiCheck extends Application {
    @Override
    public void start(Stage stage) {
        try {
            URL fxml = requireResource("main-view.fxml");
            URL css = requireResource("style.css");
            FXMLLoader loader = DesktopApp.createInMemoryLoader(fxml);
            Parent root = loader.load();
            MainController controller = loader.getController();

            verify("FXML загрузил контроллер", true, controller != null);
            verify("поле типа связано", true, controller.typeField() != null);
            verify("поле даты связано", true, controller.dateField() != null);
            verify("поле состояния связано", true, controller.statusField() != null);
            verify("начальная строка", 1, controller.workoutTable().getItems().size());

            Scene scene = new Scene(root, 980, 650);
            scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("Проверка P07 — FXML и CSS");
            stage.setMinWidth(760);
            stage.setMinHeight(500);
            stage.setScene(scene);
            stage.show();

            controller.typeField().setText("Кардио");
            controller.dateField().setValue(LocalDate.of(2026, 10, 1));
            controller.statusField().setValue(WorkoutStatus.PLANNED);
            controller.addButton().fire();
            verify("обработчик FXML добавил строку", 2, controller.workoutTable().getItems().size());
            verify("счётчик обновился", "Записей: 2", controller.countLabel().getText());

            controller.workoutTable().getSelectionModel().select(0);
            controller.statusField().setValue(WorkoutStatus.IN_PROGRESS);
            controller.changeStatusButton().fire();
            verify("состояние заменено через контроллер", WorkoutStatus.IN_PROGRESS,
                    controller.workoutTable().getItems().get(0).status());

            verifyBrokenFxId(fxml);

            stage.setWidth(800);
            stage.setHeight(520);
            stage.setWidth(980);
            stage.setHeight(650);

            PauseTransition pause = new PauseTransition(Duration.millis(700));
            pause.setOnFinished(event -> finishCheck(scene));
            pause.play();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private URL requireResource(String name) {
        return Objects.requireNonNull(
                getClass().getResource("/ru/npi/workouttracker/ui/" + name),
                "Не найден ресурс " + name
        );
    }

    private static void verifyBrokenFxId(URL fxml) throws Exception {
        String source = new String(fxml.openStream().readAllBytes(), StandardCharsets.UTF_8);
        String broken = source.replace("fx:id=\"typeField\"", "fx:id=\"brokenTypeField\"");
        FXMLLoader brokenLoader = DesktopApp.createInMemoryLoader(fxml);
        try {
            brokenLoader.load(new ByteArrayInputStream(broken.getBytes(StandardCharsets.UTF_8)));
            throw new AssertionError("повреждённый fx:id должен приводить к ошибке загрузки");
        } catch (javafx.fxml.LoadException expected) {
            System.out.println("[OK] повреждённый fx:id воспроизвёл LoadException");
        }
    }

    private void finishCheck(Scene scene) {
        try {
            WritableImage image = scene.snapshot(null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            File output = new File(System.getProperty("p07.screenshot"));
            File parent = output.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Не удалось создать каталог снимка: " + parent);
            }
            ImageIO.write(bufferedImage, "png", output);
            System.out.println("[OK] снимок сохранён: " + output.getPath());
            System.out.println("Все проверки P07 пройдены.");
            Platform.exit();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
