package com.synngate.synnframe.presentation.navigation.routes

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.Base64

/**
 * Объект, содержащий все маршруты для динамических экранов.
 */
object DynamicRoutes {

    /**
     * Маршрут для экрана динамического меню.
     */
    object DynamicMenu : Route {
        override val route = "dynamic_menu"
    }

    /**
     * Маршрут для детального просмотра динамической задачи.
     */
    object DynamicTaskDetail : RouteWithArgs {
        override val route = "dynamic_task_detail/{taskId}/{endpoint}"

        /**
         * Создает маршрут для просмотра конкретной задачи.
         *
         * @param taskId Идентификатор задачи
         * @param endpoint Эндпоинт для получения данных о задаче
         * @return Строка маршрута
         */
        fun createRoute(taskId: String, endpoint: String): String {
            // Кодируем endpoint в Base64 для безопасной передачи
            val encodedEndpoint = Base64.getEncoder().encodeToString(endpoint.toByteArray())
            return "dynamic_task_detail/$taskId/$encodedEndpoint"
        }

        override fun createRoute(vararg args: Any?): String {
            require(args.size >= 2) { "DynamicTaskDetail route requires taskId and endpoint" }
            val taskId = args[0] as? String ?: ""
            val endpoint = args[1] as? String ?: ""
            return createRoute(taskId, endpoint)
        }
    }

    /**
     * Маршрут для списка динамических задач.
     */
    object DynamicTasks : RouteWithArgs {
        override val route = "dynamic_tasks/{menuItemId}/{menuItemName}/{endpoint}?screenSettings={screenSettings}"

        /**
         * Создает маршрут для просмотра списка задач.
         *
         * @param menuItemId Идентификатор пункта меню
         * @param menuItemName Имя пункта меню
         * @param endpoint Эндпоинт для получения данных
         * @param screenSettings Настройки экрана
         * @return Строка маршрута
         */
        fun createRoute(
            menuItemId: String,
            menuItemName: String,
            endpoint: String,
            screenSettings: ScreenSettings
        ): String {
            val encodedName = URLEncoder.encode(menuItemName, "UTF-8")
            // Используем Base64 кодирование для сохранения пробелов в endpoint
            val encodedEndpoint = Base64.getEncoder().encodeToString(endpoint.toByteArray())

            // Если переданы настройки экрана, кодируем их в JSON и передаем как параметр
            val encodedSettings = if (screenSettings != ScreenSettings()) {
                val json = Json.encodeToString(ScreenSettings.serializer(), screenSettings)
                "?screenSettings=${URLEncoder.encode(json, "UTF-8")}"
            } else {
                ""
            }

            return "dynamic_tasks/$menuItemId/$encodedName/$encodedEndpoint$encodedSettings"
        }

        override fun createRoute(vararg args: Any?): String {
            require(args.size >= 3) { "DynamicTasks route requires menuItemId, menuItemName, and endpoint" }
            val menuItemId = args[0] as? String ?: ""
            val menuItemName = args[1] as? String ?: ""
            val endpoint = args[2] as? String ?: ""
            val screenSettings = args.getOrNull(3) as? ScreenSettings ?: ScreenSettings()
            return createRoute(menuItemId, menuItemName, endpoint, screenSettings)
        }
    }

    /**
     * Маршрут для списка динамических продуктов.
     */
    object DynamicProducts : RouteWithArgs {
        override val route = "dynamic_products/{menuItemId}/{menuItemName}/{endpoint}?screenSettings={screenSettings}"

        /**
         * Создает маршрут для просмотра списка продуктов.
         *
         * @param menuItemId Идентификатор пункта меню
         * @param menuItemName Имя пункта меню
         * @param endpoint Эндпоинт для получения данных
         * @param screenSettings Настройки экрана
         * @return Строка маршрута
         */
        fun createRoute(
            menuItemId: String,
            menuItemName: String,
            endpoint: String,
            screenSettings: ScreenSettings
        ): String {
            val encodedName = URLEncoder.encode(menuItemName, "UTF-8")
            // Используем Base64 кодирование для сохранения пробелов и спецсимволов в endpoint
            val encodedEndpoint = Base64.getEncoder().encodeToString(endpoint.toByteArray())

            // Если переданы настройки экрана, кодируем их в JSON и передаем как параметр
            val encodedSettings = if (screenSettings != ScreenSettings()) {
                val json = Json.encodeToString(ScreenSettings.serializer(), screenSettings)
                "?screenSettings=${URLEncoder.encode(json, "UTF-8")}"
            } else {
                ""
            }

            return "dynamic_products/$menuItemId/$encodedName/$encodedEndpoint$encodedSettings"
        }

        override fun createRoute(vararg args: Any?): String {
            require(args.size >= 3) { "DynamicProducts route requires menuItemId, menuItemName, and endpoint" }
            val menuItemId = args[0] as? String ?: ""
            val menuItemName = args[1] as? String ?: ""
            val endpoint = args[2] as? String ?: ""
            val screenSettings = args.getOrNull(3) as? ScreenSettings ?: ScreenSettings()
            return createRoute(menuItemId, menuItemName, endpoint, screenSettings)
        }
    }

    /**
     * Маршрут для просмотра деталей динамического продукта.
     */
    object DynamicProductDetail : RouteWithArgs {
        override val route = "dynamic_product_detail/{productId}/{productName}"

        /**
         * Создает маршрут для просмотра конкретного продукта.
         *
         * @param product Продукт для отображения
         * @return Строка маршрута
         */
        fun createRoute(product: DynamicProduct): String {
            val encodedName = URLEncoder.encode(product.name, "UTF-8")
            return "dynamic_product_detail/${product.id}/$encodedName"
        }

        override fun createRoute(vararg args: Any?): String {
            require(args.isNotEmpty()) { "DynamicProductDetail route requires a product" }
            val product = args[0] as? DynamicProduct
                ?: throw IllegalArgumentException("First argument must be a DynamicProduct")
            return createRoute(product)
        }
    }

    /**
     * Граф навигации для динамических экранов.
     */
    object DynamicNavGraph : NavGraph {
        override val route = "dynamic_nav_graph"
        override val startDestination = DynamicMenu
    }
}