package com.synngate.synnframe.presentation.ui.wizard.action.utils

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import timber.log.Timber

/**
 * Утилитный класс для структурированного логирования в визарде
 */
object WizardLogger {
    private const val TAG = "WizardLogger"

    /**
     * Уровни детализации логирования
     */
    enum class LogLevel {
        MINIMAL,    // Только критические сообщения
        NORMAL,     // Обычный уровень логирования
        VERBOSE     // Подробное логирование для отладки
    }

    /**
     * Текущий уровень логирования
     */
    var currentLogLevel: LogLevel = LogLevel.NORMAL

    /**
     * Логирует содержимое карты результатов
     * @param tag Метка логирования
     * @param results Карта результатов для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logResults(tag: String, results: Map<String, Any>, level: LogLevel = LogLevel.VERBOSE) {
        if (level.ordinal > currentLogLevel.ordinal) return

        Timber.d("$tag: Results (size=${results.size})")
        if (currentLogLevel == LogLevel.VERBOSE) {
            results.entries.forEach { (key, value) ->
                Timber.d("$tag:   $key -> ${value?.javaClass?.simpleName}")
            }
        }
    }

    /**
     * Логирует значения специальных ключей
     * @param tag Метка логирования
     * @param results Карта результатов для проверки
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logSpecialKeys(tag: String, results: Map<String, Any>, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        val specialKeys = listOf("lastProduct", "lastTaskProduct", "lastPallet", "lastBin")
        val foundKeys = specialKeys.filter { it in results }

        if (foundKeys.isNotEmpty()) {
            Timber.d("$tag: Found special keys: $foundKeys")

            if (currentLogLevel == LogLevel.VERBOSE) {
                foundKeys.forEach { key ->
                    val value = results[key]
                    Timber.d("$tag:   $key = ${value?.javaClass?.simpleName}")
                }
            }
        } else {
            Timber.d("$tag: No special keys found in results")
        }
    }

    /**
     * Логирует шаг в работе визарда
     * @param tag Метка логирования
     * @param stepName Название шага
     * @param message Сообщение для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logStep(tag: String, stepName: String, message: String, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        Timber.d("$tag: [Step: $stepName] $message")
    }

    /**
     * Логирует информацию о продукте
     * @param tag Метка логирования
     * @param product Продукт для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logProduct(tag: String, product: Product?, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        if (product == null) {
            Timber.d("$tag: Product is null")
            return
        }

        Timber.d("$tag: Product: id=${product.id}, name=${product.name}")
    }

    /**
     * Логирует информацию о TaskProduct
     * @param tag Метка логирования
     * @param taskProduct TaskProduct для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logTaskProduct(tag: String, taskProduct: TaskProduct?, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        if (taskProduct == null) {
            Timber.d("$tag: TaskProduct is null")
            return
        }

        Timber.d("$tag: TaskProduct: product=${taskProduct.product.name}, " +
                "quantity=${taskProduct.quantity}, status=${taskProduct.status}")
    }

    /**
     * Логирует информацию о паллете
     * @param tag Метка логирования
     * @param pallet Паллета для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logPallet(tag: String, pallet: Pallet?, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        if (pallet == null) {
            Timber.d("$tag: Pallet is null")
            return
        }

        Timber.d("$tag: Pallet: code=${pallet.code}, isClosed=${pallet.isClosed}")
    }

    /**
     * Логирует информацию о ячейке
     * @param tag Метка логирования
     * @param bin Ячейка для логирования
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logBin(tag: String, bin: BinX?, level: LogLevel = LogLevel.NORMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        if (bin == null) {
            Timber.d("$tag: Bin is null")
            return
        }

        Timber.d("$tag: Bin: code=${bin.code}, zone=${bin.zone}")
    }

    /**
     * Логирует обработку ошибки
     * @param tag Метка логирования
     * @param e Исключение
     * @param operation Название операции, при которой произошла ошибка
     * @param level Минимальный уровень для вывода этого лога
     */
    fun logError(tag: String, e: Exception, operation: String, level: LogLevel = LogLevel.MINIMAL) {
        if (level.ordinal > currentLogLevel.ordinal) return

        Timber.e(e, "$tag: Error during $operation: ${e.message}")
    }
}