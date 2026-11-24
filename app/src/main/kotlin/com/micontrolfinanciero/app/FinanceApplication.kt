package com.micontrolfinanciero.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de arranque de la app. @HiltAndroidApp prepara la inyección de dependencias
 * para todos los módulos.
 *
 * Aquí se inicializarán más adelante: canales de notificación (Fase 6) y la
 * reprogramación de recordatorios.
 */
@HiltAndroidApp
class FinanceApplication : Application()
