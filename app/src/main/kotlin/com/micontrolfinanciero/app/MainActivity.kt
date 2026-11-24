package com.micontrolfinanciero.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.navigation.FinanceApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única actividad de la app. A propósito no tiene lógica: solo aplica el tema y
 * entrega el control a la navegación (FinanceApp). Cada pantalla vive en su módulo.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        setContent {
            FinanceTheme {
                FinanceApp(appVersion = appVersion)
            }
        }
    }
}
