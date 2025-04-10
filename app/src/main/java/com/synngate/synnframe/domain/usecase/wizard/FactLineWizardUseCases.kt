package com.synngate.synnframe.domain.usecase.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import com.synngate.synnframe.domain.service.FactLineDataCacheService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import kotlinx.coroutines.flow.StateFlow

class FactLineWizardUseCases(
    private val dataCacheService: FactLineDataCacheService,
    private val taskXUseCases: TaskXUseCases,
    private val taskTypeXRepository: TaskTypeXRepository,
    private val factLineWizardViewModel: FactLineWizardViewModel
) : BaseUseCase {

    // Методы для работы с продуктами
    suspend fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        dataCacheService.loadProducts(query, planProductIds)
    }

    suspend fun findProductByBarcode(barcode: String): Product? {
        return dataCacheService.findProductByBarcode(barcode)
    }

    fun getProductsFlow(): StateFlow<List<Product>> {
        return dataCacheService.products
    }

    // Методы для работы с ячейками
    suspend fun loadBins(query: String? = null, zone: String? = null) {
        dataCacheService.loadBins(query, zone)
    }

    suspend fun findBinByCode(code: String): BinX? {
        return dataCacheService.findBinByCode(code)
    }

    fun getBinsFlow(): StateFlow<List<BinX>> {
        return dataCacheService.bins
    }

    // Методы для работы с паллетами
    suspend fun loadPallets(query: String? = null) {
        dataCacheService.loadPallets(query)
    }

    suspend fun findPalletByCode(code: String): Pallet? {
        return dataCacheService.findPalletByCode(code)
    }

    fun getPalletsFlow(): StateFlow<List<Pallet>> {
        return dataCacheService.pallets
    }

    suspend fun createPallet(): Result<Pallet> {
        return dataCacheService.createPallet()
    }

    suspend fun closePallet(code: String): Result<Boolean> {
        return dataCacheService.closePallet(code)
    }

    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return dataCacheService.printPalletLabel(code)
    }

    // Методы для работы с визардом
    suspend fun createFactLine(wizardState: FactLineWizardState): Result<FactLineX> {
        val factLine = wizardState.createFactLine()
            ?: return Result.failure(IllegalStateException("Не удалось создать строку факта"))

        return Result.success(factLine)
    }

    suspend fun addFactLine(factLine: FactLineX): Result<TaskX> {
        return taskXUseCases.addFactLine(factLine)
    }

    fun clearCache() {
        dataCacheService.clearCache()
    }

    suspend fun getTaskType(taskTypeId: String): TaskTypeX? {
        return taskTypeXRepository.getTaskTypeById(taskTypeId)
    }

    fun getWizardViewModel(): FactLineWizardViewModel {
        return factLineWizardViewModel
    }
}