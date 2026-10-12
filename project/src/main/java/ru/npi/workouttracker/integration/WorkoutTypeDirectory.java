package ru.npi.workouttracker.integration;

import java.net.URI;
import java.util.List;

public final class WorkoutTypeDirectory {
    private final WorkoutTypeDirectoryClient client;
    private List<WorkoutTypeReference> confirmed = List.of();

    public WorkoutTypeDirectory(WorkoutTypeDirectoryClient client) {
        this.client = client;
    }

    public List<WorkoutTypeReference> refresh(URI endpoint) {
        List<WorkoutTypeReference> complete = client.fetch(endpoint);
        confirmed = complete;
        return complete;
    }

    public List<WorkoutTypeReference> confirmed() {
        return confirmed;
    }
}
