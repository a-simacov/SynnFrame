package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.UUID

class ActionDataCacheService {
    // Потоки кэшированных данных
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _bins = MutableStateFlow<List<BinX>>(emptyList())
    val bins: StateFlow<List<BinX>> = _bins

    private val _pallets = MutableStateFlow<List<Pallet>>(emptyList())
    val pallets: StateFlow<List<Pallet>> = _pallets

    suspend fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        try {
            _products.value = emptyList()
            Timber.d("Имитация загрузки товаров завершена")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации загрузки товаров")
            _products.value = emptyList()
        }
    }

    suspend fun findProductByBarcode(barcode: String): Product? {
        return try {
            val product = Product(
                id = "tmp_${UUID.randomUUID()}",
                name = "Товар со штрихкодом $barcode",
                accountingModel = AccountingModel.QTY,
                articleNumber = barcode,
                mainUnitId = "unit_1",
                units = emptyList()
            )
            product
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации поиска товара по штрихкоду: $barcode")
            null
        }
    }

    suspend fun loadBins(query: String? = null, zone: String? = null) {
        try {
            _bins.value = emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации загрузки ячеек")
            _bins.value = emptyList()
        }
    }

    suspend fun findBinByCode(code: String): BinX? {
        return try {
            val bin = BinX(
                code = code,
                zone = "Зона приёмки",
                line = "A",
                rack = "01",
                tier = "1",
                position = "1"
            )
            bin
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации поиска ячейки по коду: $code")
            null
        }
    }

    suspend fun loadPallets(query: String? = null) {
        try {
            _pallets.value = emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации загрузки паллет")
            _pallets.value = emptyList()
        }
    }

    suspend fun findPalletByCode(code: String): Pallet? {
        return try {
            val pallet = Pallet(
                code = code,
                isClosed = false
            )
            pallet
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации поиска паллеты по коду: $code")
            null
        }
    }

    suspend fun createPallet(): Result<Pallet> {
        return try {
            val code = "IN${System.currentTimeMillis()}"
            val pallet = Pallet(
                code = code,
                isClosed = false
            )
            _pallets.value = _pallets.value + pallet
            Result.success(pallet)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации создания паллеты")
            Result.failure(e)
        }
    }

    suspend fun closePallet(code: String): Result<Boolean> {
        return try {
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации закрытия паллеты: $code")
            Result.failure(e)
        }
    }

    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return try {
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при имитации печати этикетки паллеты: $code")
            Result.failure(e)
        }
    }

    fun clearCache() {
        _products.value = emptyList()
        _bins.value = emptyList()
        _pallets.value = emptyList()
    }
}