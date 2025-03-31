package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.Server

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int,
    val apiEndpoint: String,
    val login: String,
    val password: String,
    val isActive: Boolean = false
) {

    fun toDomainModel(): Server {
        return Server(
            id = id,
            name = name,
            host = host,
            port = port,
            apiEndpoint = apiEndpoint,
            login = login,
            password = password,
            isActive = isActive
        )
    }

    companion object {
        fun fromDomainModel(server: Server): ServerEntity {
            return ServerEntity(
                id = server.id,
                name = server.name,
                host = server.host,
                port = server.port,
                apiEndpoint = server.apiEndpoint,
                login = server.login,
                password = server.password,
                isActive = server.isActive
            )
        }
    }
}