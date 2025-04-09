package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.Pallet
import kotlinx.coroutines.flow.Flow

interface PalletRepository {
    // Получение всех паллет
    fun getPallets(): Flow<List<Pallet>>

    // Получение паллеты по коду
    suspend fun getPalletByCode(code: String): Pallet?

    // Создание новой паллеты (с генерацией кода)
    suspend fun createPallet(): Result<Pallet>

    // Добавление паллеты
    suspend fun addPallet(pallet: Pallet)

    // Обновление паллеты
    suspend fun updatePallet(pallet: Pallet)

    // Закрытие паллеты
    suspend fun closePallet(code: String): Result<Boolean>

    // Удаление паллеты
    suspend fun deletePallet(code: String)

    // Печать этикетки
    suspend fun printPalletLabel(code: String): Result<Boolean>

    // Получение информации о содержимом паллеты
    suspend fun getPalletContents(code: String): Result<List<String>>
}