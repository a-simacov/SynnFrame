package com.synngate.synnframe.presentation.di.modules.api

import androidx.lifecycle.ViewModel

/**
 * Интерфейс для функциональных контейнеров.
 * Определяет методы для создания ViewModel-ей и работы с зависимостями
 * специфичными для конкретной функциональной области.
 */
interface FeatureAPI : ModuleAPI {
    /**
     * Создание ViewModel с указанным ключом
     *
     * @param key Уникальный ключ для ViewModel
     * @param factory Фабрика для создания ViewModel
     * @return Созданная или существующая ViewModel
     */
    fun <T : ViewModel> getViewModel(key: String, factory: () -> T): T
}