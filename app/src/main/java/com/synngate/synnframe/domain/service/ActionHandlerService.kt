package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import java.time.LocalDate

/**
 * Сервис для обработки действий в визарде
 */
class ActionHandlerService(
    private val factLineWizardUseCases: FactLineWizardUseCases
) {
    /**
     * Обработка выбора товара
     */
    fun handleProductSelection(product: Product): TaskProduct {
        return TaskProduct(
            product = product,
            quantity = 1f,
            status = ProductStatus.STANDARD
        )
    }

    /**
     * Обновление количества товара
     */
    fun updateProductQuantity(taskProduct: TaskProduct, quantity: Float): TaskProduct {
        return taskProduct.copy(quantity = quantity)
    }

    /**
     * Обновление срока годности товара
     */
    fun updateProductExpirationDate(taskProduct: TaskProduct, date: LocalDate): TaskProduct {
        return taskProduct.copy(expirationDate = date)
    }

    /**
     * Обновление статуса товара
     */
    fun updateProductStatus(taskProduct: TaskProduct, status: ProductStatus): TaskProduct {
        return taskProduct.copy(status = status)
    }

    /**
     * Создание паллеты с обработкой ошибок
     */
    suspend fun createPallet(): Result<Pallet> {
        return factLineWizardUseCases.createPallet()
    }

    /**
     * Закрытие паллеты с обработкой ошибок
     */
    suspend fun closePallet(palletCode: String): Result<Boolean> {
        return factLineWizardUseCases.closePallet(palletCode)
    }

    /**
     * Печать этикетки паллеты с обработкой ошибок
     */
    suspend fun printPalletLabel(palletCode: String): Result<Boolean> {
        return factLineWizardUseCases.printPalletLabel(palletCode)
    }

    /**
     * Обновление модели результатов на основе типа целевого поля и значения
     */
    fun updateResultsByTargetField(
        results: WizardResultModel,
        targetFieldType: TaskXLineFieldType,
        value: Any
    ): WizardResultModel {
        return when (targetFieldType) {
            TaskXLineFieldType.STORAGE_PRODUCT -> {
                if (value is TaskProduct) {
                    results.withStorageProduct(value)
                } else {
                    results
                }
            }
            TaskXLineFieldType.STORAGE_PALLET -> {
                if (value is Pallet) {
                    results.withStoragePallet(value)
                } else {
                    results
                }
            }
            TaskXLineFieldType.PLACEMENT_PALLET -> {
                if (value is Pallet) {
                    results.withPlacementPallet(value)
                } else {
                    results
                }
            }
            TaskXLineFieldType.PLACEMENT_BIN -> {
                if (value is BinX) {
                    results.withPlacementBin(value)
                } else {
                    results
                }
            }
            TaskXLineFieldType.WMS_ACTION -> {
                if (value is WmsAction) {
                    results.withWmsAction(value)
                } else {
                    results
                }
            }
        }
    }
}