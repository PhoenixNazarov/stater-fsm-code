plugins {
    kotlin("jvm") version "2.0.20"
    id("jacoco")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // Запускать после тестов
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
kotlin {
    jvmToolchain(20)
}