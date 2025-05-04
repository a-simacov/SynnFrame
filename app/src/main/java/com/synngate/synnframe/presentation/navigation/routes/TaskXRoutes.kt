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

    object TaskXGraph : NavGraph {
        override val route = "taskx_graph"
        override val startDestination = TaskXList
    }
}