package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.BinX
import kotlinx.coroutines.flow.Flow

interface BinXRepository {
    // Получение всех ячеек
    fun getBins(): Flow<List<BinX>>

    // Получение отфильтрованного списка ячеек
    fun getFilteredBins(
        codeFilter: String? = null,
        zoneFilter: String? = null
    ): Flow<List<BinX>>

    // Получение ячейки по коду
    suspend fun getBinByCode(code: String): BinX?

    // Добавление ячейки
    suspend fun addBin(bin: BinX)

    // Обновление ячейки
    suspend fun updateBin(bin: BinX)

    // Удаление ячейки
    suspend fun deleteBin(code: String)

    // Проверка существования ячейки
    suspend fun binExists(code: String): Boolean
}