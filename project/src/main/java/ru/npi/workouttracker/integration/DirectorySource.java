package ru.npi.workouttracker.integration;

public enum DirectorySource {
    SERVER("сервер"),
    CACHE("кэш");

    private final String title;

    DirectorySource(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
