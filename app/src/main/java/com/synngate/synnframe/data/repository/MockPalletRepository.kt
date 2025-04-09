package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.PalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class MockPalletRepository : PalletRepository {

    private val palletsFlow = MutableStateFlow<Map<String, Pallet>>(createInitialPallets())
    private var nextPalletNumber = 1000 // Начальный номер для генерации новых паллет

    override fun getPallets(): Flow<List<Pallet>> {
        return palletsFlow.map { it.values.toList() }
    }

    override suspend fun getPalletByCode(code: String): Pallet? {
        return palletsFlow.value[code]
    }

    override suspend fun createPallet(): Result<Pallet> {
        return try {
            // Имитация задержки обращения к серверу
            delay(500)

            val palletCode = "IN0000000${nextPalletNumber++}"
            val newPallet = Pallet(
                code = palletCode,
                isClosed = false
            )

            // Добавляем в репозиторий
            addPallet(newPallet)

            Timber.i("Создана новая паллета с кодом: $palletCode")
            Result.success(newPallet)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании паллеты")
            Result.failure(e)
        }
    }

    override suspend fun addPallet(pallet: Pallet) {
        val updatedPallets = palletsFlow.value.toMutableMap()
        updatedPallets[pallet.code] = pallet
        palletsFlow.value = updatedPallets
    }

    override suspend fun updatePallet(pallet: Pallet) {
        addPallet(pallet) // Same implementation for mock
    }

    override suspend fun closePallet(code: String): Result<Boolean> {
        return try {
            // Имитация задержки обращения к серверу
            delay(300)

            val pallet = palletsFlow.value[code]
            if (pallet == null) {
                Timber.w("Попытка закрыть несуществующую паллету: $code")
                return Result.failure(NoSuchElementException("Паллета не найдена: $code"))
            }

            // Обновляем статус паллеты
            val updatedPallet = pallet.copy(isClosed = true)
            updatePallet(updatedPallet)

            Timber.i("Паллета $code успешно закрыта")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при закрытии паллеты: $code")
            Result.failure(e)
        }
    }

    override suspend fun deletePallet(code: String) {
        val updatedPallets = palletsFlow.value.toMutableMap()
        updatedPallets.remove(code)
        palletsFlow.value = updatedPallets
    }

    override suspend fun printPalletLabel(code: String): Result<Boolean> {
        return try {
            // Имитация задержки обращения к принтеру
            delay(1000)

            val pallet = palletsFlow.value[code]
            if (pallet == null) {
                Timber.w("Попытка печати этикетки для несуществующей паллеты: $code")
                return Result.failure(NoSuchElementException("Паллета не найдена: $code"))
            }

            Timber.i("Этикетка для паллеты $code успешно отправлена на печать")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при печати этикетки паллеты: $code")
            Result.failure(e)
        }
    }

    override suspend fun getPalletContents(code: String): Result<List<String>> {
        return try {
            // Имитация задержки обращения к серверу
            delay(700)

            val pallet = palletsFlow.value[code]
            if (pallet == null) {
                Timber.w("Попытка получения содержимого несуществующей паллеты: $code")
                return Result.failure(NoSuchElementException("Паллета не найдена: $code"))
            }

            // Генерируем тестовое содержимое
            val contents = listOf(
                "Товар: Наушники вкладыши, Количество: 26",
                "Товар: Молоко, Количество: 18, Срок годности: 21.10.2025"
            )

            Timber.i("Получено содержимое паллеты $code")
            Result.success(contents)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении содержимого паллеты: $code")
            Result.failure(e)
        }
    }

    // Создание начальных тестовых данных
    private fun createInitialPallets(): Map<String, Pallet> {
        val pallets = mutableMapOf<String, Pallet>()

        // Добавляем несколько тестовых паллет
        pallets["IN000000001"] = Pallet(code = "IN000000001", isClosed = true)
        pallets["IN000000002"] = Pallet(code = "IN000000002", isClosed = false)
        pallets["IN000000003"] = Pallet(code = "IN000000003", isClosed = true)

        return pallets
    }
}