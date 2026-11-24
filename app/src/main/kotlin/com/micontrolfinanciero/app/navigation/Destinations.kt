package com.micontrolfinanciero.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.micontrolfinanciero.app.R
import kotlinx.serialization.Serializable
import com.micontrolfinanciero.app.core.designsystem.R as DesignR

/*
 * Rutas de navegación tipadas: cada pantalla es un objeto, no una cadena de texto.
 * Si una ruta cambia de nombre, el compilador avisa en todos los lugares que la usan.
 */
@Serializable data object HomeDestination
@Serializable data object BudgetDestination
@Serializable data object RegisterDestination
@Serializable data object ReportsDestination
@Serializable data object ProfileDestination

/* Pantallas secundarias (sin barra inferior). id = 0 significa "nuevo". */
@Serializable data class ExpenseFormDestination(val id: Long = 0L)
@Serializable data class IncomeFormDestination(val id: Long = 0L)
@Serializable data class BudgetFormDestination(val id: Long = 0L)
@Serializable data object MovementsDestination

/** Las cinco entradas de la barra inferior, en orden. */
enum class TopLevelDestination(
    val route: Any,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    /** El botón central destacado (Registrar). */
    val isPrimaryAction: Boolean = false,
) {
    HOME(HomeDestination, DesignR.drawable.ic_nav_home, R.string.nav_home),
    BUDGET(BudgetDestination, DesignR.drawable.ic_nav_budget, R.string.nav_budget),
    REGISTER(RegisterDestination, DesignR.drawable.ic_nav_add, R.string.nav_register, isPrimaryAction = true),
    REPORTS(ReportsDestination, DesignR.drawable.ic_nav_reports, R.string.nav_reports),
    PROFILE(ProfileDestination, DesignR.drawable.ic_nav_profile, R.string.nav_profile),
}
