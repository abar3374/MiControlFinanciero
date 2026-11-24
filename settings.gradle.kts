// Configuración de módulos y repositorios de MiControlFinanciero (nombre visible: Finance).
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Ningún módulo puede declarar repositorios propios: todo se descarga de estas fuentes oficiales.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MiControlFinanciero"

// Permite escribir projects.core.model en vez de project(":core:model").
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Aplicación
include(":app")

// Núcleo (capas y servicios compartidos)
include(":core:model")
include(":core:domain")
include(":core:database")
include(":core:data")
include(":core:security")
include(":core:designsystem")
include(":core:notifications")
include(":core:updates")
include(":core:reports")

// Funcionalidades (una por sección de la app)
include(":feature:dashboard")
include(":feature:budget")
include(":feature:expense")
include(":feature:credit")
include(":feature:reports")
include(":feature:profile")
include(":feature:lock")
