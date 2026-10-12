# Взаимодействие Java и Kotlin

## Граница компонента

Kotlin добавлен как небольшой форматирующий компонент. Существующие Java-модель,
репозиторий и пользовательский интерфейс не переписывались. Функция
`caption(Workout)` читает Java `record Workout` и Java `enum WorkoutStatus`.

Аннотация `@file:JvmName("WorkoutCaptions")` задаёт стабильное имя сгенерированного
класса. Поэтому Java-код вызывает функцию явно:

```java
String text = WorkoutCaptions.caption(workout);
```

Её JVM-сигнатура:

```text
public static final java.lang.String caption(ru.npi.workouttracker.domain.Workout);
```

## Null-безопасность

Параметр Kotlin объявлен как non-null `Workout`. Java-компилятор допускает передачу
`null`, но Kotlin вставляет проверку на границе и выбрасывает
`NullPointerException`. Этот контракт закреплён тестом, поэтому неоднозначного
«пустого» результата нет.

## Воспроизведение

```bash
./gradlew clean test runKotlinInteropDemo
javap -classpath build/classes/kotlin/main ru.npi.workouttracker.interop.WorkoutCaptions
```

Тест `KotlinInteropTest` проверяет результат вызова, статичность и точную сигнатуру
метода, а также поведение при Java `null`.
