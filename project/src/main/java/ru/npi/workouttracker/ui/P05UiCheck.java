package ru.npi.workouttracker.ui;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class P05UiCheck extends Application {
    @Override
    public void start(Stage stage) {
        try {
            WorkoutView view = new WorkoutView();
            Scene scene = new Scene(view.root(), 760, 480);
            stage.setTitle("Проверка P05 — Трекер тренировок");
            stage.setScene(scene);
            stage.show();

            verify("при первом запуске одна строка", 1, view.table().getItems().size());

            view.typeField().setText("Йога");
            view.addButton().fire();
            verify("одно нажатие добавляет одну строку", 2, view.table().getItems().size());
            verify("поле очищается после успеха", "", view.typeField().getText());

            view.typeField().setText("   ");
            verify("пустой ввод не меняет таблицу", 2, view.table().getItems().size());
            verify("пустой ввод блокирует добавление", true, view.addButton().isDisabled());

            stage.setWidth(900);
            stage.setHeight(600);
            view.typeField().setText("Растяжка");
            view.addButton().fire();
            verify("повторный корректный ввод добавлен", 3, view.table().getItems().size());

            PauseTransition pause = new PauseTransition(Duration.millis(700));
            pause.setOnFinished(event -> finishCheck(scene));
            pause.play();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private void finishCheck(Scene scene) {
        try {
            WritableImage image = scene.snapshot(null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            File output = new File(System.getProperty("p05.screenshot"));
            File parent = output.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Не удалось создать каталог снимка: " + parent);
            }
            ImageIO.write(bufferedImage, "png", output);
            System.out.println("[OK] снимок сохранён: " + output.getPath());
            System.out.println("Все проверки P05 пройдены.");
            Platform.exit();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
