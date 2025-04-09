package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.BinXRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockBinXRepository : BinXRepository {

    private val binsFlow = MutableStateFlow<Map<String, BinX>>(createInitialBins())

    override fun getBins(): Flow<List<BinX>> {
        return binsFlow.map { it.values.toList() }
    }

    override fun getFilteredBins(codeFilter: String?, zoneFilter: String?): Flow<List<BinX>> {
        return binsFlow.map { bins ->
            bins.values.filter { bin ->
                (codeFilter == null || bin.code.contains(codeFilter, ignoreCase = true)) &&
                        (zoneFilter == null || bin.zone == zoneFilter)
            }
        }
    }

    override suspend fun getBinByCode(code: String): BinX? {
        return binsFlow.value[code]
    }

    override suspend fun addBin(bin: BinX) {
        val updatedBins = binsFlow.value.toMutableMap()
        updatedBins[bin.code] = bin
        binsFlow.value = updatedBins
    }

    override suspend fun updateBin(bin: BinX) {
        addBin(bin) // Same implementation for mock
    }

    override suspend fun deleteBin(code: String) {
        val updatedBins = binsFlow.value.toMutableMap()
        updatedBins.remove(code)
        binsFlow.value = updatedBins
    }

    override suspend fun binExists(code: String): Boolean {
        return binsFlow.value.containsKey(code)
    }

    // Создание начальных тестовых данных
    private fun createInitialBins(): Map<String, BinX> {
        val bins = mutableMapOf<String, BinX>()

        // Создаем ячейки для зоны приемки
        for (i in 1..5) {
            val code = "A0011$i"
            bins[code] = BinX(
                code = code,
                zone = "Приемка",
                line = "A",
                rack = "01",
                tier = "1",
                position = i.toString()
            )
        }

        // Создаем ячейки для зоны хранения
        for (i in 1..10) {
            val code = "B0021$i"
            bins[code] = BinX(
                code = code,
                zone = "Хранение",
                line = "B",
                rack = "02",
                tier = "1",
                position = i.toString()
            )
        }

        // Создаем ячейки для зоны отбора
        for (i in 1..5) {
            val code = "C0031$i"
            bins[code] = BinX(
                code = code,
                zone = "Отбор",
                line = "C",
                rack = "03",
                tier = "1",
                position = i.toString()
            )
        }

        return bins
    }
}