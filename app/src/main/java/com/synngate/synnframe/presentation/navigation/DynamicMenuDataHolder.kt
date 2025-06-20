package com.synngate.synnframe.presentation.navigation

import timber.log.Timber

/**
 * Синглтон для хранения данных динамического меню между навигациями
 */
object DynamicMenuDataHolder {
    // Карта для хранения сохраненных ключей поиска по menuItemId
    private val savedSearchKeys = mutableMapOf<String, SavedKeyData>()

    data class SavedKeyData(
        val key: String,
        val isValid: Boolean
    )

    /**
     * Сохраняет ключ поиска для конкретного пункта меню
     */
    fun setSavedSearchKey(menuItemId: String, key: String, isValid: Boolean) {
        savedSearchKeys[menuItemId] = SavedKeyData(key, isValid)
        Timber.d("Saved search key for menuItemId: $menuItemId, key: $key")
    }

    /**
     * Получает сохраненный ключ поиска для пункта меню
     */
    fun getSavedSearchKey(menuItemId: String): SavedKeyData? {
        return savedSearchKeys[menuItemId]
    }

    /**
     * Очищает сохраненный ключ поиска для пункта меню
     */
    fun clearSavedSearchKey(menuItemId: String) {
        savedSearchKeys.remove(menuItemId)
        Timber.d("Cleared saved search key for menuItemId: $menuItemId")
    }

    /**
     * Проверяет, есть ли сохраненный ключ для пункта меню
     */
    fun hasSavedSearchKey(menuItemId: String): Boolean {
        return savedSearchKeys.containsKey(menuItemId)
    }
}