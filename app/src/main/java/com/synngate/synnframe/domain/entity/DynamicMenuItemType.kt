package com.synngate.synnframe.domain.entity

enum class DynamicMenuItemType {
    SUBMENU,
    TASKS,
    PRODUCTS;

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