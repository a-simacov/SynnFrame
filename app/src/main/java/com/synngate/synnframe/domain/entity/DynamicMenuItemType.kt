package com.synngate.synnframe.domain.entity

enum class DynamicMenuItemType {
    SHOW_LIST,
    SEARCH;

    companion object {
        fun fromString(value: String): DynamicMenuItemType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                SHOW_LIST
            }
        }
    }
}