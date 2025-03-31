package com.synngate.synnframe.domain.entity

data class Server(
    val id: Int = 0,
    val name: String,

    val host: String,
    val port: Int,
    val apiEndpoint: String,

    val login: String,
    val password: String,

    val isActive: Boolean = false
) {
    val apiUrl: String
        get() = "https://$host:$port$apiEndpoint"

    val echoUrl: String
        get() = "$apiUrl/echo"
}