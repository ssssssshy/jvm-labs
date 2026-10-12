# Трекер тренировок

Учебное настольное приложение для пользователя, который ведёт личный журнал
тренировок. Проект помогает хранить историю занятий в одном месте и считать
суммарное время тренировок за выбранный период.

## Паспорт проекта

- Тема: трекер тренировок.
- Основной пользователь: человек, который регулярно тренируется и ведёт журнал.
- Проблема пользователя: записи о занятиях разрознены, поэтому трудно восстановить
  историю и оценить общий объём нагрузки.
- Главный объект: тренировка (`Workout`).
- Жизненный цикл объекта: создание, просмотр, изменение и удаление.
- Основной процесс: пользователь фиксирует тренировку и позднее анализирует историю.
- Ценность первой версии: единая локальная история с проверкой обязательных полей.
- Контекст применения: персональный компьютер, работа без обязательного подключения к сети.

## Сценарии первой версии

1. Добавить тренировку.
2. Изменить запись о тренировке.
3. Удалить запись.
4. Просмотреть и отфильтровать историю тренировок.
5. Рассчитать суммарное время за выбранный период.

Подробности находятся в [кратком техническом задании](docs/mini_tz.md),
[описании проблемы](docs/brief.md), [сравнении аналогов](docs/analogs.md) и
[контекстной схеме](docs/architecture/C4_контекст.md).

## Требования к среде

- JDK 25, включающий команды `java` и `javac`;
- доступ в Интернет при первом запуске Gradle Wrapper.

Отдельно устанавливать Gradle не нужно: оболочка находится в проекте.

## Запуск

Из каталога `JVM/petrosyan/project`:

```bash
java -version
javac -version
./gradlew run
```

Точка входа: `ru.npi.workouttracker.HelloWorld`.

## Проверка контролируемого отказа

После сборки намеренно указывается несуществующий класс:

```bash
java -cp build/classes/java/main ru.npi.workouttracker.MissingClass
```

Команда должна завершиться ошибкой поиска главного класса. Исправленный запуск:

```bash
java -cp build/classes/java/main ru.npi.workouttracker.HelloWorld
```

Протокол команд сохраняется в каталоге `evidence/`.

## Практика P02

Расчёт пропущенных запланированных тренировок и машинные проверки граничных
значений запускаются командой:

```bash
./gradlew runBasicsDemo
```

Контролируемый выход за границы массива:

```bash
./gradlew runIndexFailure
```

Правило расчёта: тренировка пропущена, если её плановая дата строго раньше
контрольной. Равная контрольной дата не считается пропущенной.

## Практика P03

Предметная модель тренировки и проверки её допустимых состояний запускаются командой:

```bash
./gradlew runDomainDemo
```

Описание полей, состояний и диаграмма находятся в
[`docs/domain-model.md`](docs/domain-model.md).

## Практика P04

Типобезопасный счётчик повторений типов тренировок запускается командой:

```bash
./gradlew runCountMapDemo
```

Контракты методов приведены в
[`docs/count-map-contract.md`](docs/count-map-contract.md).

## Практика P05

Первое окно JavaFX запускается командой:

```bash
./gradlew run
```

Автоматическая проверка добавления, пустого ввода, изменения размера и создание
снимка выполняются командой:

```bash
./gradlew runP05Check
```

Граф сцены описан в [`docs/scene-graph.md`](docs/scene-graph.md). Добавленные записи
хранятся только в памяти и после закрытия приложения исчезают.

## Практика P06

Проверка наблюдаемого списка, связанных свойств, удаления и замены записи:

```bash
./gradlew runP06Check
```

Принятые решения описаны в
[`docs/observable-data.md`](docs/observable-data.md).

## Практика P07

Основной интерфейс хранится в FXML, оформление — в CSS, обработчики — в
`MainController`. Проверка ресурсов и связи `fx:id`:

```bash
./gradlew clean runP07Check
```

Таблица соответствия элементов приведена в
[`docs/fxml-mapping.md`](docs/fxml-mapping.md).

## Практика P08

Проверка обязательных полей, повторного сохранения и доступности:

```bash
./gradlew runP08Check
```

Матрица ввода, порядок фокуса и граница между формой и доменной моделью
описаны в [`docs/input-validation.md`](docs/input-validation.md).

## Практика P09

Прикладные сценарии вынесены в `WorkoutService`, а доступ к записям описан
интерфейсом `WorkoutRepository`. Проверка без запуска JavaFX:

```bash
./gradlew clean runP09ServiceTest
```

Схема зависимостей приведена в
[`docs/architecture/C4_компоненты.md`](docs/architecture/C4_%D0%BA%D0%BE%D0%BC%D0%BF%D0%BE%D0%BD%D0%B5%D0%BD%D1%82%D1%8B.md).

## Практика P10

Аннотации описывают публичные операции сервиса, а динамический Proxy считает их вызовы.
Воспроизводимая проверка:

```bash
./gradlew clean runReflectionDemo
```

Границы reflection и посредника описаны в
[`docs/reflection-and-proxy.md`](docs/reflection-and-proxy.md).

