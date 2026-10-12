package ru.npi.workouttracker.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class WorkoutTypeCache {
    private final Path path;
    private final ObjectMapper json = new ObjectMapper();
    private final WorkoutTypeJsonCodec codec = new WorkoutTypeJsonCodec(json);

    public WorkoutTypeCache(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public Optional<DirectorySnapshot> read() {
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try {
            JsonNode root = json.readTree(Files.readString(path, StandardCharsets.UTF_8));
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("корень кэша должен быть объектом");
            }
            JsonNode receivedAtNode = root.get("receivedAt");
            if (receivedAtNode == null || !receivedAtNode.isTextual()) {
                throw new IllegalArgumentException("в кэше нет времени получения");
            }
            Instant receivedAt = Instant.parse(receivedAtNode.textValue());
            List<WorkoutTypeReference> entries = codec.decode(root.get("entries"));
            return Optional.of(new DirectorySnapshot(entries, DirectorySource.CACHE, receivedAt));
        } catch (IOException | IllegalArgumentException error) {
            throw new CacheReadException("Не удалось прочитать сохранённые данные: " + error.getMessage(), error);
        }
    }

    public void write(List<WorkoutTypeReference> entries, Instant receivedAt) {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            ObjectNode root = json.createObjectNode();
            root.put("receivedAt", receivedAt.toString());
            ArrayNode array = root.putArray("entries");
            for (WorkoutTypeReference entry : entries) {
                ObjectNode item = array.addObject();
                item.put("code", entry.code());
                item.put("name", entry.name());
                item.put("recommendedMinutes", entry.recommendedMinutes());
            }
            temporary = Files.createTempFile(parent, "workout-types-", ".tmp");
            Files.writeString(temporary, json.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                    StandardCharsets.UTF_8);
            replaceAtomically(temporary, absolute);
            temporary = null;
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось атомарно сохранить справочник", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Основная ошибка сохранения важнее ошибки удаления временного файла.
                }
            }
        }
    }

    private static void replaceAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
