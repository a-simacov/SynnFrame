package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.repository.WizardProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Сервис для кэширования данных при работе с мастером
 */
class FactLineDataCacheService(
    private val wizardProductRepository: WizardProductRepository,
    private val wizardBinRepository: WizardBinRepository,
    private val wizardPalletRepository: WizardPalletRepository
) {
    // Кэш для продуктов
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // Кэш для ячеек
    private val _bins = MutableStateFlow<List<BinX>>(emptyList())
    val bins: StateFlow<List<BinX>> = _bins.asStateFlow()

    // Кэш для паллет
    private val _pallets = MutableStateFlow<List<Pallet>>(emptyList())
    val pallets: StateFlow<List<Pallet>> = _pallets.asStateFlow()

    /**
     * Загрузка продуктов с фильтрацией
     */
    suspend fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        val products = if (planProductIds != null && planProductIds.isNotEmpty()) {
            wizardProductRepository.getProductsByIds(planProductIds)
        } else {
            wizardProductRepository.getProducts(query)
        }

        _products.value = products
    }

    /**
     * Загрузка ячеек с фильтрацией по зоне
     */
    suspend fun loadBins(query: String? = null, zone: String? = null) {
        val bins = wizardBinRepository.getBins(query, zone)
        _bins.value = bins
    }

    /**
     * Загрузка паллет с фильтрацией
     */
    suspend fun loadPallets(query: String? = null) {
        val pallets = wizardPalletRepository.getPallets(query)
        _pallets.value = pallets
    }

    /**
     * Поиск продукта по штрихкоду
     */
    suspend fun findProductByBarcode(barcode: String): Product? {
        return wizardProductRepository.findProductByBarcode(barcode)
    }

    /**
     * Поиск ячейки по коду
     */
    suspend fun findBinByCode(code: String): BinX? {
        return wizardBinRepository.getBinByCode(code)
    }

    /**
     * Поиск паллеты по коду
     */
    suspend fun findPalletByCode(code: String): Pallet? {
        return wizardPalletRepository.getPalletByCode(code)
    }

    /**
     * Создание новой паллеты
     */
    suspend fun createPallet(): Result<Pallet> {
        val result = wizardPalletRepository.createPallet()

        // Обновляем кэш паллет при успешном создании
        if (result.isSuccess) {
            val newPallet = result.getOrNull()
            if (newPallet != null) {
                _pallets.value = _pallets.value + newPallet
            }
        }

        return result
    }

    /**
     * Закрытие паллеты
     */
    suspend fun closePallet(code: String): Result<Boolean> {
        val result = wizardPalletRepository.closePallet(code)

        // Обновляем кэш паллет при успешном закрытии
        if (result.isSuccess && result.getOrNull() == true) {
            _pallets.value = _pallets.value.map { pallet ->
                if (pallet.code == code) pallet.copy(isClosed = true) else pallet
            }
        }

        return result
    }

    /**
     * Печать этикетки паллеты
     */
    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return wizardPalletRepository.printPalletLabel(code)
    }

    /**
     * Очистка кэша
     */
    fun clearCache() {
        _products.value = emptyList()
        _bins.value = emptyList()
        _pallets.value = emptyList()
    }
}