package com.synngate.synnframe.data.datasource.mock

import com.synngate.synnframe.data.datasource.BinDataSource
import com.synngate.synnframe.domain.entity.taskx.BinX

class MockBinDataSource : BinDataSource {
    private val bins = listOf(
        BinX(
            code = "A00111",
            zone = "Приемка",
            line = "A",
            rack = "01",
            tier = "1",
            position = "1"
        ),
        BinX(
            code = "A00112",
            zone = "Приемка",
            line = "A",
            rack = "01",
            tier = "1",
            position = "2"
        ),
        BinX(
            code = "B00211",
            zone = "Хранение",
            line = "B",
            rack = "02",
            tier = "1",
            position = "1"
        )
        // Можно добавить больше тестовых данных
    )

    override suspend fun getBins(query: String?, zone: String?): List<BinX> {
        return bins.filter { bin ->
            (query == null || bin.code.contains(query, ignoreCase = true)) &&
                    (zone == null || bin.zone == zone)
        }
    }

    override suspend fun getBinByCode(code: String): BinX? {
        return bins.find { it.code == code }
    }
}