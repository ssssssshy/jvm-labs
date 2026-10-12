package ru.npi.workouttracker.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.npi.workouttracker.domain.Workout;
import ru.npi.workouttracker.domain.WorkoutRepository;
import ru.npi.workouttracker.domain.WorkoutStatus;

public final class WorkoutCsvImporter {
    private static final String HEADER = "code;type;date";
    private final WorkoutRepository repository;

    public WorkoutCsvImporter(WorkoutRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Репозиторий обязателен");
    }

    public List<Workout> importFile(Path csvFile) {
        Objects.requireNonNull(csvFile, "CSV-файл обязателен");
        List<String> lines;
        try {
            lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalArgumentException("Не удалось прочитать CSV: " + csvFile, error);
        }
        List<Workout> parsed = parseAll(lines);
        return repository.saveAll(parsed);
    }

    private List<Workout> parseAll(List<String> lines) {
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst().strip())) {
            throw new IllegalArgumentException("Ожидается заголовок " + HEADER);
        }
        long nextId = repository.findAll().stream().mapToLong(Workout::id).max().orElse(0) + 1;
        List<Workout> result = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            int number = index + 1;
            if (line.isBlank()) {
                throw new IllegalArgumentException("Пустая строка CSV: " + number);
            }
            String[] columns = line.split(";", -1);
            if (columns.length != 3) {
                throw new IllegalArgumentException(
                        "Строка " + number + ": ожидаются 3 поля без кавычек и ';' внутри значения"
                );
            }
            String code = columns[0].strip();
            String type = columns[1].strip();
            String dateText = columns[2].strip();
            try {
                result.add(new Workout(
                        nextId++,
                        code,
                        type,
                        LocalDate.parse(dateText),
                        WorkoutStatus.PLANNED
                ));
            } catch (DateTimeParseException error) {
                throw new IllegalArgumentException(
                        "Строка " + number + ", код " + code + ": дата должна иметь вид YYYY-MM-DD",
                        error
                );
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Строка " + number + ": " + error.getMessage(),
                        error
                );
            }
        }
        return List.copyOf(result);
    }
}
