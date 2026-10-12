CREATE TABLE IF NOT EXISTS workouts (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL CHECK (length(trim(type)) > 0),
    workout_date TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'ATTENTION'))
);
