package ru.npi.workouttracker.lesson;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import ru.npi.workouttracker.collection.CountMap;

public final class CountMapDemo {
    private CountMapDemo() {
    }

    public static void main(String[] args) {
        CountMap<String> workoutTypes = new CountMap<>();
        verify("пустой счётчик", 0, workoutTypes.getCount("Силовая"));

        workoutTypes.add("Силовая");
        workoutTypes.add("Силовая");
        workoutTypes.add("Кардио");
        verify("два повторения", 2, workoutTypes.getCount("Силовая"));
        verify("одна запись", 1, workoutTypes.getCount("Кардио"));
        verify("отсутствующий ключ", 0, workoutTypes.getCount("Йога"));
        verify("число разных типов", 2, workoutTypes.toMap().size());

        CountMap<String> additionalTypes = new CountMap<>();
        additionalTypes.add("Силовая");
        additionalTypes.add("Йога");
        additionalTypes.add("Йога");
        Map<String, Integer> additionalBeforeMerge = additionalTypes.toMap();

        workoutTypes.addAll(additionalTypes);
        verify("объединённая силовая", 3, workoutTypes.getCount("Силовая"));
        verify("объединённое кардио", 1, workoutTypes.getCount("Кардио"));
        verify("объединённая йога", 2, workoutTypes.getCount("Йога"));
        verify("источник объединения не изменился", additionalBeforeMerge, additionalTypes.toMap());

        Map<String, Integer> snapshot = workoutTypes.toMap();
        expectFailure("снимок нельзя изменить", UnsupportedOperationException.class,
                () -> snapshot.put("Плавание", 10));
        workoutTypes.add("Плавание");
        verify("ранее полученный снимок не меняется", false, snapshot.containsKey("Плавание"));
        verify("исходный счётчик изменился", 1, workoutTypes.getCount("Плавание"));

        Map<Object, Integer> destination = new HashMap<>();
        workoutTypes.copyTo(destination);
        verify("копирование в super-приёмник", 3, destination.get("Силовая"));

        Map<String, Integer> sorted = new TreeMap<>(workoutTypes.toMap());
        System.out.println("Исходный снимок: " + workoutTypes.toMap());
        System.out.println("Сортировка по типу: " + sorted);

        verify("remove возвращает прежнее количество", 2, workoutTypes.remove("Йога"));
        verify("удалённый ключ отсутствует", 0, workoutTypes.getCount("Йога"));
        verify("remove отсутствующего ключа", 0, workoutTypes.remove("Интервальная"));
        expectFailure("null запрещён", NullPointerException.class, () -> workoutTypes.add(null));

        System.out.println("Все проверки P04 пройдены.");
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    private static void expectFailure(
            String scenario,
            Class<? extends RuntimeException> expectedType,
            Runnable action
    ) {
        try {
            action.run();
            throw new AssertionError(scenario + ": ожидалась ошибка " + expectedType.getSimpleName());
        } catch (RuntimeException exception) {
            if (!expectedType.isInstance(exception)) {
                throw new AssertionError(scenario + ": получена неожиданная ошибка", exception);
            }
            System.out.println("[OK] " + scenario + ": получена "
                    + exception.getClass().getSimpleName());
        }
    }
}
