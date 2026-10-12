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
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class P06UiCheck extends Application {
    @Override
    public void start(Stage stage) {
        try {
            WorkoutView view = new WorkoutView();
            Scene scene = new Scene(view.root(), 980, 600);
            stage.setTitle("Проверка P06 — наблюдаемые данные и привязки");
            stage.setScene(scene);
            stage.show();

            verify("начальный счётчик", "Записей: 1", view.countLabel().getText());
            verify("без выбора удаление недоступно", true, view.removeButton().isDisabled());
            verify("пустой ввод блокирует добавление", true, view.addButton().isDisabled());

            view.typeField().setText("Йога");
            verify("корректный ввод включает добавление", false, view.addButton().isDisabled());
            view.addButton().fire();
            verify("таблица получила уведомление о добавлении", 2, view.table().getItems().size());
            verify("привязка увеличила счётчик", "Записей: 2", view.countLabel().getText());

            Workout original = view.table().getItems().get(0);
            view.table().getSelectionModel().select(0);
            verify("при выборе удаление доступно", false, view.removeButton().isDisabled());
            view.changeStatusButton().fire();
            Workout updated = view.table().getItems().get(0);
            verify("новое состояние видно в таблице", WorkoutStatus.IN_PROGRESS, updated.status());
            verify("запись заменена новым объектом", false, original == updated);

            view.removeButton().fire();
            verify("после удаления осталась одна строка", 1, view.table().getItems().size());
            verify("привязка уменьшила счётчик", "Записей: 1", view.countLabel().getText());
            verify("выделение очищено", null, view.table().getSelectionModel().getSelectedItem());
            verify("после удаления кнопка недоступна", true, view.removeButton().isDisabled());

            view.table().getSelectionModel().select(0);
            view.removeButton().fire();
            verify("пустой список", 0, view.table().getItems().size());
            verify("счётчик пустого списка", "Записей: 0", view.countLabel().getText());

            add(view, "Кардио");
            add(view, "Растяжка");
            view.table().getSelectionModel().select(0);
            view.changeStatusButton().fire();
            view.table().getSelectionModel().clearSelection();
            verify("финальный счётчик для снимка", "Записей: 2", view.countLabel().getText());

            PauseTransition pause = new PauseTransition(Duration.millis(700));
            pause.setOnFinished(event -> finishCheck(scene));
            pause.play();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void add(WorkoutView view, String type) {
        view.typeField().setText(type);
        view.addButton().fire();
    }

    private void finishCheck(Scene scene) {
        try {
            WritableImage image = scene.snapshot(null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            File output = new File(System.getProperty("p06.screenshot"));
            File parent = output.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Не удалось создать каталог снимка: " + parent);
            }
            ImageIO.write(bufferedImage, "png", output);
            System.out.println("[OK] снимок сохранён: " + output.getPath());
            System.out.println("Все проверки P06 пройдены.");
            Platform.exit();
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
