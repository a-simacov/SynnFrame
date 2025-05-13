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

/**
 * Утилитные методы для шагов визарда действий
 */
object WizardUtils {
    /**
     * Форматирует количество для отображения
     * @param value Значение для форматирования
     * @return Отформатированная строка с числом
     */
    fun formatQuantity(value: Float): String {
        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Округляет значение до 3 знаков после запятой
     * @param value Значение для округления
     * @return Округленное значение
     */
    fun roundToThreeDecimals(value: Float): Float {
        return (round(value * 1000) / 1000).toFloat()
    }

    /**
     * Создает TaskProduct из Product со стандартными параметрами
     * @param product Исходный Product
     * @param quantity Количество (по умолчанию 0)
     * @param status Статус товара (по умолчанию STANDARD)
     * @param expirationDate Срок годности (опционально)
     * @return Созданный TaskProduct
     */
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

    /**
     * Проверяет, является ли объект валидным для передачи между шагами
     * @param result Объект для проверки
     * @return true, если объект валидный, иначе false
     */
    fun isValidStepResult(result: Any?): Boolean {
        return when (result) {
            is Product, is TaskProduct, is Pallet, is BinX -> true
            else -> false
        }
    }

    /**
     * Преобразует строку в Float с обработкой ошибок
     * @param input Строка для разбора
     * @return Числовое значение или 0, если разбор не удался
     */
    fun parseQuantityInput(input: String): Float {
        return try {
            // Заменяем запятую на точку перед парсингом
            input.replace(",", ".").toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Обрабатывает ввод количества, применяя форматирование
     * @param input Ввод пользователя
     * @return Обработанную строку
     */
    fun processQuantityInput(input: String): String {
        return when {
            // Если поле пустое, устанавливаем "0"
            input.isEmpty() -> "0"
            // Если только точка, преобразуем в "0."
            input == "." -> "0."
            // Если начинается с точки, добавляем ведущий ноль
            input.startsWith(".") -> "0$input"
            // Остальные значения оставляем как есть
            else -> input
        }
    }

    /**
     * Ищет объект TaskProduct в карте результатов
     * @param results Карта результатов
     * @return Найденный TaskProduct или null
     */
    fun findTaskProduct(results: Map<String, Any>): TaskProduct? {
        // Сначала ищем по специальному ключу
        val directResult = results["lastTaskProduct"] as? TaskProduct
        if (directResult != null) return directResult

        // Затем ищем среди всех значений
        return results.values.filterIsInstance<TaskProduct>().firstOrNull()
    }

    /**
     * Ищет объект Product в карте результатов
     * @param results Карта результатов
     * @return Найденный Product или null
     */
    fun findProduct(results: Map<String, Any>): Product? {
        // Сначала ищем по специальному ключу
        val directResult = results["lastProduct"] as? Product
        if (directResult != null) return directResult

        // Затем ищем в TaskProduct, если есть
        val taskProduct = findTaskProduct(results)
        if (taskProduct != null) return taskProduct.product

        // Наконец, ищем среди всех значений
        return results.values.filterIsInstance<Product>().firstOrNull()
    }

    /**
     * Ищет объект Pallet в карте результатов
     * @param results Карта результатов
     * @return Найденный Pallet или null
     */
    fun findPallet(results: Map<String, Any>): Pallet? {
        // Сначала ищем по специальному ключу
        val directResult = results["lastPallet"] as? Pallet
        if (directResult != null) return directResult

        // Затем ищем среди всех значений
        return results.values.filterIsInstance<Pallet>().firstOrNull()
    }

    /**
     * Ищет объект BinX в карте результатов
     * @param results Карта результатов
     * @return Найденный BinX или null
     */
    fun findBin(results: Map<String, Any>): BinX? {
        // Сначала ищем по специальному ключу
        val directResult = results["lastBin"] as? BinX
        if (directResult != null) return directResult

        // Затем ищем среди всех значений
        return results.values.filterIsInstance<BinX>().firstOrNull()
    }

    /**
     * Обогащает карту результатов специальными ключами для облегчения доступа к данным
     * @param results Исходная карта результатов
     * @return Обогащенная карта результатов
     */
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