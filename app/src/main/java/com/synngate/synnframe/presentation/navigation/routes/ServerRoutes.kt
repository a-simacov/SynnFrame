package com.synngate.synnframe.presentation.navigation.routes

object ServerRoutes {

    object ServerList : Route {
        override val route = "server_list"
    }

    object ServerDetail : RouteWithArgs {
        override val route = "server_detail/{serverId}"

        fun createRoute(serverId: Int? = null): String =
            serverId?.let { "server_detail/$it" } ?: "server_detail/new"

        override fun createRoute(vararg args: Any?): String =
            createRoute(args.firstOrNull() as? Int)
    }

    object ServersGraph : NavGraph {
        override val route = "servers_graph"
        override val startDestination = ServerList
    }
}