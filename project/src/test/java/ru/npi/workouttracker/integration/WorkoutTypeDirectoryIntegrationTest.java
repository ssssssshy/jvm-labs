package ru.npi.workouttracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkoutTypeDirectoryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-04T17:00:00Z");

    @TempDir
    Path temporaryDirectory;
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/valid", exchange -> respond(exchange, """
                [{"code":"WT-RUN","name":"Бег","recommendedMinutes":30}]
                """));
        server.createContext("/broken", exchange -> respond(exchange, "[{\"code\":"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        if (server.getExecutor() instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception error) {
                throw new IllegalStateException("Не удалось закрыть исполнитель тестового HTTP-сервера", error);
            }
        }
    }

    @Test
    void brokenJsonDoesNotOverwriteVerifiedCache() throws IOException {
        WorkoutTypeCache cache = new WorkoutTypeCache(temporaryDirectory.resolve("types.json"));
        CachedWorkoutTypeDirectory directory = new CachedWorkoutTypeDirectory(
                new WorkoutTypeDirectoryClient(), cache,
                Clock.fixed(NOW, ZoneOffset.UTC), 1
        );
        DirectorySnapshot fresh = directory.synchronize(endpoint("/valid"));
        String verifiedCache = Files.readString(cache.path(), StandardCharsets.UTF_8);

        assertThrows(DirectoryResponseException.class,
                () -> directory.synchronize(endpoint("/broken")));

        assertEquals(DirectorySource.SERVER, fresh.source());
        assertEquals(verifiedCache, Files.readString(cache.path(), StandardCharsets.UTF_8));
    }

    private URI endpoint(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
