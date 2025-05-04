package com.synngate.synnframe.presentation.navigation.routes

/**
 * Базовый интерфейс для всех маршрутов в приложении.
 */
interface Route {
    val route: String
}

/**
 * Интерфейс для маршрутов, которые могут создавать конкретные пути с аргументами.
 */
interface RouteWithArgs : Route {
    /**
     * Создает полный путь маршрута с подставленными аргументами.
     *
     * @param args Аргументы для подстановки в шаблон маршрута
     * @return Полный путь для навигации
     */
    fun createRoute(vararg args: Any?): String
}

/**
 * Базовый интерфейс для всех графов навигации.
 * Позволяет определить стартовый маршрут и сам граф.
 */
interface NavGraph : Route {
    /** Маршрут, с которого начинается граф */
    val startDestination: Route
}