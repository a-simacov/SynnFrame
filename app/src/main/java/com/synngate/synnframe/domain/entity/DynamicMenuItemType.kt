package com.synngate.synnframe.domain.entity

enum class DynamicMenuItemType {
    SUBMENU,    // Подменю - остаемся на этом экране, загружаем новые элементы
    TASKS,      // Задания - переход на экран списка заданий
    PRODUCTS;   // Товары - переход на экран товаров

    companion object {
        fun fromString(value: String): DynamicMenuItemType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                SUBMENU
            }
        }
    }
}