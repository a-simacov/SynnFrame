package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datasource.BinDataSource
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.WizardBinRepository

class WizardBinRepositoryImpl(
    private val binDataSource: BinDataSource
) : WizardBinRepository {

    override suspend fun getBins(query: String?, zone: String?): List<BinX> {
        return binDataSource.getBins(query, zone)
    }

    override suspend fun getBinByCode(code: String): BinX? {
        return binDataSource.getBinByCode(code)
    }
}