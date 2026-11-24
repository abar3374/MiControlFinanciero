package com.micontrolfinanciero.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micontrolfinanciero.app.core.designsystem.theme.FinanceColors
import com.micontrolfinanciero.app.feature.budget.BudgetRoute
import com.micontrolfinanciero.app.feature.dashboard.DashboardRoute
import com.micontrolfinanciero.app.feature.expense.RegisterRoute
import com.micontrolfinanciero.app.feature.lock.AppLockManager
import com.micontrolfinanciero.app.feature.lock.LockRoute
import com.micontrolfinanciero.app.feature.lock.LockState
import com.micontrolfinanciero.app.feature.lock.LockStatusScreen
import com.micontrolfinanciero.app.feature.profile.ProfileRoute
import com.micontrolfinanciero.app.feature.reports.ReportsRoute

/**
 * Esqueleto de la app. Primero pasa por el bloqueo (si el usuario creó contraseña);
 * después muestra la barra inferior y el contenido de la pestaña elegida.
 */
@Composable
fun FinanceApp(appVersion: String, lockManager: AppLockManager) {
    val lockState by lockManager.state.collectAsStateWithLifecycle()
    when (val state = lockState) {
        LockState.Checking -> LockStatusScreen(message = null)
        is LockState.Failed -> LockStatusScreen(message = state.message)
        LockState.Locked -> LockRoute()
        LockState.Unlocked -> FinanceMainContent(appVersion)
    }
}

@Composable
private fun FinanceMainContent(appVersion: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = FinanceColors.White,
        bottomBar = {
            FinanceBottomBar(
                isSelected = { destination -> currentDestination.isOn(destination) },
                onNavigate = { destination -> navController.navigateToTopLevel(destination) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeDestination> {
                DashboardRoute(
                    onOpenProfile = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) },
                    onRegisterMovement = { navController.navigateToTopLevel(TopLevelDestination.REGISTER) },
                )
            }
            composable<BudgetDestination> { BudgetRoute() }
            composable<RegisterDestination> { RegisterRoute() }
            composable<ReportsDestination> { ReportsRoute() }
            composable<ProfileDestination> { ProfileRoute(appVersion = appVersion) }
        }
    }
}

/** ¿La pantalla actual pertenece a esta pestaña? */
private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/**
 * Cambia de pestaña sin apilar pantallas repetidas: al tocar Inicio varias veces
 * no se acumulan copias, y cada pestaña recuerda dónde estaba.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
