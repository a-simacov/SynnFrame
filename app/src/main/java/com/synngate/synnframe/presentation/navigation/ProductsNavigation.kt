package com.synngate.synnframe.presentation.navigation

import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.di.ProductsGraphContainer
import com.synngate.synnframe.presentation.ui.products.ProductDetailScreen
import com.synngate.synnframe.presentation.ui.products.ProductListScreen
import timber.log.Timber

/**
 * Навигационный граф для экранов товаров
 */
fun NavGraphBuilder.productsNavGraph(
    navController: NavHostController,
    productsGraphContainer: ProductsGraphContainer,
    lifecycleOwner: LifecycleOwner,
    returnProductToTask: ((Product) -> Unit)? = null
) {
    navigation(
        startDestination = Screen.ProductList.route,
        route = "products_graph"
    ) {
        // Экран списка товаров
        composable(
            route = Screen.ProductList.route,
            arguments = listOf(
                navArgument("isSelectionMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            // Получаем аргументы для режима выбора
            val isSelectionMode = entry.arguments?.getBoolean("isSelectionMode") ?: false

            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        Timber.d("ProductList screen destroyed, clearing container")
                        productsGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            ProductListScreen(
                viewModel = productsGraphContainer.createProductListViewModel(isSelectionMode),
                navigateToProductDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                navigateBack = {
                    navController.popBackStack()
                },
                returnProductToTask = returnProductToTask
            )

            // Удаляем наблюдателя при выходе из композиции
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }

        // Заменяем временный заполнитель в composable для ProductDetail
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            // Получаем ID товара из аргументов
            val productId = entry.arguments?.getString("productId") ?: ""

            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        Timber.d("ProductDetail screen destroyed, clearing container")
                        productsGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            ProductDetailScreen(
                viewModel = productsGraphContainer.createProductDetailViewModel(productId),
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToProduct = { newProductId ->
                    // Переходим к новому товару, заменяя текущий в стеке навигации
                    navController.navigate(Screen.ProductDetail.createRoute(newProductId)) {
                        popUpTo(Screen.ProductDetail.route) { inclusive = true }
                    }
                }
            )

            // Удаляем наблюдателя при выходе из композиции
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }
}