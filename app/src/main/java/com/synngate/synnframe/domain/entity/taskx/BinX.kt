// Ячейка X (BinX) - исправленная
package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class BinX(
    val code: String,          // Код ячейки (для сканирования)
    val zone: String,          // Зона
    val line: String = "",          // Линия
    val rack: String = "",          // Стеллаж
    val tier: String = "",          // Ярус
    val position: String = ""       // Позиция
) {
    // Метод для получения полного имени ячейки в отформатированном виде
    fun getFullName(): String {
        return "$zone-$line-$rack-$tier-$position"
    }
}