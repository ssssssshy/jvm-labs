package ru.npi.workouttracker.integration;

public final class DirectoryUnavailableException extends IllegalStateException {
    public DirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
