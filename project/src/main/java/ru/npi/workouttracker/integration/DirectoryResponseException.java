package ru.npi.workouttracker.integration;

public final class DirectoryResponseException extends IllegalStateException {
    public DirectoryResponseException(String message) {
        super(message);
    }

    public DirectoryResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
