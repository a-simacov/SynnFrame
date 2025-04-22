package com.synngate.synnframe.domain.entity

enum class OperationMenuType {
    SHOW_LIST,  // Стандартное поведение - показ списка
    SEARCH;     // Поиск по значению

    companion object {
        fun fromString(value: String): OperationMenuType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                SHOW_LIST // Значение по умолчанию
            }
        }
    }
}