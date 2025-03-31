package com.synngate.synnframe.data.remote.service

import com.synngate.synnframe.domain.entity.Server

interface ServerProvider {

    suspend fun getActiveServer(): Server?

    suspend fun getCurrentUserId(): String?
}