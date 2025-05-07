package com.synngate.synnframe.presentation.navigation.routes

object TaskXRoutes {

    object TaskXList : Route {
        override val route = "taskx_list"
    }

    object TaskXDetail : RouteWithArgs {
        override val route = "taskx_detail/{taskId}"

        fun createRoute(taskId: String): String = "taskx_detail/$taskId"

        override fun createRoute(vararg args: Any?): String =
            createRoute(args.firstOrNull() as? String ?: "")
    }

    // Новый маршрут для экрана визарда действий
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