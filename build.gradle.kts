plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

println("This section of code will be called in the configuration phase.")

/**
 * Demo задача для проверки работоспособности Gradle
 */
tasks.register("Demo") {
    println("This section of code will be called in the configuration phase., beacuse :configured use in build")

    doLast {
        println("Hello from Gradle Kotlin DSL!")
    }
}

/**
 * Пример 1: Простая таска для генерации данных
 * Задача ни от чего не зависит, но работает с окружением запуска
 */
val generateResources by tasks.registering {
    val dataText = "Default content"
    inputs.property("content", dataText)

    val outputFile = layout.buildDirectory.file("generated/data.txt")
    outputs.file(outputFile)

    doLast {
        val inputData = inputs.properties["content"] as String
        val file = outputFile.get().asFile
        file.writeText(inputData)
        println("Task generateResources - file created with content: $inputData")
    }
}

/**
 * Пример 2: Задача, которая читает сгенерированный файл и обрабатывает его.
 * Задача зависит от generateResources, чтобы файл точно был создан.
 * Указываем inputs.file(...) и outputs.file(...) для корректной работы Gradle.
 */
val processAllResources by tasks.registering {
    description = "Task, which reads generated file and processes it"
    group = "Generated"

    dependsOn(generateResources)

    // Файл, который мы будем читать (из предыдущей задачи)
    val inputFile = generateResources.get().outputs.files.singleFile

    // Файл, который мы сформируем
    val outputFile = layout.buildDirectory.file("processed/result.txt")

    // Сообщаем Gradle о входном и выходном файле
    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        val inFile = inputFile
        val outFile = outputFile.get().asFile

        val content = inFile.readText().uppercase()
        outFile.writeText(content)

        println("Task 'processResources' - input file: ${inFile.absolutePath}")
        println("Task 'processResources' - output file: ${outFile.absolutePath}")
    }
}

/**
 * 3) showProcessedData — выводит содержимое result.txt на экран.
 * Здесь мы используем более правильный подход: создаём наследника DefaultTask,
 * указываем входной файл через @InputFile, а логику — в @TaskAction.
 */
abstract class ShowProcessedDataTask : DefaultTask() {
    // Описываем входной файл (он должен существовать к моменту выполнения задачи)
    @get:InputFile
    abstract val processedFile: RegularFileProperty

    @TaskAction
    fun showData() {
        val inFile = processedFile.get().asFile

        println("=== Processed data ===")
        println(inFile.readText())
        println("======================")
    }
}

/**
 * Регистрируем задачу showProcessedData.
 * Она должна зависеть от processResources, чтобы файл уже был готов.
 */
val showProcessedData by tasks.registering(ShowProcessedDataTask::class) {
    dependsOn(processAllResources)

    // Указываем, что входной файл — это выходной файл из processResources
    processedFile.set(processAllResources.get().outputs.files.singleFile)
}
