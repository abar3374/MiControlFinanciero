// :app — la aplicación instalable. Une todos los módulos, arranca Hilt y define la navegación.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.micontrolfinanciero.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.micontrolfinanciero.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Regla de versiones: versionCode SIEMPRE sube en cada publicación (el actualizador
        // rechaza versiones menores o iguales). versionName sigue MAYOR.MENOR.PARCHE.
        versionCode = 9
        versionName = "0.9.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma de RELEASE: GitHub Actions descifra signing/finance-release.jks.enc y entrega
    // la ruta y la contraseña por variables de entorno. En un computador sin esas variables,
    // la versión RELEASE queda sin firmar (y la DEBUG usa la clave de depuración normal).
    val releaseKeystore = System.getenv("FINANCE_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("FINANCE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FINANCE_KEY_ALIAS") ?: "finance"
                keyPassword = System.getenv("FINANCE_KEYSTORE_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // La versión de desarrollo se instala junto a la real sin reemplazarla.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Finance Dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            resValue("string", "app_name", "Finance")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    // Módulos del proyecto
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.notifications)
    implementation(projects.feature.lock)
    implementation(projects.feature.dashboard)
    implementation(projects.feature.budget)
    implementation(projects.feature.credit)
    implementation(projects.feature.expense)
    implementation(projects.feature.reports)
    implementation(projects.feature.profile)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // FragmentActivity: la ventana de huella de Android (BiometricPrompt) la necesita.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Inyección de dependencias (y en las tareas de WorkManager)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
