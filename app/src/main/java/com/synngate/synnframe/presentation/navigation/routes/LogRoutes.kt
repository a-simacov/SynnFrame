package com.synngate.synnframe.presentation.navigation.routes

object LogRoutes {

    object LogList : Route {
        override val route: String
            get() = "log_list"
    }

    object LogDetail : RouteWithArgs {

        override val route: String
            get() = "log_detail/{logId}"

        fun createRoute(logId: Int? = null): String {
            return "log_detail/$logId"
        }

        override fun createRoute(vararg args: Any?): String {
            return createRoute(args.firstOrNull() as? Int)
        }
    }

    object LogsGraph : NavGraph {
        override val route: String
            get() = "logs_graph"
        override val startDestination: Route
            get() = LogList
    }
}