package com.synngate.synnframe.data.datasource

import com.synngate.synnframe.domain.entity.taskx.BinX

interface BinDataSource {
    suspend fun getBins(query: String? = null, zone: String? = null): List<BinX>
    suspend fun getBinByCode(code: String): BinX?
}