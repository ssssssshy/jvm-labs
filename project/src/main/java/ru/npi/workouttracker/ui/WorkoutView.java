package ru.npi.workouttracker.ui;

import java.time.LocalDate;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

final class WorkoutView {
    private static final LocalDate LAB_DATE = LocalDate.of(2026, 9, 15);

    private final ObservableList<Workout> workouts = FXCollections.observableArrayList();
    private final TableView<Workout> table = new TableView<>(workouts);
    private final TextField typeField = new TextField();
    private final Button addButton = new Button("Добавить тренировку");
    private final Button changeStatusButton = new Button("Начать выбранную");
    private final Button removeButton = new Button("Удалить выбранную");
    private final Label countLabel = new Label();
    private final Label message = new Label();
    private final VBox root = new VBox(12);
    private long nextId = 2;

    WorkoutView() {
        workouts.add(new Workout(1, "Силовая", LAB_DATE, WorkoutStatus.PLANNED));
        configureTable();
        configureForm();

        Label title = new Label("Трекер тренировок");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        HBox form = new HBox(10, typeField, addButton, changeStatusButton, removeButton);
        HBox.setHgrow(typeField, Priority.ALWAYS);

        root.setPadding(new Insets(20));
        root.getChildren().addAll(title, countLabel, table, form, message);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    Parent root() {
        return root;
    }

    TableView<Workout> table() {
        return table;
    }

    TextField typeField() {
        return typeField;
    }

    Button addButton() {
        return addButton;
    }

    Button changeStatusButton() {
        return changeStatusButton;
    }

    Button removeButton() {
        return removeButton;
    }

    Label countLabel() {
        return countLabel;
    }

    Label message() {
        return message;
    }

    private void configureTable() {
        TableColumn<Workout, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().id()));

        TableColumn<Workout, String> typeColumn = new TableColumn<>("Тип тренировки");
        typeColumn.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().type()));

        TableColumn<Workout, LocalDate> dateColumn = new TableColumn<>("Дата");
        dateColumn.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().date()));

        TableColumn<Workout, WorkoutStatus> statusColumn = new TableColumn<>("Состояние");
        statusColumn.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().status()));

        table.getColumns().add(idColumn);
        table.getColumns().add(typeColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Тренировки ещё не добавлены"));
    }

    private void configureForm() {
        typeField.setPromptText("Например, кардио или растяжка");
        addButton.setDefaultButton(true);
        addButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> typeField.getText().isBlank(),
                typeField.textProperty()
        ));
        addButton.setOnAction(event -> addWorkout());

        changeStatusButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );
        changeStatusButton.setOnAction(event -> changeSelectedStatus());

        removeButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );
        removeButton.setOnAction(event -> removeSelected());

        countLabel.textProperty().bind(Bindings.size(workouts).asString("Записей: %d"));
    }

    private void addWorkout() {
        String type = typeField.getText();
        if (type == null || type.isBlank()) {
            message.setText("Введите тип тренировки, например «Кардио».");
            message.setStyle("-fx-text-fill: #b42318;");
            return;
        }

        Workout workout = new Workout(nextId++, type, LAB_DATE, WorkoutStatus.PLANNED);
        workouts.add(workout);
        typeField.clear();
        message.setText("Добавлена тренировка: " + workout.type());
        message.setStyle("-fx-text-fill: #067647;");
    }

    private void changeSelectedStatus() {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }

        Workout updated = workouts.get(index).withStatus(WorkoutStatus.IN_PROGRESS);
        workouts.set(index, updated);
        table.getSelectionModel().select(index);
        message.setText("Тренировка переведена в состояние IN_PROGRESS.");
        message.setStyle("-fx-text-fill: #067647;");
    }

    private void removeSelected() {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }

        Workout removed = workouts.remove(index);
        table.getSelectionModel().clearSelection();
        message.setText("Удалена тренировка: " + removed.type());
        message.setStyle("-fx-text-fill: #067647;");
    }
}
