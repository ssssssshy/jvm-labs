package ru.npi.workouttracker.diagnostics;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OperationCatalog {
    private OperationCatalog() {
    }

    public static Map<String, String> titles(Class<?> serviceInterface) {
        Map<String, String> result = new LinkedHashMap<>();
        Arrays.stream(serviceInterface.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(OperationTitle.class))
                .sorted(java.util.Comparator.comparing(Method::getName))
                .forEach(method -> result.put(
                        method.getName(),
                        method.getAnnotation(OperationTitle.class).value()
                ));
        return Map.copyOf(result);
    }
}
