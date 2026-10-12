package ru.npi.workouttracker.config;

import java.util.logging.Level;

public enum LogLevel {
    DEBUG(Level.FINE),
    INFO(Level.INFO),
    WARNING(Level.WARNING),
    ERROR(Level.SEVERE);

    private final Level julLevel;

    LogLevel(Level julLevel) {
        this.julLevel = julLevel;
    }

    public Level julLevel() {
        return julLevel;
    }
}