## Практика P11

Основное приложение хранит тренировки в `data/workouts.db`. SQLite работает через JDBC-драйвер,
который Gradle загружает автоматически. Проверка двух запусков:

```bash
./gradlew clean runP11First runP11Second
```

Схема и границы JDBC описаны в
[`docs/sqlite-storage.md`](docs/sqlite-storage.md).

## Практика P12

Атомарный импорт двух CSV-наборов, миграция старой базы до версии 2
и повторный запуск мигратора проверяются командой:

```bash
./gradlew clean runP12Check
```

Формат CSV: `code;type;date`, одна тренировка на строку, дата в виде `YYYY-MM-DD`.
Учебный разбор не поддерживает кавычки, многострочные поля и `;` внутри значения.
Механизм описан в [`docs/transactions-and-migrations.md`](docs/transactions-and-migrations.md).

## Практика P13

Проверка целостности истории тренировок выполняется в JavaFX `Task`. Окно остаётся
отзывчивым, интерфейс показывает прогресс, а отмена не публикует неполный результат.

```bash
./gradlew clean runP13Check
```

Схема потоков и правила публикации описаны в
[`docs/background-task.md`](docs/background-task.md).

## Практика P14

Воспроизводимая гонка, исправление через `AtomicInteger`, ожидание `Future`, отмена и
закрытие исполнителя:

```bash
./gradlew clean runConcurrencyDemo
```

Границы параллелизма описаны в [`docs/concurrency.md`](docs/concurrency.md).

## Практика P15

Локальный HTTP-сервер и клиент JSON-справочника типов тренировок:

```bash
./gradlew clean runHttpJsonDemo
```

Контракт описан в [`docs/http-json.md`](docs/http-json.md).

## Практика P16

Проверка атомарного кэша, двух сетевых попыток и автономного режима:

```bash
./gradlew clean runCacheOfflineDemo
```

Матрица состояний и правила публикации описаны в
[`docs/cache-offline.md`](docs/cache-offline.md).

## Практика P17

JUnit 5 проверяет предметные правила, сервис, временную SQLite, локальный HTTP и
короткий JavaFX-маршрут без фиксированных задержек:

```bash
./gradlew clean test
./gradlew clean test
```

Связь рисков и уровней проверки приведена в
[`docs/test-risk-matrix.md`](docs/test-risk-matrix.md).

## Практика P18

Регрессионный набор отдельно проверяет SQLite, ресурсы FXML/CSS и сквозной маршрут
от ввода в форме до повторного чтения записи из временной базы:

```bash
./gradlew clean test
```

Границы сценария описаны в
[`docs/integration-ui-tests.md`](docs/integration-ui-tests.md).

## Практика P19

Параметры среды находятся в `config/application.properties`; другой файл можно
указать без перекомпиляции:

```bash
./gradlew run -Dworkout.config=/path/to/application.properties
./gradlew clean test
```

Формат настроек и политика безопасного журнала описаны в
[`docs/configuration-diagnostics.md`](docs/configuration-diagnostics.md).

## Практика P20

Согласованный SQLite-backup, проверка целостности и восстановление в отдельную
тестовую базу проверяются командой:

```bash
./gradlew clean test
```

Критерии готовности копии и памятка восстановления приведены в
[`docs/backup-restore.md`](docs/backup-restore.md).

## Практика P21

Сборка исполняемого JAR и запуск двух расширений через `ServiceLoader`:

```bash
./gradlew clean test jar runServiceLoaderJar
java -jar build/libs/workout-tracker.jar
```

Состав архива и контракт расширений описаны в
[`docs/jar-service-loader.md`](docs/jar-service-loader.md).

## Практика P22

Linux app-image со встроенной средой выполнения создаётся командой:

```bash
./gradlew clean test packageAppImage
WORKOUT_HOME=/tmp/workout-clean build/package/WorkoutTracker/bin/WorkoutTracker --smoke-test
```

Паспорт образа, пользовательские каталоги и ограничения проверки описаны в
[`docs/app-image-delivery.md`](docs/app-image-delivery.md).

## Практика P23

Java/Kotlin-взаимодействие и проверка стабильной JVM-сигнатуры:

```bash
./gradlew clean test runKotlinInteropDemo
javap -classpath build/classes/kotlin/main ru.npi.workouttracker.interop.WorkoutCaptions
```

Граница компонента и null-контракт описаны в
[`docs/java-kotlin-interop.md`](docs/java-kotlin-interop.md).

## Выпуск 1.0.0 и практика P24

Полная приёмка, упаковка и автономный сценарий:

```bash
./gradlew clean test packageAppImage
./gradlew runCacheOfflineDemo
WORKOUT_HOME=/tmp/workout-check \
  build/package/WorkoutTracker/bin/WorkoutTracker --smoke-test
```

Результаты и ограничения находятся в
[`docs/release-passport-1.0.0.md`](docs/release-passport-1.0.0.md), а запуск
переданного комплекта — в [`docs/handoff-1.0.0.md`](docs/handoff-1.0.0.md).
