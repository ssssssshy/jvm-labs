package ru.npi.workouttracker.lesson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

final class LocalWorkoutDirectoryServer implements AutoCloseable {
    private static final String VALID_JSON = """
            [
              {"code":"WT-RUN","name":"Бег","recommendedMinutes":30},
              {"code":"WT-YOGA","name":"Йога","recommendedMinutes":45}
            ]
            """;
    private final HttpServer server;
    private final ExecutorService executor;

    private LocalWorkoutDirectoryServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/directory", exchange -> respond(exchange, 200, VALID_JSON));
        server.createContext("/error", exchange -> respond(exchange, 500, "{\"error\":\"demo\"}"));
        server.createContext("/broken", exchange -> respond(exchange, 200, "[{\"code\":\"WT-BROKEN\""));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(400);
                respond(exchange, 200, VALID_JSON);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        server.start();
    }

    static LocalWorkoutDirectoryServer start(int port) throws IOException {
        return new LocalWorkoutDirectoryServer(port);
    }

    int port() {
        return server.getAddress().getPort();
    }

    URI endpoint(String path) {
        return URI.create("http://127.0.0.1:" + port() + path);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            status = 405;
            body = "{\"error\":\"method\"}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
