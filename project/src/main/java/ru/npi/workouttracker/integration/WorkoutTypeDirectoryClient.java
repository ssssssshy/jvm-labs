package ru.npi.workouttracker.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class WorkoutTypeDirectoryClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final WorkoutTypeJsonCodec codec = new WorkoutTypeJsonCodec(new ObjectMapper());

    public List<WorkoutTypeReference> fetch(URI endpoint) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new DirectoryResponseException("Сервер вернул HTTP " + response.statusCode());
            }
            return parseCompleteSet(response.body());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new DirectoryUnavailableException("Запрос справочника прерван", error);
        } catch (IOException error) {
            throw new DirectoryUnavailableException("Не удалось получить справочник", error);
        }
    }

    private List<WorkoutTypeReference> parseCompleteSet(String body) {
        try {
            return codec.decode(body);
        } catch (IOException | IllegalArgumentException error) {
            throw new DirectoryResponseException("Не удалось разобрать JSON: " + error.getMessage(), error);
        }
    }
}
