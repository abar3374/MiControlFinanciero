// :core:domain — reglas de negocio y casos de uso en Kotlin puro (sin Android).
// Las pantallas y la base de datos dependen de este módulo, nunca al revés.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(projects.core.model)
    // Coroutines y Flow se agregan en la Fase 3, cuando existan los repositorios.

    testImplementation(libs.junit)
}
