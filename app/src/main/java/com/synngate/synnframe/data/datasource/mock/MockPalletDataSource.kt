package com.synngate.synnframe.data.datasource.mock

import com.synngate.synnframe.data.datasource.PalletDataSource
import com.synngate.synnframe.domain.entity.taskx.Pallet
import kotlinx.coroutines.delay
import timber.log.Timber

class MockPalletDataSource : PalletDataSource {
    private val pallets = mutableListOf(
        Pallet(code = "IN00000000001", isClosed = true),
        Pallet(code = "IN00000000002", isClosed = false),
        Pallet(code = "IN00000000003", isClosed = false)
    )
    private var nextPalletNumber = 1000

    override suspend fun getPallets(query: String?): List<Pallet> {
        return pallets.filter { pallet ->
            query == null || pallet.code.contains(query, ignoreCase = true)
        }
    }

    override suspend fun getPalletByCode(code: String): Pallet? {
        return pallets.find { it.code == code }
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

            pallets.add(newPallet)

            Timber.i("Создана новая паллета с кодом: $palletCode")
            Result.success(newPallet)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании паллеты")
            Result.failure(e)
        }
    }

    override suspend fun closePallet(code: String): Result<Boolean> {
        return try {
            // Имитация задержки обращения к серверу
            delay(300)

            val index = pallets.indexOfFirst { it.code == code }
            if (index == -1) {
                Timber.w("Попытка закрыть несуществующую паллету: $code")
                return Result.failure(NoSuchElementException("Паллета не найдена: $code"))
            }

            // Обновляем статус паллеты
            pallets[index] = pallets[index].copy(isClosed = true)

            Timber.i("Паллета $code успешно закрыта")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при закрытии паллеты: $code")
            Result.failure(e)
        }
    }

    override suspend fun printPalletLabel(code: String): Result<Boolean> {
        return try {
            // Имитация задержки обращения к принтеру
            delay(1000)

            val pallet = pallets.find { it.code == code }
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
}