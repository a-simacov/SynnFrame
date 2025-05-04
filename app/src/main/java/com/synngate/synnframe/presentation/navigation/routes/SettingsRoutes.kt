package com.synngate.synnframe.presentation.navigation.routes

object SettingsRoutes {

    object Settings : Route {
        override val route = "settings"
    }

    object SyncHistory : Route {
        override val route = "sync_history"
    }

    object SettingsGraph : NavGraph {
        override val route = "settings_graph"
        override val startDestination = Settings
    }
}