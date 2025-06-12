package com.synngate.synnframe.presentation.service.webserver

object WebServerConstants {
    // API Routes
    const val ROUTE_ECHO = "/echo"
    const val ROUTE_PRODUCTS = "/products"

    // Parameters
    const val PORT_DEFAULT = 8080
    const val AUTH_REALM = "SynnFrame API"
    const val AUTH_SCHEME = "auth-basic"

    // Service parameters
    const val SYNC_PREFIX = "webserver-"
    const val DEFAULT_SYNC_DURATION = 1000L

    // Log messages
    const val LOG_WEB_SERVER_STARTED = "Local web server started on port %d"
    const val LOG_WEB_SERVER_STOPPED = "Local web server stopped"
    const val LOG_TASKS_RECEIVED = "Received %d tasks via local web server. Added new: %d, updated: %d. Processing time: %dms"
    const val LOG_PRODUCTS_RECEIVED = "Received %d products via local web server. Added new: %d, updated: %d. Processing time: %dms"
    const val LOG_PRODUCTS_FULL_UPDATE = "Full update of product catalog via local web server: %d products"
    const val LOG_TASK_TYPES_RECEIVED = "Received %d task types via local web server. Processing time: %dms"
    const val LOG_ERROR_TASK_TYPES = "Error processing task types: %s"

    // Operations
    const val OPERATION_TASKS_RECEIVED = "Tasks received via local web server"
    const val OPERATION_PRODUCTS_RECEIVED = "Products received via local web server"
    const val OPERATION_TASK_TYPES_RECEIVED = "Task types received via local web server"
    const val OPERATION_DATA_RECEIVED = "Data received via local web server"
}