package ru.npi.workouttracker.integration;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CachedWorkoutTypeDirectory {
    private final WorkoutTypeDirectoryClient client;
    private final WorkoutTypeCache cache;
    private final Clock clock;
    private final int maxAttempts;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    public CachedWorkoutTypeDirectory(
            WorkoutTypeDirectoryClient client,
            WorkoutTypeCache cache,
            Clock clock,
            int maxAttempts
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Количество попыток должно быть положительным");
        }
        this.client = client;
        this.cache = cache;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
    }

    public DirectorySnapshot synchronize(URI endpoint) {
        if (!refreshing.compareAndSet(false, true)) {
            throw new IllegalStateException("Обновление справочника уже выполняется");
        }
        try {
            DirectoryUnavailableException unavailable = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    List<WorkoutTypeReference> complete = client.fetch(endpoint);
                    Instant receivedAt = clock.instant();
                    cache.write(complete, receivedAt);
                    return new DirectorySnapshot(complete, DirectorySource.SERVER, receivedAt);
                } catch (DirectoryUnavailableException error) {
                    unavailable = error;
                }
            }
            try {
                return cache.read().orElseThrow(NoCachedDirectoryException::new);
            } catch (CacheReadException | NoCachedDirectoryException cacheError) {
                if (unavailable != null) {
                    cacheError.addSuppressed(unavailable);
                }
                throw cacheError;
            }
        } finally {
            refreshing.set(false);
        }
    }
}
