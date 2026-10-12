package ru.npi.workouttracker.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ru.npi.workouttracker.domain.DuplicateWorkoutException;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;

public final class WorkoutServiceTest {
    private WorkoutServiceTest() {
    }

    public static void main(String[] args) {
        createAndReadWithoutJavaFx();
        repositoriesAreIndependent();
        duplicateIdDoesNotPartiallyChangeStorage();
        stableIdSurvivesViewSorting();
        missingDateDoesNotChangeStorage();
        System.out.println("Все проверки P09 пройдены без JavaFX.");
    }

    private static void createAndReadWithoutJavaFx() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());
        Workout created = service.create("Кардио", LocalDate.of(2026, 10, 7), WorkoutStatus.PLANNED);
        verify("новая запись сохранена", 1, service.findAll().size());
        verify("первый устойчивый id", 1L, created.id());
    }

    private static void repositoriesAreIndependent() {
        WorkoutService first = new WorkoutService(new MemoryWorkoutRepository());
        WorkoutService second = new WorkoutService(new MemoryWorkoutRepository());
        first.create("Силовая", LocalDate.of(2026, 10, 8), WorkoutStatus.PLANNED);
        verify("первый репозиторий содержит запись", 1, first.findAll().size());
        verify("второй репозиторий независим", 0, second.findAll().size());
    }

    private static void duplicateIdDoesNotPartiallyChangeStorage() {
        MemoryWorkoutRepository repository = new MemoryWorkoutRepository();
        repository.save(new Workout(17, "Йога", LocalDate.of(2026, 10, 9), WorkoutStatus.PLANNED));
        List<Workout> before = repository.findAll();
        try {
            repository.save(new Workout(17, "Бег", LocalDate.of(2026, 10, 10), WorkoutStatus.COMPLETED));
            throw new AssertionError("дублирующийся id должен быть отклонён");
        } catch (DuplicateWorkoutException expected) {
            verify("повторный id отклонён", true, expected.getMessage().contains("id=17"));
        }
        verify("после отказа хранилище не изменилось", before, repository.findAll());
    }

    private static void stableIdSurvivesViewSorting() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());
        Workout cardio = service.create("Кардио", LocalDate.of(2026, 10, 11), WorkoutStatus.PLANNED);
        service.create("Аквааэробика", LocalDate.of(2026, 10, 12), WorkoutStatus.PLANNED);

        List<Workout> sortedView = new ArrayList<>(service.findAll());
        sortedView.sort(Comparator.comparing(Workout::type));
        verify("после сортировки строка сменилась", "Аквааэробика", sortedView.getFirst().type());

        Workout updated = service.changeStatus(cardio.id(), WorkoutStatus.COMPLETED);
        verify("обновлена та же запись по id", cardio.id(), updated.id());
        verify("состояние изменено", WorkoutStatus.COMPLETED, updated.status());
    }

    private static void missingDateDoesNotChangeStorage() {
        WorkoutService service = new WorkoutService(new MemoryWorkoutRepository());
        try {
            service.create("Плавание", null, WorkoutStatus.PLANNED);
            throw new AssertionError("отсутствующая дата должна быть отклонена");
        } catch (IllegalArgumentException expected) {
            verify("причина отказа понятна", "Выберите дату тренировки.", expected.getMessage());
        }
        verify("отказ без даты не изменил хранилище", 0, service.findAll().size());
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
