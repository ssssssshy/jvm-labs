ALTER TABLE workouts ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 1
    CHECK (duration_minutes > 0);
