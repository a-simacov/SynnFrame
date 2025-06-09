package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.ServerRoutes
import com.synngate.synnframe.presentation.ui.server.ServerDetailScreen
import com.synngate.synnframe.presentation.ui.server.ServerListScreen

/**
 * Создает навигационный граф для экранов серверов.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 * @param navigateToLogin Функция для навигации на экран логина
 */
fun NavGraphBuilder.serverNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager,
    navigateToLogin: () -> Unit
) {
    navigation(
        startDestination = ServerRoutes.ServerList.route,
        route = ServerRoutes.ServersGraph.route
    ) {
        // Экран списка серверов
        composable(ServerRoutes.ServerList.route) { entry ->
            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createServerListViewModel() }

            ServerListScreen(
                viewModel = viewModel,
                navigateToServerDetail = { serverId ->
                    navController.navigate(ServerRoutes.ServerDetail.createRoute(serverId))
                },
                navigateBack = { navigateToLogin() }
            )
        }

        // Экран детальной информации о сервере
        composable(
            route = ServerRoutes.ServerDetail.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val serverIdArg = entry.arguments?.getString("serverId")
            val serverId = serverIdArg?.toIntOrNull()

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(serverId) {
                screenContainer.createServerDetailViewModel(serverId)
            }

            ServerDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}