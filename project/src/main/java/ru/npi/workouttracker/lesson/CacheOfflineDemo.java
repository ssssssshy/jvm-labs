package ru.npi.workouttracker.lesson;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import ru.npi.workouttracker.integration.CacheReadException;
import ru.npi.workouttracker.integration.CachedWorkoutTypeDirectory;
import ru.npi.workouttracker.integration.DirectoryResponseException;
import ru.npi.workouttracker.integration.DirectorySnapshot;
import ru.npi.workouttracker.integration.DirectorySource;
import ru.npi.workouttracker.integration.NoCachedDirectoryException;
import ru.npi.workouttracker.integration.WorkoutTypeCache;
import ru.npi.workouttracker.integration.WorkoutTypeDirectoryClient;

public final class CacheOfflineDemo {
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-04T17:00:00Z");

    private CacheOfflineDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(System.getProperty("p16.directory", "build/p16"));
        Files.createDirectories(directory);
        Path cachePath = directory.resolve("workout-types.json");
        Files.deleteIfExists(cachePath);
        WorkoutTypeCache cache = new WorkoutTypeCache(cachePath);
        CachedWorkoutTypeDirectory service = service(cache);

        URI stoppedEndpoint;
        int port;
        try (LocalWorkoutDirectoryServer server = LocalWorkoutDirectoryServer.start(0)) {
            port = server.port();
            stoppedEndpoint = server.endpoint("/directory");
            DirectorySnapshot fresh = service.synchronize(stoppedEndpoint);
            verify("свежий ответ помечен как серверный", DirectorySource.SERVER, fresh.source());
            verify("получены все записи", 2, fresh.entries().size());
            verify("время ответа сохранено", RECEIVED_AT, fresh.receivedAt());

            DirectorySnapshot cached = cache.read().orElseThrow();
            verify("ответ и кэш совпадают", fresh.entries(), cached.entries());
            String goodCache = Files.readString(cachePath, StandardCharsets.UTF_8);
            expect(DirectoryResponseException.class, "повреждённый ответ явно отклонён",
                    () -> service.synchronize(server.endpoint("/broken")));
            verify("плохой ответ не перезаписал хороший кэш", goodCache,
                    Files.readString(cachePath, StandardCharsets.UTF_8));

            verifyConcurrentRefreshIsRejected(service, server.endpoint("/slow"));
        }

        DirectorySnapshot offline = service.synchronize(stoppedEndpoint);
        verify("без сети используется кэш", DirectorySource.CACHE, offline.source());
        verify("у кэша виден источник и исходное время", "кэш, получено " + RECEIVED_AT,
                offline.originLabel());

        WorkoutTypeCache emptyCache = new WorkoutTypeCache(directory.resolve("empty.json"));
        Files.deleteIfExists(emptyCache.path());
        expect(NoCachedDirectoryException.class, "первый автономный запуск объяснён",
                () -> service(emptyCache).synchronize(stoppedEndpoint));

        Path corruptCopy = directory.resolve("corrupt-copy.json");
        Files.copy(cachePath, corruptCopy, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(corruptCopy, "{not-json", StandardCharsets.UTF_8);
        expect(CacheReadException.class, "повреждение копии кэша объяснено",
                () -> new WorkoutTypeCache(corruptCopy).read());

        try (LocalWorkoutDirectoryServer restored = LocalWorkoutDirectoryServer.start(port)) {
            DirectorySnapshot restoredSnapshot = service.synchronize(restored.endpoint("/directory"));
            verify("после восстановления снова свежие данные", DirectorySource.SERVER,
                    restoredSnapshot.source());
        }
        System.out.println("Все проверки P16 пройдены.");
    }

    private static CachedWorkoutTypeDirectory service(WorkoutTypeCache cache) {
        return new CachedWorkoutTypeDirectory(
                new WorkoutTypeDirectoryClient(), cache,
                Clock.fixed(RECEIVED_AT, ZoneOffset.UTC), 2
        );
    }

    private static void verifyConcurrentRefreshIsRejected(
            CachedWorkoutTypeDirectory service,
            URI slowEndpoint
    ) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> service.synchronize(slowEndpoint));
            Thread.sleep(80);
            expect(IllegalStateException.class, "параллельное обновление не запущено",
                    () -> service.synchronize(slowEndpoint));
            try {
                first.get();
            } catch (ExecutionException error) {
                throw new AssertionError("первое обновление должно завершиться", error.getCause());
            }
        }
    }

    private static void expect(Class<? extends RuntimeException> type, String scenario, ThrowingAction action)
            throws Exception {
        try {
            action.run();
            throw new AssertionError(scenario + ": ожидался отказ");
        } catch (RuntimeException expected) {
            if (!type.isInstance(expected)) {
                throw expected;
            }
            System.out.println("[OK] " + scenario + ": " + expected.getMessage());
        }
    }

    private static void verify(String scenario, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": ожидалось " + expected + ", получено " + actual);
        }
        System.out.println("[OK] " + scenario + ": " + actual);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
