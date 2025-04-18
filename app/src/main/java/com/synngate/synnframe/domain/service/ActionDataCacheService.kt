package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.repository.WizardProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Сервис для кэширования данных при выполнении действий
 */
class ActionDataCacheService(
    private val productRepository: WizardProductRepository,
    private val binRepository: WizardBinRepository,
    private val palletRepository: WizardPalletRepository
) {
    // Потоки кэшированных данных
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _bins = MutableStateFlow<List<BinX>>(emptyList())
    val bins: StateFlow<List<BinX>> = _bins

    private val _pallets = MutableStateFlow<List<Pallet>>(emptyList())
    val pallets: StateFlow<List<Pallet>> = _pallets

    /**
     * Загружает товары по запросу и опционально фильтрует по идентификаторам плана
     * @param query Строка запроса
     * @param planProductIds Идентификаторы товаров из плана
     */
    suspend fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        try {
            Timber.d("Loading products with query: $query, plan product ids: $planProductIds")

            val loadedProducts = if (planProductIds != null && planProductIds.isNotEmpty()) {
                // Если указаны идентификаторы товаров из плана, загружаем только их
                val allProducts = productRepository.getProductsByIds(planProductIds)

                // Дополнительно фильтруем по запросу, если он указан
                if (query != null && query.isNotEmpty()) {
                    allProducts.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.articleNumber.contains(query, ignoreCase = true)
                    }
                } else {
                    allProducts
                }
            } else {
                // Иначе загружаем по запросу
                productRepository.getProducts(query)
            }

            _products.value = loadedProducts
            Timber.d("Loaded ${loadedProducts.size} products")
        } catch (e: Exception) {
            Timber.e(e, "Error loading products")
            // В случае ошибки сохраняем пустой список
            _products.value = emptyList()
        }
    }

    /**
     * Ищет товар по штрихкоду
     * @param barcode Штрихкод
     * @return Найденный товар или null
     */
    suspend fun findProductByBarcode(barcode: String): Product? {
        return try {
            Timber.d("Finding product by barcode: $barcode")
            val product = productRepository.findProductByBarcode(barcode)

            if (product != null) {
                Timber.d("Found product: ${product.name} by barcode: $barcode")
            } else {
                Timber.d("Product not found by barcode: $barcode")
            }

            product
        } catch (e: Exception) {
            Timber.e(e, "Error finding product by barcode: $barcode")
            null
        }
    }

    /**
     * Загружает ячейки по запросу и опционально фильтрует по зоне
     * @param query Строка запроса
     * @param zone Зона для фильтрации
     */
    suspend fun loadBins(query: String? = null, zone: String? = null) {
        try {
            Timber.d("Loading bins with query: $query, zone: $zone")
            val loadedBins = binRepository.getBins(query, zone)
            _bins.value = loadedBins
            Timber.d("Loaded ${loadedBins.size} bins")
        } catch (e: Exception) {
            Timber.e(e, "Error loading bins")
            // В случае ошибки сохраняем пустой список
            _bins.value = emptyList()
        }
    }

    /**
     * Ищет ячейку по коду
     * @param code Код ячейки
     * @return Найденная ячейка или null
     */
    suspend fun findBinByCode(code: String): BinX? {
        return try {
            Timber.d("Finding bin by code: $code")
            val bin = binRepository.getBinByCode(code)

            if (bin != null) {
                Timber.d("Found bin: ${bin.code} by code: $code")
            } else {
                Timber.d("Bin not found by code: $code")
            }

            bin
        } catch (e: Exception) {
            Timber.e(e, "Error finding bin by code: $code")
            null
        }
    }

    /**
     * Загружает паллеты по запросу
     * @param query Строка запроса
     */
    suspend fun loadPallets(query: String? = null) {
        try {
            Timber.d("Loading pallets with query: $query")
            val loadedPallets = palletRepository.getPallets(query)
            _pallets.value = loadedPallets
            Timber.d("Loaded ${loadedPallets.size} pallets")
        } catch (e: Exception) {
            Timber.e(e, "Error loading pallets")
            // В случае ошибки сохраняем пустой список
            _pallets.value = emptyList()
        }
    }

    /**
     * Ищет паллету по коду
     * @param code Код паллеты
     * @return Найденная паллета или null
     */
    suspend fun findPalletByCode(code: String): Pallet? {
        return try {
            Timber.d("Finding pallet by code: $code")
            val pallet = palletRepository.getPalletByCode(code)

            if (pallet != null) {
                Timber.d("Found pallet: ${pallet.code} by code: $code")
            } else {
                Timber.d("Pallet not found by code: $code")
            }

            pallet
        } catch (e: Exception) {
            Timber.e(e, "Error finding pallet by code: $code")
            null
        }
    }

    /**
     * Создает новую паллету
     * @return Результат с созданной паллетой или ошибкой
     */
    suspend fun createPallet(): Result<Pallet> {
        return try {
            Timber.d("Creating new pallet")
            val result = palletRepository.createPallet()

            if (result.isSuccess) {
                // Обновляем кэш паллет
                val newPallet = result.getOrNull()
                if (newPallet != null) {
                    _pallets.value = _pallets.value + newPallet
                    Timber.d("New pallet created: ${newPallet.code}")
                }
            } else {
                Timber.w("Failed to create pallet: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Error creating pallet")
            Result.failure(e)
        }
    }

    /**
     * Закрывает паллету по коду
     * @param code Код паллеты
     * @return Результат операции
     */
    suspend fun closePallet(code: String): Result<Boolean> {
        return try {
            Timber.d("Closing pallet: $code")
            val result = palletRepository.closePallet(code)

            if (result.isSuccess && result.getOrNull() == true) {
                // Обновляем кэш паллет
                val pallet = _pallets.value.find { it.code == code }
                if (pallet != null) {
                    val updatedPallet = pallet.copy(isClosed = true)
                    _pallets.value = _pallets.value.map { if (it.code == code) updatedPallet else it }
                    Timber.d("Pallet closed: $code")
                }
            } else {
                Timber.w("Failed to close pallet: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Error closing pallet: $code")
            Result.failure(e)
        }
    }

    /**
     * Отправляет на печать этикетку паллеты
     * @param code Код паллеты
     * @return Результат операции
     */
    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return try {
            Timber.d("Printing label for pallet: $code")
            val result = palletRepository.printPalletLabel(code)

            if (result.isSuccess) {
                Timber.d("Label printed for pallet: $code")
            } else {
                Timber.w("Failed to print label for pallet: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Error printing label for pallet: $code")
            Result.failure(e)
        }
    }

    /**
     * Очищает все кэши
     */
    fun clearCache() {
        Timber.d("Clearing all caches")
        _products.value = emptyList()
        _bins.value = emptyList()
        _pallets.value = emptyList()
    }
}