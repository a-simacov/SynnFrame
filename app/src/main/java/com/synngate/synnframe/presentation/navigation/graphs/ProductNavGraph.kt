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
import com.synngate.synnframe.presentation.navigation.rememberPersistentScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.ProductRoutes
import com.synngate.synnframe.presentation.ui.products.ProductDetailScreen
import com.synngate.synnframe.presentation.ui.products.ProductListScreen

/**
 * Создает навигационный граф для экранов продуктов.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.productsNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = ProductRoutes.ProductList.route,
        route = ProductRoutes.ProductsGraph.route
    ) {
        // Экран списка продуктов - используем ПОСТОЯННЫЙ контейнер
        composable(
            route = ProductRoutes.ProductList.route,
            arguments = listOf(
                navArgument("isSelectionMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val isSelectionMode = entry.arguments?.getBoolean("isSelectionMode") ?: false
            val screenContainer = rememberPersistentScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(isSelectionMode) {
                screenContainer.createProductListViewModel(isSelectionMode)
            }

            ProductListScreen(
                viewModel = viewModel,
                navigateToProductDetail = { productId ->
                    navController.navigate(ProductRoutes.ProductDetail.createRoute(productId)) {
                        launchSingleTop = true
                        popUpTo(ProductRoutes.ProductList.route)
                    }
                },
                navigateBack = {
                    navController.popBackStack()
                },
                navController = navController
            )
        }

        // Экран детальной информации о продукте - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = ProductRoutes.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val productId = entry.arguments?.getString("productId") ?: ""
            val screenContainer = rememberEphemeralScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(productId) {
                screenContainer.createProductDetailViewModel(productId)
            }

            ProductDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}