package ru.npi.workouttracker.integration;

public final class NoCachedDirectoryException extends IllegalStateException {
    public NoCachedDirectoryException() {
        super("Нет сохранённых данных. Повторите запрос.");
    }
}
