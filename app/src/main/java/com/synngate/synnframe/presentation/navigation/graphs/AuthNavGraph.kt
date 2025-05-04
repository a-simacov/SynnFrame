package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.MainRoutes
import com.synngate.synnframe.presentation.navigation.routes.ServerRoutes
import com.synngate.synnframe.presentation.ui.login.LoginScreen

/**
 * Создает навигационные маршруты для аутентификации.
 * (Не является полноценным графом, так как содержит только один экран)
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 * @param exitApp Функция для выхода из приложения
 */
fun NavGraphBuilder.authRoutes(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager,
    exitApp: () -> Unit
) {
    composable(AuthRoutes.Login.route) { entry ->
        val screenContainer = rememberEphemeralScreenContainer(
            navController = navController,
            navBackStackEntry = entry,
            navigationScopeManager = navigationScopeManager
        )
        val viewModel = remember { screenContainer.createLoginViewModel() }

        LoginScreen(
            viewModel = viewModel,
            navigateToMainMenu = {
                navController.navigate(MainRoutes.MainMenu.route) {
                    popUpTo(AuthRoutes.Login.route) { inclusive = true }
                }
            },
            navigateToServersList = {
                navController.navigate(ServerRoutes.ServerList.route) {
                    popUpTo(AuthRoutes.Login.route) { inclusive = false }
                }
            },
            exitApp = exitApp
        )
    }
}