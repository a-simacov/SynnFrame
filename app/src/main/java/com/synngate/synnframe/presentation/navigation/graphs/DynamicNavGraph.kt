package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.DynamicRoutes
import com.synngate.synnframe.presentation.navigation.routes.TaskXRoutes
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.DynamicCustomListScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.DynamicMenuScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductDetailScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductsScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTaskDetailScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTasksScreen
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Base64

/**
 * Создает навигационный граф для динамических экранов приложения.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.dynamicNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = DynamicRoutes.DynamicMenu.route,
        route = DynamicRoutes.DynamicNavGraph.route
    ) {
        // Экран динамического меню
        composable(DynamicRoutes.DynamicMenu.route) { entry ->
            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createDynamicMenuViewModel() }

            DynamicMenuScreen(
                viewModel = viewModel,
                navigateToDynamicTasks = { menuItemId, menuItemName, endpoint, screenSettings ->
                    navController.navigate(
                        DynamicRoutes.DynamicTasks.createRoute(
                            menuItemId = menuItemId,
                            menuItemName = menuItemName,
                            endpoint = endpoint,
                            screenSettings = screenSettings
                        )
                    )
                },
                navigateToDynamicProducts = { menuItemId, menuItemName, endpoint, screenSettings ->
                    navController.navigate(
                        DynamicRoutes.DynamicProducts.createRoute(
                            menuItemId = menuItemId,
                            menuItemName = menuItemName,
                            endpoint = endpoint,
                            screenSettings = screenSettings
                        )
                    )
                },
                navigateToCustomList = { menuItemId, menuItemName, endpoint, screenSettings ->
                    navController.navigate(
                        DynamicRoutes.CustomList.createRoute(
                            menuItemId = menuItemId,
                            menuItemName = menuItemName,
                            endpoint = endpoint,
                            screenSettings = screenSettings
                        )
                    )
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детального просмотра динамической задачи
        composable(
            route = DynamicRoutes.DynamicTaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                },
                navArgument("endpoint") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val encodedEndpoint = entry.arguments?.getString("endpoint") ?: ""
            val endpoint = try {
                String(Base64.getDecoder().decode(encodedEndpoint))
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode endpoint from Base64, using as is")
                encodedEndpoint
            }

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(taskId, endpoint) {
                screenContainer.createDynamicTaskDetailViewModel(
                    taskId = taskId,
                    endpoint = endpoint
                )
            }

            DynamicTaskDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToTaskXDetail = { taskIdNav, endpointNav ->
                    // Передаем параметры в новый маршрут
                    navController.navigate(TaskXRoutes.TaskXDetail.createRoute(taskIdNav, endpointNav)) {
                        // При переходе к экрану выполнения задания закрываем экран деталей
                        popUpTo(DynamicRoutes.DynamicTaskDetail.route) { inclusive = true }
                    }
                }
            )
        }

        // Экран списка динамических задач
        composable(
            route = DynamicRoutes.DynamicTasks.route,
            arguments = listOf(
                navArgument("menuItemId") {
                    type = NavType.StringType
                },
                navArgument("menuItemName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("endpoint") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("screenSettings") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val menuItemId = entry.arguments?.getString("menuItemId") ?: ""
            val encodedMenuItemName = entry.arguments?.getString("menuItemName") ?: ""
            val menuItemName = java.net.URLDecoder.decode(encodedMenuItemName, "UTF-8")

            // Декодируем endpoint из Base64
            val encodedEndpoint = entry.arguments?.getString("endpoint") ?: ""
            val endpoint = try {
                String(Base64.getDecoder().decode(encodedEndpoint))
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode endpoint from Base64, using as is")
                encodedEndpoint
            }

            val encodedScreenSettings = entry.arguments?.getString("screenSettings")

            // Декодирование screenSettings из JSON, если они переданы
            val screenSettings = if (encodedScreenSettings != null) {
                try {
                    val json = java.net.URLDecoder.decode(encodedScreenSettings, "UTF-8")
                    Json.decodeFromString<ScreenSettings>(json)
                } catch (e: Exception) {
                    ScreenSettings() // Используем значение по умолчанию в случае ошибки
                }
            } else {
                ScreenSettings() // Значение по умолчанию, если настройки не переданы
            }

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )

            val viewModel = remember(menuItemId, menuItemName, endpoint, screenSettings) {
                screenContainer.createDynamicTasksViewModel(
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    endpoint = endpoint,
                    screenSettings = screenSettings
                )
            }

            DynamicTasksScreen(
                viewModel = viewModel,
                navigateToTaskDetail = { taskId, taskEndpoint ->
                    navController.navigate(
                        DynamicRoutes.DynamicTaskDetail.createRoute(
                            taskId = taskId,
                            endpoint = taskEndpoint
                        )
                    )
                },
                navigateToTaskXDetail = { taskId, taskEndpoint ->
                    navController.navigate(TaskXRoutes.TaskXDetail.createRoute(taskId, taskEndpoint))
                },
                navigateBack = {
                    navController.popBackStack()
                },
                screenContainer = screenContainer
            )
        }

        // Экран списка динамических продуктов
        composable(
            route = DynamicRoutes.DynamicProducts.route,
            arguments = listOf(
                navArgument("menuItemId") {
                    type = NavType.StringType
                },
                navArgument("menuItemName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("endpoint") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("screenSettings") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val menuItemId = entry.arguments?.getString("menuItemId") ?: ""
            val encodedMenuItemName = entry.arguments?.getString("menuItemName") ?: ""
            val menuItemName = java.net.URLDecoder.decode(encodedMenuItemName, "UTF-8")

            // Декодируем endpoint из Base64
            val encodedEndpoint = entry.arguments?.getString("endpoint") ?: ""
            val endpoint = try {
                String(Base64.getDecoder().decode(encodedEndpoint))
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode endpoint from Base64, using as is")
                encodedEndpoint
            }

            val encodedScreenSettings = entry.arguments?.getString("screenSettings")

            // Декодирование screenSettings из JSON, если они переданы
            val screenSettings = if (encodedScreenSettings != null) {
                try {
                    val json = java.net.URLDecoder.decode(encodedScreenSettings, "UTF-8")
                    Json.decodeFromString<ScreenSettings>(json)
                } catch (e: Exception) {
                    ScreenSettings() // Используем значение по умолчанию в случае ошибки
                }
            } else {
                ScreenSettings() // Значение по умолчанию, если настройки не переданы
            }

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(menuItemId, menuItemName, endpoint, screenSettings) {
                screenContainer.createDynamicProductsViewModel(
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    endpoint = endpoint,
                    screenSettings = screenSettings
                )
            }

            DynamicProductsScreen(
                viewModel = viewModel,
                navigateToProductDetail = { product ->
                    navController.navigate(DynamicRoutes.DynamicProductDetail.createRoute(product))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о динамическом продукте
        composable(
            route = DynamicRoutes.DynamicProductDetail.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                },
                navArgument("productName") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { entry ->
            val productId = entry.arguments?.getString("productId") ?: ""
            val encodedProductName = entry.arguments?.getString("productName") ?: ""
            val productName = java.net.URLDecoder.decode(encodedProductName, "UTF-8")

            // Создаем временный объект DynamicProduct, который будет заполнен из ViewModel
            val product = DynamicProduct(
                id = productId,
                name = productName,
                accountingModel = "QTY",  // Значение по умолчанию, которое будет заменено
                articleNumber = "",       // Будет заполнено в ViewModel
                mainUnitId = "",          // Будет заполнено в ViewModel
                units = emptyList()       // Будет заполнено в ViewModel
            )

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(productId, productName) {
                screenContainer.createDynamicProductDetailViewModel(product)
            }

            DynamicProductDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран списка пользовательских элементов
        composable(
            route = DynamicRoutes.CustomList.route,
            arguments = listOf(
                navArgument("menuItemId") {
                    type = NavType.StringType
                },
                navArgument("menuItemName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("endpoint") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("screenSettings") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val menuItemId = entry.arguments?.getString("menuItemId") ?: ""
            val encodedMenuItemName = entry.arguments?.getString("menuItemName") ?: ""
            val menuItemName = java.net.URLDecoder.decode(encodedMenuItemName, "UTF-8")

            // Декодируем endpoint из Base64
            val encodedEndpoint = entry.arguments?.getString("endpoint") ?: ""
            val endpoint = try {
                String(Base64.getDecoder().decode(encodedEndpoint))
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode endpoint from Base64, using as is")
                encodedEndpoint
            }

            val encodedScreenSettings = entry.arguments?.getString("screenSettings")

            // Декодирование screenSettings из JSON, если они переданы
            val screenSettings = if (encodedScreenSettings != null) {
                try {
                    val json = java.net.URLDecoder.decode(encodedScreenSettings, "UTF-8")
                    Json.decodeFromString<ScreenSettings>(json)
                } catch (e: Exception) {
                    ScreenSettings() // Используем значение по умолчанию в случае ошибки
                }
            } else {
                ScreenSettings() // Значение по умолчанию, если настройки не переданы
            }

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )

            val viewModel = remember(menuItemId, menuItemName, endpoint, screenSettings) {
                screenContainer.createDynamicCustomListViewModel(
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    endpoint = endpoint,
                    screenSettings = screenSettings
                )
            }

            DynamicCustomListScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                screenContainer = screenContainer
            )
        }
    }
}