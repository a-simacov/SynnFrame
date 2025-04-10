package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datasource.PalletDataSource
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardPalletRepository

class WizardPalletRepositoryImpl(
    private val palletDataSource: PalletDataSource
) : WizardPalletRepository {

    override suspend fun getPallets(query: String?): List<Pallet> {
        return palletDataSource.getPallets(query)
    }

    override suspend fun getPalletByCode(code: String): Pallet? {
        return palletDataSource.getPalletByCode(code)
    }

    override suspend fun createPallet(): Result<Pallet> {
        return palletDataSource.createPallet()
    }

    override suspend fun closePallet(code: String): Result<Boolean> {
        return palletDataSource.closePallet(code)
    }

    override suspend fun printPalletLabel(code: String): Result<Boolean> {
        return palletDataSource.printPalletLabel(code)
    }
}