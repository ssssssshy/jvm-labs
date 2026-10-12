package ru.npi.workouttracker.lesson;

import java.util.List;

import ru.npi.workouttracker.integration.WorkoutTypeDirectory;
import ru.npi.workouttracker.integration.WorkoutTypeDirectoryClient;
import ru.npi.workouttracker.integration.WorkoutTypeReference;

public final class HttpJsonDemo {
    private HttpJsonDemo() {
    }

    public static void main(String[] args) throws Exception {
        WorkoutTypeDirectory directory = new WorkoutTypeDirectory(new WorkoutTypeDirectoryClient());
        int releasedPort;
        try (LocalWorkoutDirectoryServer server = LocalWorkoutDirectoryServer.start(0)) {
            releasedPort = server.port();
            List<WorkoutTypeReference> accepted = directory.refresh(server.endpoint("/directory"));
            verify("HTTP 200 и JSON приняты", 2, accepted.size());
            verify("поля DTO преобразованы", new WorkoutTypeReference("WT-RUN", "Бег", 30), accepted.getFirst());

            expectFailure("ошибка HTTP отличается", "HTTP 500",
                    () -> directory.refresh(server.endpoint("/error")));
            verify("HTTP 500 не изменил список", accepted, directory.confirmed());

            expectFailure("ошибка JSON отличается", "JSON",
                    () -> directory.refresh(server.endpoint("/broken")));
            verify("повреждённый JSON не изменил список", accepted, directory.confirmed());
        }

        try (LocalWorkoutDirectoryServer restarted = LocalWorkoutDirectoryServer.start(releasedPort)) {
            verify("порт доступен после остановки", releasedPort, restarted.port());
            verify("повторный HTTP-запуск работает", 2,
                    directory.refresh(restarted.endpoint("/directory")).size());
        }
        System.out.println("Все проверки P15 пройдены.");
    }

    private static void expectFailure(String scenario, String fragment, Runnable action) {
        try {
            action.run();
            throw new AssertionError(scenario + ": ожидался отказ");
        } catch (IllegalStateException expected) {
            verify(scenario, true, expected.getMessage().contains(fragment));
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }
}
