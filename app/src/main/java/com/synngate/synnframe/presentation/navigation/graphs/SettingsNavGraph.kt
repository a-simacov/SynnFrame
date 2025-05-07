package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.SettingsRoutes
import com.synngate.synnframe.presentation.ui.settings.SettingsScreen
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryScreen

/**
 * Создает навигационный граф для экранов настроек.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = SettingsRoutes.Settings.route,
        route = SettingsRoutes.SettingsGraph.route
    ) {
        // Экран настроек
        composable(SettingsRoutes.Settings.route) { entry ->
            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createSettingsViewModel() }

            SettingsScreen(
                viewModel = viewModel,
                navigateToServerList = {
                    navController.navigate(com.synngate.synnframe.presentation.navigation.routes.ServerRoutes.ServerList.route) {
                        popUpTo(SettingsRoutes.Settings.route) { inclusive = false }
                    }
                },
                navigateToSyncHistory = {
                    navController.navigate(SettingsRoutes.SyncHistory.route)
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран истории синхронизации
        composable(SettingsRoutes.SyncHistory.route) { entry ->
            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createSyncHistoryViewModel() }

            SyncHistoryScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}