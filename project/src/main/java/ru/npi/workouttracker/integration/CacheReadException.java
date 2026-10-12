package ru.npi.workouttracker.integration;

public final class CacheReadException extends IllegalStateException {
    public CacheReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
