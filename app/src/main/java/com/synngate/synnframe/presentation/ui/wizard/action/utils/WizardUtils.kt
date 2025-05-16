package com.synngate.synnframe.presentation.ui.wizard.action.utils

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.round

object WizardUtils {

    fun formatQuantity(value: Float): String {
        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun roundToThreeDecimals(value: Float): Float {
        return (round(value * 1000) / 1000).toFloat()
    }

    fun createTaskProductFromProduct(
        product: Product,
        quantity: Float = 0f,
        status: ProductStatus = ProductStatus.STANDARD,
        expirationDate: LocalDateTime? = null
    ): TaskProduct {
        val actualExpirationDate = if (product.accountingModel == AccountingModel.BATCH && expirationDate != null) {
            expirationDate
        } else {
            LocalDateTime.of(1970, 1, 1, 0, 0)
        }

        return TaskProduct(
            product = product,
            quantity = quantity,
            status = status,
            expirationDate = actualExpirationDate
        )
    }

    fun isValidStepResult(result: Any?): Boolean {
        return when (result) {
            is Product, is TaskProduct, is Pallet, is BinX -> true
            else -> false
        }
    }

    fun parseQuantityInput(input: String): Float {
        return try {
            input.replace(",", ".").toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun processQuantityInput(input: String): String {
        return when {
            input.isEmpty() -> "0"
            input == "." -> "0."
            input.startsWith(".") -> "0$input"
            else -> input
        }
    }

    fun findTaskProduct(results: Map<String, Any>): TaskProduct? {
        val directResult = results["lastTaskProduct"] as? TaskProduct
        if (directResult != null) return directResult

        return results.values.filterIsInstance<TaskProduct>().firstOrNull()
    }

    fun findProduct(results: Map<String, Any>): Product? {
        val directResult = results["lastProduct"] as? Product
        if (directResult != null) return directResult

        val taskProduct = findTaskProduct(results)
        if (taskProduct != null) return taskProduct.product

        return results.values.filterIsInstance<Product>().firstOrNull()
    }

    fun findPallet(results: Map<String, Any>): Pallet? {
        val directResult = results["lastPallet"] as? Pallet
        if (directResult != null) return directResult

        return results.values.filterIsInstance<Pallet>().firstOrNull()
    }

    fun findBin(results: Map<String, Any>): BinX? {
        val directResult = results["lastBin"] as? BinX
        if (directResult != null) return directResult

        return results.values.filterIsInstance<BinX>().firstOrNull()
    }

    fun enrichResultsData(results: Map<String, Any>): Map<String, Any> {
        val enriched = results.toMutableMap()

        // Добавляем специальные ключи для быстрого доступа
        findTaskProduct(results)?.let {
            enriched["lastTaskProduct"] = it
            enriched["lastProduct"] = it.product
        } ?: findProduct(results)?.let {
            enriched["lastProduct"] = it
        }

        findPallet(results)?.let { enriched["lastPallet"] = it }
        findBin(results)?.let { enriched["lastBin"] = it }

        return enriched
    }
}