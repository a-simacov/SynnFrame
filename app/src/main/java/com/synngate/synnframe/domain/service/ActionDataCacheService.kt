package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.UUID

/**
 * Упрощенный сервис для работы с объектами в процессе выполнения действий
 * Не использует репозитории, только создает объекты на основе введенных данных
 */
class ActionDataCacheService {
    // Потоки кэшированных данных - сохраняем пустые для обратной совместимости
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _bins = MutableStateFlow<List<BinX>>(emptyList())
    val bins: StateFlow<List<BinX>> = _bins

    private val _pallets = MutableStateFlow<List<Pallet>>(emptyList())
    val pallets: StateFlow<List<Pallet>> = _pallets

    // Методы загрузки данных - теперь просто заглушки
    suspend fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        // Ничего не делаем, данные приходят из TaskContextManager
        _products.value = emptyList()
    }

    suspend fun loadBins(query: String? = null, zone: String? = null) {
        // Ничего не делаем, ячейки создаются по коду
        _bins.value = emptyList()
    }

    suspend fun loadPallets(query: String? = null) {
        // Ничего не делаем, паллеты создаются по коду
        _pallets.value = emptyList()
    }

    // Методы поиска по коду - создают новые объекты
    suspend fun findProductByBarcode(barcode: String): Product? {
        return try {
            // Создаем временный продукт по штрихкоду
            Product(
                id = "tmp_${UUID.randomUUID()}",
                name = "Товар со штрихкодом $barcode",
                accountingModel = AccountingModel.QTY,
                articleNumber = barcode,
                mainUnitId = "unit_1",
                units = emptyList()
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании временного товара по штрихкоду: $barcode")
            null
        }
    }

    suspend fun findBinByCode(code: String): BinX? {
        return try {
            // Создаем объект ячейки с введенным кодом
            BinX(
                code = code,
                zone = "Неизвестная зона",
                line = "",
                rack = "",
                tier = "",
                position = ""
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании ячейки по коду: $code")
            null
        }
    }

    suspend fun findPalletByCode(code: String): Pallet? {
        return try {
            // Создаем объект паллеты с введенным кодом
            Pallet(
                code = code,
                isClosed = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании паллеты по коду: $code")
            null
        }
    }

    // Методы для работы с паллетами
    suspend fun createPallet(): Result<Pallet> {
        return try {
            val code = "IN${System.currentTimeMillis()}"
            val pallet = Pallet(
                code = code,
                isClosed = false
            )
            Result.success(pallet)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании паллеты")
            Result.failure(e)
        }
    }

    suspend fun closePallet(code: String): Result<Boolean> {
        // Просто возвращаем успех, так как мы не храним состояние паллет
        return Result.success(true)
    }

    suspend fun printPalletLabel(code: String): Result<Boolean> {
        // Просто возвращаем успех, так как мы не выполняем реальную печать
        return Result.success(true)
    }

    fun clearCache() {
        _products.value = emptyList()
        _bins.value = emptyList()
        _pallets.value = emptyList()
    }
}