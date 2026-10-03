// agp: command-line tool for plugin authors (validate, build, inspect, check targets against screen dumps).
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.proj.automation:core")
}

application {
    mainClass.set("com.proj.automation.agp.MainKt")
    applicationName = "agp"
}

// `./gradlew :agp:run --args="..."` resolves relative paths from the repository root
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
