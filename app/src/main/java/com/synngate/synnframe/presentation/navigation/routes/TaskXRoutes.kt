package com.synngate.synnframe.presentation.navigation.routes

import java.util.Base64

object TaskXRoutes {

    object TaskXList : Route {
        override val route = "taskx_list"
    }

    object TaskXDetail : RouteWithArgs {
        override val route = "taskx_detail/{taskId}/{endpoint}"

        fun createRoute(taskId: String, endpoint: String): String {
            // Кодируем endpoint в Base64, т.к. он может содержать спецсимволы
            val encodedEndpoint = Base64.getEncoder().encodeToString(endpoint.toByteArray())
            return "taskx_detail/$taskId/$encodedEndpoint"
        }

        override fun createRoute(vararg args: Any?): String {
            require(args.size >= 2) { "TaskXDetail route requires taskId and endpoint" }
            val taskId = args[0] as? String ?: ""
            val endpoint = args[1] as? String ?: ""
            return createRoute(taskId, endpoint)
        }
    }

    // Маршрут для экрана визарда действий
    object ActionWizardScreen : RouteWithArgs {
        override val route = "action_wizard/{taskId}/{actionId}"

        fun createRoute(taskId: String, actionId: String): String =
            "action_wizard/$taskId/$actionId"

        override fun createRoute(vararg args: Any?): String {
            require(args.size >= 2) { "ActionWizardScreen route requires taskId and actionId" }
            val taskId = args[0] as? String ?: ""
            val actionId = args[1] as? String ?: ""
            return createRoute(taskId, actionId)
        }
    }

    object TaskXGraph : NavGraph {
        override val route = "taskx_graph"
        override val startDestination = TaskXList
    }
}