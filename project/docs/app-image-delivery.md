# Поставка Linux app-image

Gradle-задача `packageAppImage` собирает все runtime-зависимости и вызывает
`jpackage --type app-image`. Результат содержит launcher, приложение и собственную
среду Java, поэтому пользователю не нужны Gradle, IDE или установленная JDK.

## Сборка и проверка

```bash
./gradlew clean test packageAppImage
WORKOUT_HOME=/tmp/workout-clean \
  build/package/WorkoutTracker/bin/WorkoutTracker --smoke-test
PATH=/nonexistent WORKOUT_HOME=/tmp/workout-clean \
  build/package/WorkoutTracker/bin/WorkoutTracker --smoke-test
```

`--smoke-test` запускает настоящий JavaFX lifecycle, загружает FXML и CSS из JAR,
открывает SQLite, печатает проверяемые пути и закрывает окно автоматически.

## Пользовательские данные

По умолчанию изменяемые файлы находятся в `${user.home}/.workout-tracker`:

- `config/application.properties` — внешние настройки;
- `data/workouts.db` — SQLite;
- `logs/` — диагностический журнал.

Для изолированной проверки корень меняется переменной `WORKOUT_HOME`. Каталог
app-image не содержит рабочую базу, пользовательский конфиг, журналы или секреты.
Удаление каталога приложения не должно удалять пользовательский каталог.

## Паспорт образа

| Поле | Значение |
|---|---|
| Имя | WorkoutTracker |
| Версия | 1.0.0 |
| Платформа | Linux x64 |
| JDK/jpackage | OpenJDK 25.0.4.1 |
| Точка входа | `ru.npi.workouttracker.ui.DesktopLauncher` |
| Размер app-image | 277 MiB |
| Архив | `WorkoutTracker-linux-x64-1.0.0.tar.gz`, 112 MiB |
| SHA-256 архива | `474a9444227075f27d9ea27c97997ffa08f188f078fada545c4126f9802090a9` |

Системный Red Hat OpenJDK содержит изменённый `java.security`, из-за чего его
`jlink` отказался создавать сокращённую среду. Поэтому `jpackage` получил текущую
JDK через `--runtime-image`. Образ крупнее сокращённого, но автономность проверена.

Полноценная проверка на отдельной чистой виртуальной машине в этой практике не
выполнялась. Локально подтверждены запуск launcher без Java в `PATH`, отдельный
пустой пользовательский каталог, первый и повторный запуск.
