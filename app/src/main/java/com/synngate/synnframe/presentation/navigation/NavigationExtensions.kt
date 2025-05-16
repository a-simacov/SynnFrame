package com.synngate.synnframe.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.DynamicRoutes
import com.synngate.synnframe.presentation.navigation.routes.LogRoutes
import com.synngate.synnframe.presentation.navigation.routes.MainRoutes
import com.synngate.synnframe.presentation.navigation.routes.ProductRoutes
import com.synngate.synnframe.presentation.navigation.routes.SettingsRoutes
import com.synngate.synnframe.presentation.navigation.routes.TaskXRoutes

/**
 * Расширения для NavController для упрощения навигации.
 * Эти функции скрывают детали создания маршрутов и настройки опций навигации.
 */

fun NavController.navigateToLogin(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(AuthRoutes.Login.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = true }
        builder()
    }
}

fun NavController.navigateToProductList(isSelectionMode: Boolean = false, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ProductRoutes.ProductList.createRoute(isSelectionMode)) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToLogList(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(LogRoutes.LogList.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToTaskXList(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(TaskXRoutes.TaskXList.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToSettings(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(SettingsRoutes.Settings.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToDynamicMenu(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(DynamicRoutes.DynamicMenu.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}