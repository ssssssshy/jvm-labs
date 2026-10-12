package ru.npi.workouttracker.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class WorkoutTypeJsonCodec {
    private final ObjectMapper json;

    WorkoutTypeJsonCodec(ObjectMapper json) {
        this.json = json;
    }

    List<WorkoutTypeReference> decode(String body) throws IOException {
        return decode(json.readTree(body));
    }

    List<WorkoutTypeReference> decode(JsonNode root) {
        if (root == null || !root.isArray()) {
            throw new IllegalArgumentException("Корень JSON должен быть массивом");
        }
        List<WorkoutTypeReference> parsed = new ArrayList<>();
        for (JsonNode item : root) {
            parsed.add(new WorkoutTypeReference(
                    requiredText(item, "code"),
                    requiredText(item, "name"),
                    requiredPositiveInt(item, "recommendedMinutes")
            ));
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Справочник не должен быть пустым");
        }
        return List.copyOf(parsed);
    }

    private static String requiredText(JsonNode item, String field) {
        JsonNode value = item.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Обязательное поле " + field + " отсутствует");
        }
        return value.textValue();
    }

    private static int requiredPositiveInt(JsonNode item, String field) {
        JsonNode value = item.get(field);
        if (value == null || !value.isIntegralNumber() || value.intValue() <= 0) {
            throw new IllegalArgumentException("Поле " + field + " должно быть положительным целым");
        }
        return value.intValue();
    }
}
