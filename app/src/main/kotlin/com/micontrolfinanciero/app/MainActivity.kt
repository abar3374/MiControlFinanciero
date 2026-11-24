package com.micontrolfinanciero.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.feature.lock.AppLockManager
import com.micontrolfinanciero.app.navigation.FinanceApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Única actividad de la app. Aplica el tema, entrega el control a la navegación
 * (FinanceApp) y avisa al bloqueo cuándo la app sale y vuelve de segundo plano.
 *
 * Es FragmentActivity (y no ComponentActivity) porque la ventana de huella de Android
 * la necesita.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var lockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        setContent {
            FinanceTheme {
                FinanceApp(appVersion = appVersion, lockManager = lockManager)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lockManager.onAppForegrounded(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        super.onStop()
        // Si la pantalla solo gira, no cuenta como salir de la app.
        if (!isChangingConfigurations) lockManager.onAppBackgrounded(SystemClock.elapsedRealtime())
    }
}
