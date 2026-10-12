package ru.npi.workouttracker.lesson;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutStatus;
import ru.npi.workouttracker.extension.WorkoutReportProvider;

public final class ServiceLoaderDemo {
    private ServiceLoaderDemo() {
    }

    public static void main(String[] args) {
        List<WorkoutReportProvider> providers = ServiceLoader.load(WorkoutReportProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(WorkoutReportProvider::code))
                .toList();
        if (providers.size() != 2) {
            throw new IllegalStateException("Ожидались два расширения, найдено: " + providers.size());
        }

        List<Workout> workouts = List.of(
                new Workout(1, "Бег", LocalDate.of(2026, 10, 1), WorkoutStatus.COMPLETED),
                new Workout(2, "Йога", LocalDate.of(2026, 10, 2), WorkoutStatus.PLANNED),
                new Workout(3, "Бег", LocalDate.of(2026, 10, 3), WorkoutStatus.PLANNED)
        );

        System.out.println("API загружен: " + classLoaderName(WorkoutReportProvider.class));
        for (WorkoutReportProvider provider : providers) {
            System.out.println("[OK] " + provider.code() + " | " + provider.title()
                    + " | loader=" + classLoaderName(provider.getClass())
                    + " | " + provider.render(workouts));
        }
        System.out.println("Все проверки P21 пройдены: ServiceLoader нашёл два поставщика.");
    }

    private static String classLoaderName(Class<?> type) {
        ClassLoader loader = type.getClassLoader();
        return loader == null ? "bootstrap" : loader.getClass().getName();
    }
}
