package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.BinX

interface WizardBinRepository {

    suspend fun getBins(query: String? = null, zone: String? = null): List<BinX>
    suspend fun getBinByCode(code: String): BinX?
}