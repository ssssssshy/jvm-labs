package ru.npi.workouttracker.ui;

import java.time.LocalDate;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.npi.workouttracker.application.WorkoutOperations;
import ru.npi.workouttracker.application.WorkoutAuditResult;
import ru.npi.workouttracker.application.WorkoutAuditTask;
import ru.npi.workouttracker.application.WorkoutHistoryAuditor;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class MainController {
    private static final LocalDate INITIAL_DATE = LocalDate.of(2026, 9, 15);
    private static final String INVALID_STYLE = "invalid-field";

    private final ObservableList<Workout> workouts = FXCollections.observableArrayList();
    private final WorkoutOperations service;
    private final WorkoutHistoryAuditor auditor;
    private WorkoutAuditTask currentAuditTask;
    private WorkoutAuditResult lastAuditResult;

    MainController(WorkoutOperations service) {
        this(service, new WorkoutHistoryAuditor(250));
    }

    MainController(WorkoutOperations service, WorkoutHistoryAuditor auditor) {
        this.service = service;
        this.auditor = auditor;
    }

    @FXML
    private TableView<Workout> workoutTable;
    @FXML
    private TableColumn<Workout, Number> idColumn;
    @FXML
    private TableColumn<Workout, String> typeColumn;
    @FXML
    private TableColumn<Workout, LocalDate> dateColumn;
    @FXML
    private TableColumn<Workout, WorkoutStatus> statusColumn;
    @FXML
    private TableColumn<Workout, Number> durationColumn;
    @FXML
    private TextField typeField;
    @FXML
    private DatePicker dateField;
    @FXML
    private ComboBox<WorkoutStatus> statusField;
    @FXML
    private TextField durationField;
    @FXML
    private Button addButton;
    @FXML
    private Button changeStatusButton;
    @FXML
    private Button removeButton;
    @FXML
    private Label countLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private TextField filterField;
    @FXML
    private DatePicker periodFromField;
    @FXML
    private DatePicker periodToField;
    @FXML
    private Label totalLabel;
    @FXML
    private Button totalButton;
    @FXML
    private Button startAuditButton;
    @FXML
    private Button cancelAuditButton;
    @FXML
    private ProgressBar auditProgress;
    @FXML
    private Label auditMessageLabel;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(value -> new ReadOnlyLongWrapper(value.getValue().id()));
        typeColumn.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().type()));
        dateColumn.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().date()));
        statusColumn.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().status()));
        durationColumn.setCellValueFactory(value -> new ReadOnlyLongWrapper(value.getValue().durationMinutes()));

        workoutTable.setItems(workouts);
        workoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        statusField.setItems(FXCollections.observableArrayList(WorkoutStatus.values()));
        statusField.setValue(WorkoutStatus.PLANNED);
        dateField.setValue(INITIAL_DATE);
        durationField.setText("60");
        periodFromField.setValue(INITIAL_DATE.minusMonths(1));
        periodToField.setValue(INITIAL_DATE.plusMonths(1));
        if (service.findAll().isEmpty()) {
            service.create("Силовая", INITIAL_DATE, WorkoutStatus.PLANNED);
        }
        refreshFromRepository();

        countLabel.textProperty().bind(Bindings.size(workouts).asString("Записей: %d"));
        changeStatusButton.disableProperty().bind(
                workoutTable.getSelectionModel().selectedItemProperty().isNull()
                        .or(statusField.valueProperty().isNull())
        );
        removeButton.disableProperty().bind(
                workoutTable.getSelectionModel().selectedItemProperty().isNull()
        );

        typeField.textProperty().addListener((observable, oldValue, newValue) -> clearFieldError(typeField));
        dateField.valueProperty().addListener((observable, oldValue, newValue) -> clearFieldError(dateField));
        statusField.valueProperty().addListener((observable, oldValue, newValue) -> clearFieldError(statusField));
        filterField.textProperty().addListener((observable, oldValue, newValue) ->
                workouts.setAll(service.filterByType(newValue)));
        Platform.runLater(typeField::requestFocus);
    }

    @FXML
    private void handleAdd() {
        clearValidation();
        Workout workout;
        try {
            int duration = Integer.parseInt(durationField.getText().strip());
            workout = service.create(typeField.getText(), dateField.getValue(), statusField.getValue(), duration);
        } catch (IllegalArgumentException expected) {
            showFieldError(invalidField(), expected.getMessage());
            return;
        }
        refreshFromRepository();
        typeField.clear();
        showSuccess("Добавлена тренировка: " + workout.type());
    }

    @FXML
    private void handleCalculateTotal() {
        try {
            int total = service.totalMinutes(periodFromField.getValue(), periodToField.getValue());
            totalLabel.setText("Суммарная длительность: " + total + " мин.");
        } catch (IllegalArgumentException expected) {
            totalLabel.setText(expected.getMessage());
        }
    }

    @FXML
    private void handleChangeStatus() {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Workout updated = service.changeStatus(selected.id(), statusField.getValue());
        refreshFromRepository();
        selectById(updated.id());
        showSuccess("Установлено состояние: " + updated.status());
    }

    @FXML
    private void handleRemove() {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Workout removed = service.remove(selected.id());
        refreshFromRepository();
        workoutTable.getSelectionModel().clearSelection();
        showSuccess("Удалена тренировка: " + removed.type());
    }

    @FXML
    private void handleStartAudit() {
        if (currentAuditTask != null && currentAuditTask.isRunning()) {
            return;
        }
        lastAuditResult = null;
        WorkoutAuditTask task = new WorkoutAuditTask(auditor, service.findAll());
        currentAuditTask = task;
        startAuditButton.setDisable(true);
        cancelAuditButton.setDisable(false);
        auditProgress.progressProperty().bind(task.progressProperty());
        auditMessageLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            WorkoutAuditResult result = task.getValue();
            finishAuditControls();
            if (result == null) {
                auditMessageLabel.setText("Проверка завершилась без публикации результата.");
                return;
            }
            lastAuditResult = result;
            auditProgress.setProgress(1);
            auditMessageLabel.setText("Проверка завершена: " + result.total()
                    + ", запланировано " + result.planned()
                    + ", выполнено " + result.completed() + ".");
        });
        task.setOnCancelled(event -> {
            finishAuditControls();
            auditProgress.setProgress(0);
            auditMessageLabel.setText("Проверка отменена. Неполный результат не опубликован.");
        });
        task.setOnFailed(event -> {
            finishAuditControls();
            auditProgress.setProgress(0);
            Throwable failure = task.getException();
            auditMessageLabel.setText("Ошибка проверки: "
                    + (failure == null ? "неизвестная причина" : failure.getMessage()));
        });

        Thread worker = new Thread(task, "workout-history-audit");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleCancelAudit() {
        if (currentAuditTask != null) {
            currentAuditTask.cancel(true);
        }
    }

    private void finishAuditControls() {
        auditProgress.progressProperty().unbind();
        auditMessageLabel.textProperty().unbind();
        startAuditButton.setDisable(false);
        cancelAuditButton.setDisable(true);
    }

    private javafx.scene.control.Control invalidField() {
        String type = typeField.getText();
        if (type == null || type.isBlank() || type.strip().length() > Workout.MAX_TYPE_LENGTH) {
            return typeField;
        }
        if (dateField.getValue() == null) {
            return dateField;
        }
        try {
            if (Integer.parseInt(durationField.getText().strip()) <= 0) {
                return durationField;
            }
        } catch (RuntimeException ignored) {
            return durationField;
        }
        return statusField;
    }

    private void refreshFromRepository() {
        workouts.setAll(service.findAll());
    }

    private void selectById(long id) {
        workouts.stream()
                .filter(workout -> workout.id() == id)
                .findFirst()
                .ifPresent(workoutTable.getSelectionModel()::select);
    }

    private void showSuccess(String text) {
        clearValidation();
        messageLabel.setText(text);
        messageLabel.setAccessibleText(text);
        messageLabel.getStyleClass().remove("message-error");
        if (!messageLabel.getStyleClass().contains("message-success")) {
            messageLabel.getStyleClass().add("message-success");
        }
    }

    private void showFieldError(javafx.scene.control.Control field, String text) {
        if (!field.getStyleClass().contains(INVALID_STYLE)) {
            field.getStyleClass().add(INVALID_STYLE);
        }
        messageLabel.setText(text);
        messageLabel.setAccessibleText("Ошибка: " + text);
        messageLabel.getStyleClass().remove("message-success");
        if (!messageLabel.getStyleClass().contains("message-error")) {
            messageLabel.getStyleClass().add("message-error");
        }
        field.requestFocus();
    }

    private void clearValidation() {
        clearFieldError(typeField);
        clearFieldError(dateField);
        clearFieldError(statusField);
        messageLabel.getStyleClass().remove("message-error");
    }

    private static void clearFieldError(javafx.scene.control.Control field) {
        field.getStyleClass().remove(INVALID_STYLE);
    }

    TableView<Workout> workoutTable() {
        return workoutTable;
    }

    TextField typeField() {
        return typeField;
    }

    DatePicker dateField() {
        return dateField;
    }

    ComboBox<WorkoutStatus> statusField() {
        return statusField;
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

    Label messageLabel() {
        return messageLabel;
    }

    TextField durationField() {
        return durationField;
    }

    TextField filterField() {
        return filterField;
    }

    DatePicker periodFromField() {
        return periodFromField;
    }

    DatePicker periodToField() {
        return periodToField;
    }

    Label totalLabel() {
        return totalLabel;
    }

    Button totalButton() {
        return totalButton;
    }

    Button startAuditButton() {
        return startAuditButton;
    }

    Button cancelAuditButton() {
        return cancelAuditButton;
    }

    ProgressBar auditProgress() {
        return auditProgress;
    }

    Label auditMessageLabel() {
        return auditMessageLabel;
    }

    WorkoutAuditResult lastAuditResult() {
        return lastAuditResult;
    }
}
