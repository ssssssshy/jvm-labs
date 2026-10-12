# Локальное хранилище SQLite

## Соответствие полей

| `Workout` | SQLite | Преобразование |
|---|---|---|
| `id` | `id INTEGER PRIMARY KEY` | `long` ↔ `INTEGER` |
| `code` | `code TEXT NOT NULL UNIQUE` | `String` ↔ `TEXT` |
| `type` | `type TEXT NOT NULL` | `String` ↔ `TEXT` |
| `date` | `workout_date TEXT NOT NULL` | `LocalDate.toString()` и `LocalDate.parse()` |
| `status` | `status TEXT NOT NULL` | `enum.name()` и `WorkoutStatus.valueOf()` |

SQLite не имеет отдельного типа `LocalDate`, поэтому дата хранится как ISO-текст
`YYYY-MM-DD`. Такой формат однозначно восстанавлиается в `LocalDate`.

## Границы JDBC

`SqliteWorkoutRepository` открывает `Connection` на время одной операции. Каждый
`Connection`, `PreparedStatement` и `ResultSet` ограничен try-with-resources. Все значения передаются
методами `setLong` и `setString`. Поэтому апостроф в типе тренировки считается частью
значения, а не SQL-синтаксиса.

Параметры защищают и учебную строку: привычка склеивать текст с SQL легко переносится
в реальный код и создаёт SQL injection или ошибки экранирования. Параметры подставляют только
значения; имена таблиц и столбцов заданы в исходном SQL.

Нарушения первичного ключа или `UNIQUE(code)` преобразуются в `DuplicateWorkoutException`, чтобы
вышестоящий слой не зависел от текста ошибки SQLite.
