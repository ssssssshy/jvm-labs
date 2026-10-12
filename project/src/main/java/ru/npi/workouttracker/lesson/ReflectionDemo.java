package ru.npi.workouttracker.lesson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import ru.npi.workouttracker.application.WorkoutOperations;
import ru.npi.workouttracker.application.WorkoutService;
import ru.npi.workouttracker.diagnostics.MonitoringProxy;
import ru.npi.workouttracker.diagnostics.OperationCatalog;
import ru.npi.workouttracker.diagnostics.ServiceCallMetrics;
import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.infrastructure.MemoryWorkoutRepository;

public final class ReflectionDemo {
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    private @interface ClassOnlyTitle {
        String value();
    }

    private interface ClassRetentionSample {
        @ClassOnlyTitle("Описание не доступно во время выполнения")
        void operation();
    }

    private ReflectionDemo() {
    }

    public static void main(String[] args) throws NoSuchMethodException {
        verifyMetadata();
        verifyRetentionDifference();
        verifyProxy();
        System.out.println("Все проверки P10 пройдены.");
    }

    private static void verifyMetadata() {
        Map<String, String> titles = OperationCatalog.titles(WorkoutOperations.class);
        verify("найдены две аннотированные операции", 2, titles.size());
        verify("русское описание create", "Добавить тренировку", titles.get("create"));
        verify("русское описание changeStatus",
                "Изменить состояние тренировки", titles.get("changeStatus"));
        verify("метод без аннотации пропущен", false, titles.containsKey("findAll"));
    }

    private static void verifyRetentionDifference() throws NoSuchMethodException {
        ClassOnlyTitle title = ClassRetentionSample.class
                .getMethod("operation")
                .getAnnotation(ClassOnlyTitle.class);
        verify("CLASS retention не видна через reflection", null, title);
    }

    private static void verifyProxy() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        WorkoutOperations direct = new WorkoutService(new MemoryWorkoutRepository());
        Workout directResult = direct.create("Кардио", date, WorkoutStatus.PLANNED);

        ServiceCallMetrics metrics = new ServiceCallMetrics();
        WorkoutOperations proxied = MonitoringProxy.wrap(
                WorkoutOperations.class,
                new WorkoutService(new MemoryWorkoutRepository()),
                metrics
        );
        Workout proxyResult = proxied.create("Кардио", date, WorkoutStatus.PLANNED);
        verify("прямой и proxy-вызовы вернули одинаковый результат", directResult, proxyResult);

        proxied.findAll();
        proxied.toString();
        proxied.hashCode();
        proxied.equals(proxied);
        verify("технические вызовы не исказили метрику", 1, metrics.totalCalls());
        verify("equals остался рефлексивным", true, proxied.equals(proxied));

        Workout updated = proxied.changeStatus(proxyResult.id(), WorkoutStatus.COMPLETED);
        verify("вторая предметная операция выполнена", WorkoutStatus.COMPLETED, updated.status());
        verify("два предметных вызова учтены", 2, metrics.totalCalls());

        IllegalArgumentException directError = captureDateError(direct);
        IllegalArgumentException proxyError = captureDateError(proxied);
        verify("proxy сохранил тип исходного исключения", directError.getClass(), proxyError.getClass());
        verify("proxy сохранил причину отказа", directError.getMessage(), proxyError.getMessage());
        verify("неудачный предметный вызов тоже учтён", 3, metrics.totalCalls());
    }

    private static IllegalArgumentException captureDateError(WorkoutOperations service) {
        try {
            service.create("Плавание", null, WorkoutStatus.PLANNED);
            throw new AssertionError("отсутствующая дата должна быть отклонена");
        } catch (IllegalArgumentException expected) {
            return expected;
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
