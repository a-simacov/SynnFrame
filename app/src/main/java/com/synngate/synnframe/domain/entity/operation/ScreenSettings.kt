package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.Serializable

@Serializable
enum class ScreenElementType {
    SHOW_LIST,  // Показ списка объектов
    SEARCH      // Поле поиска
}

@Serializable
data class ScreenSettings(
    val openImmediately: Boolean = false,  // Автоматически открыть экран объекта при одном результате
    val screenElements: List<ScreenElementType> = listOf(ScreenElementType.SHOW_LIST)
)