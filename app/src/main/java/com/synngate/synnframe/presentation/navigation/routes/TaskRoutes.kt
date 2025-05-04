package com.synngate.synnframe.presentation.navigation.routes

object TaskRoutes {

    object TaskList : Route {
        override val route = "task_list"
    }

    object TaskDetail : RouteWithArgs {
        override val route = "task_detail/{taskId}"

        fun createRoute(taskId: String): String = "task_detail/$taskId"

        override fun createRoute(vararg args: Any?): String =
            createRoute(args.firstOrNull() as? String ?: "")
    }

    object TasksGraph : NavGraph {
        override val route = "tasks_graph"
        override val startDestination = TaskList
    }
}