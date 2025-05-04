package com.synngate.synnframe.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.DynamicRoutes
import com.synngate.synnframe.presentation.navigation.routes.LogRoutes
import com.synngate.synnframe.presentation.navigation.routes.MainRoutes
import com.synngate.synnframe.presentation.navigation.routes.ProductRoutes
import com.synngate.synnframe.presentation.navigation.routes.SettingsRoutes
import com.synngate.synnframe.presentation.navigation.routes.TaskRoutes
import com.synngate.synnframe.presentation.navigation.routes.TaskXRoutes

/**
 * Расширения для NavController для упрощения навигации.
 * Эти функции скрывают детали создания маршрутов и настройки опций навигации.
 */

// Навигация для аутентификации
fun NavController.navigateToLogin(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(AuthRoutes.Login.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = true }
        builder()
    }
}

// Навигация для главного меню
fun NavController.navigateToMainMenu(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(MainRoutes.MainMenu.route) {
        popUpTo(AuthRoutes.Login.route) { inclusive = true }
        builder()
    }
}

// Навигация для задач
fun NavController.navigateToTaskList(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(TaskRoutes.TaskList.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToTaskDetail(taskId: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(TaskRoutes.TaskDetail.createRoute(taskId)) {
        builder()
    }
}

// Навигация для продуктов
fun NavController.navigateToProductList(isSelectionMode: Boolean = false, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ProductRoutes.ProductList.createRoute(isSelectionMode)) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToProductDetail(productId: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ProductRoutes.ProductDetail.createRoute(productId)) {
        launchSingleTop = true
        builder()
    }
}

// Навигация для логов
fun NavController.navigateToLogList(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(LogRoutes.LogList.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToLogDetail(logId: Int, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(LogRoutes.LogDetail.createRoute(logId)) {
        builder()
    }
}

// Навигация для заданий X
fun NavController.navigateToTaskXList(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(TaskXRoutes.TaskXList.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToTaskXDetail(taskId: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(TaskXRoutes.TaskXDetail.createRoute(taskId)) {
        builder()
    }
}

// Навигация для настроек
fun NavController.navigateToSettings(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(SettingsRoutes.Settings.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}

fun NavController.navigateToSyncHistory(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(SettingsRoutes.SyncHistory.route) {
        builder()
    }
}

// Навигация для динамического меню
fun NavController.navigateToDynamicMenu(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(DynamicRoutes.DynamicMenu.route) {
        popUpTo(MainRoutes.MainMenu.route) { inclusive = false }
        builder()
    }
}