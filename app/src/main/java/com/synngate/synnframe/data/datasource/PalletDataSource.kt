package com.synngate.synnframe.data.datasource

import com.synngate.synnframe.domain.entity.taskx.Pallet

interface PalletDataSource {
    suspend fun getPallets(query: String? = null): List<Pallet>
    suspend fun getPalletByCode(code: String): Pallet?
    suspend fun createPallet(): Result<Pallet>
    suspend fun closePallet(code: String): Result<Boolean>
    suspend fun printPalletLabel(code: String): Result<Boolean>
}