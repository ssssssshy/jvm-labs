package ru.npi.workouttracker.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CountMap<T> {
    private final Map<T, Integer> counts = new HashMap<>();

    public void add(T element) {
        counts.merge(requireElement(element), 1, Math::addExact);
    }

    public int getCount(T element) {
        return counts.getOrDefault(requireElement(element), 0);
    }

    public int remove(T element) {
        Integer previousCount = counts.remove(requireElement(element));
        return previousCount == null ? 0 : previousCount;
    }

    public void addAll(CountMap<? extends T> source) {
        Objects.requireNonNull(source, "Исходный счётчик обязателен");
        for (var entry : source.toMap().entrySet()) {
            counts.merge(entry.getKey(), entry.getValue(), Math::addExact);
        }
    }

    public Map<T, Integer> toMap() {
        return Map.copyOf(counts);
    }

    public void copyTo(Map<? super T, Integer> destination) {
        Objects.requireNonNull(destination, "Карта-приёмник обязательна");
        destination.putAll(counts);
    }

    private T requireElement(T element) {
        return Objects.requireNonNull(element, "Элемент счётчика не может быть null");
    }
}
