package ru.npi.workouttracker.diagnostics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServiceCallMetrics {
    private final Map<String, AtomicInteger> callsByMethod = new ConcurrentHashMap<>();

    void record(String methodName) {
        callsByMethod.computeIfAbsent(methodName, ignored -> new AtomicInteger()).incrementAndGet();
    }

    public int callsFor(String methodName) {
        AtomicInteger calls = callsByMethod.get(methodName);
        return calls == null ? 0 : calls.get();
    }

    public int totalCalls() {
        return callsByMethod.values().stream().mapToInt(AtomicInteger::get).sum();
    }
}
