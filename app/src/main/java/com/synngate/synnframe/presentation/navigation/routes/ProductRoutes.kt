package com.synngate.synnframe.presentation.navigation.routes

object ProductRoutes {

    object ProductList : RouteWithArgs {
        override val route = "product_list?isSelectionMode={isSelectionMode}"

        fun createRoute(isSelectionMode: Boolean = false): String =
            "product_list?isSelectionMode=$isSelectionMode"

        override fun createRoute(vararg args: Any?): String =
            createRoute(args.firstOrNull() as? Boolean ?: false)
    }

    object ProductDetail : RouteWithArgs {
        override val route = "product_detail/{productId}"

        fun createRoute(productId: String): String = "product_detail/$productId"

        override fun createRoute(vararg args: Any?): String =
            createRoute(args.firstOrNull() as? String ?: "")
    }

    object ProductsGraph : NavGraph {
        override val route = "products_graph"
        override val startDestination = ProductList
    }
}