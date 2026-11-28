package com.micontrolfinanciero.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceTheme
import com.micontrolfinanciero.app.core.notifications.ReminderNotifications
import com.micontrolfinanciero.app.feature.lock.AppLockManager
import com.micontrolfinanciero.app.navigation.FinanceApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Pantalla pedida desde una notificación ("payments"); se abre después del bloqueo. */
    private val pendingOpen = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readOpenExtra(intent)
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        setContent {
            FinanceTheme {
                FinanceApp(
                    appVersion = appVersion,
                    lockManager = lockManager,
                    pendingOpen = pendingOpen,
                    onOpenHandled = { pendingOpen.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readOpenExtra(intent)
    }

    private fun readOpenExtra(intent: Intent?) {
        intent?.getStringExtra(ReminderNotifications.EXTRA_OPEN)?.let { pendingOpen.value = it }
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
