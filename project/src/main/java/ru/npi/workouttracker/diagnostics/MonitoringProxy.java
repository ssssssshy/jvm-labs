package ru.npi.workouttracker.diagnostics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

public final class MonitoringProxy {
    private MonitoringProxy() {
    }

    public static <T> T wrap(Class<T> serviceInterface, T target, ServiceCallMetrics metrics) {
        Objects.requireNonNull(serviceInterface, "Интерфейс обязателен");
        Objects.requireNonNull(target, "Целевой сервис обязателен");
        Objects.requireNonNull(metrics, "Счётчик обязателен");
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("Для Proxy нужен интерфейс");
        }

        Object proxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxyObject, method, arguments) -> invoke(proxyObject, method, arguments, target, metrics)
        );
        return serviceInterface.cast(proxy);
    }

    private static Object invoke(
            Object proxy,
            Method method,
            Object[] arguments,
            Object target,
            ServiceCallMetrics metrics
    ) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> proxy == arguments[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "Наблюдаемый сервис тренировок";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        if (method.isAnnotationPresent(OperationTitle.class)) {
            metrics.record(method.getName());
        }
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException wrapper) {
            throw wrapper.getCause();
        }
    }
}
