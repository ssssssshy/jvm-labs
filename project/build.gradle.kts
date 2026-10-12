plugins {
    application
    java
    kotlin("jvm") version "2.4.20"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

javafx {
    version = "25"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClass = "ru.npi.workouttracker.ui.DesktopApp"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics,ALL-UNNAMED")
}

tasks.jar {
    archiveBaseName = "workout-tracker"
    manifest {
        attributes["Main-Class"] = "ru.npi.workouttracker.lesson.ServiceLoaderDemo"
    }
}

tasks.named<JavaExec>("run") {
    systemProperty(
        "workout.config",
        System.getProperty("workout.config", "config/application.properties")
    )
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("prism.order", "sw")
    reports {
        html.required = true
        junitXml.required = true
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("runBasicsDemo") {
    group = "application"
    description = "Запускает демонстрацию массивов, условий, циклов и методов для P02"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.BasicsDemo"
}

tasks.register<JavaExec>("runIndexFailure") {
    group = "verification"
    description = "Воспроизводит контролируемый выход за границы массива для P02"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.BasicsDemo"
    args("--show-index-error")
}

tasks.register<JavaExec>("runDomainDemo") {
    group = "application"
    description = "Запускает демонстрацию предметной модели и инвариантов для P03"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.DomainDemo"
}

tasks.register<JavaExec>("runCountMapDemo") {
    group = "application"
    description = "Запускает демонстрацию коллекций и CountMap для P04"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.CountMapDemo"
}

tasks.register<JavaExec>("runP05Check") {
    group = "verification"
    description = "Проверяет сценарии JavaFX и сохраняет снимок P05"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.ui.P05CheckLauncher"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p05.screenshot", "../labs/P05/screenshots/success.png")
}

tasks.register<JavaExec>("runP06Check") {
    group = "verification"
    description = "Проверяет ObservableList, привязки, удаление и замену для P06"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.ui.P06CheckLauncher"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p06.screenshot", "../labs/P06/screenshots/bindings.png")
}

tasks.register<JavaExec>("runP07Check") {
    group = "verification"
    description = "Проверяет FXML, CSS, контроллер и ошибку fx:id для P07"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.ui.P07CheckLauncher"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p07.screenshot", "../labs/P07/screenshots/fxml-form.png")
}

tasks.register<JavaExec>("runP08Check") {
    group = "verification"
    description = "Проверяет валидацию, повторное сохранение и доступность P08"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.ui.P08CheckLauncher"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p08.screenshot", "../labs/P08/screenshots/validation.png")
}

tasks.register<JavaExec>("runP09ServiceTest") {
    group = "verification"
    description = "Проверяет сервис и репозиторий P09 без запуска JavaFX"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.application.WorkoutServiceTest"
}

tasks.register<JavaExec>("runReflectionDemo") {
    group = "verification"
    description = "Проверяет аннотации, reflection и Proxy для P10"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.ReflectionDemo"
}

val runP11First by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Первый запуск SQLite-проверки P11"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.SqlitePersistenceDemo"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p11.mode", "first")
    systemProperty("p11.database", "build/p11/workouts.db")
}

tasks.register<JavaExec>("runP11Second") {
    group = "verification"
    description = "Второй запуск SQLite-проверки P11"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.SqlitePersistenceDemo"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p11.mode", "second")
    systemProperty("p11.database", "build/p11/workouts.db")
    mustRunAfter(runP11First)
}

tasks.register<JavaExec>("runP12Check") {
    group = "verification"
    description = "Проверяет транзакционный CSV-импорт и миграции P12"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.TransactionMigrationDemo"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p12.database", "build/p12/workouts.db")
    systemProperty("p12.validCsv", "samples/p12-valid.csv")
    systemProperty("p12.duplicateCsv", "samples/p12-duplicate.csv")
}

tasks.register<JavaExec>("runP13Check") {
    group = "verification"
    description = "Проверяет JavaFX Task, прогресс, отмену, успех и отказ P13"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.ui.P13CheckLauncher"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("p13.screenshot", "../labs/P13/screenshots/background-task.png")
}

tasks.register<JavaExec>("runConcurrencyDemo") {
    group = "verification"
    description = "Проверяет гонку, AtomicInteger, Future, отмену и virtual threads P14"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.ConcurrencyDemo"
}

tasks.register<JavaExec>("runHttpJsonDemo") {
    group = "verification"
    description = "Проверяет локальный HTTP, JSON, статусы и освобождение порта P15"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.HttpJsonDemo"
}

tasks.register<JavaExec>("runCacheOfflineDemo") {
    group = "verification"
    description = "Проверяет кэш, повторный запрос и автономный режим P16"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.CacheOfflineDemo"
    systemProperty("p16.directory", "build/p16")
}

tasks.register<JavaExec>("runServiceLoaderJar") {
    group = "verification"
    description = "Запускает ServiceLoader-пример P21 только из собранного JAR"
    dependsOn(tasks.jar)
    classpath = files(tasks.jar.flatMap { it.archiveFile })
    mainClass = "ru.npi.workouttracker.lesson.ServiceLoaderDemo"
}

tasks.register<JavaExec>("runKotlinInteropDemo") {
    group = "verification"
    description = "Проверяет вызов Kotlin из Java и Java-модели из Kotlin P23"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.npi.workouttracker.lesson.KotlinInteropDemo"
}

val prepareJpackageInput by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Собирает JAR и runtime-зависимости для jpackage"
    dependsOn(tasks.jar)
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("jpackage-input"))
}

tasks.register<Exec>("packageAppImage") {
    group = "distribution"
    description = "Создаёт переносимый Linux app-image P22"
    dependsOn(prepareJpackageInput)
    doFirst {
        val destination = layout.buildDirectory.dir("package").get().asFile
        delete(destination)
        commandLine(
            "jpackage",
            "--type", "app-image",
            "--name", "WorkoutTracker",
            "--input", layout.buildDirectory.dir("jpackage-input").get().asFile.absolutePath,
            "--main-jar", tasks.jar.get().archiveFileName.get(),
            "--main-class", "ru.npi.workouttracker.ui.DesktopLauncher",
            "--dest", destination.absolutePath,
            "--runtime-image", System.getProperty("java.home"),
            "--app-version", "1.0.0",
            "--vendor", "Georgiy Petrosyan",
            "--description", "Personal workout journal",
            "--java-options", "--enable-native-access=ALL-UNNAMED"
        )
    }
}
