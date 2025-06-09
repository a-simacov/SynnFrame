package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.BarcodeProcessResult
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class ObjectSearchService(
    productUseCases: ProductUseCases,
    validationService: ValidationService
) {
    private val handlerFactory = FieldHandlerFactory(validationService, productUseCases)

    // Переменные для отслеживания времени последнего сканирования
    private val MIN_SCAN_INTERVAL = 1000L // Интервал между сканированиями (миллисекунды)
    private var lastScanTime = 0L        // Время последнего сканирования
    private var lastScannedBarcode = ""  // Последний отсканированный штрихкод

    // Кэш результатов валидации для предотвращения повторных запросов
    private val validationCache = ConcurrentHashMap<String, BarcodeProcessResult<Any>>()

    // Флаг для отслеживания активных запросов по типам полей
    private val activeRequests = ConcurrentHashMap<FactActionField, Long>()

    suspend fun handleBarcode(
        state: ActionWizardState,
        barcode: String
    ): BarcodeProcessResult<Any> {
        if (state.error != null) {
            Timber.d("Clearing cache when error exists before new scan")
            validationCache.clear()
        }

        val currentTime = System.currentTimeMillis()
        val currentStep = state.getCurrentStep()

        if (currentStep == null) {
            Timber.w("Current step not defined for barcode: $barcode")
            return BarcodeProcessResult.error("Failed to determine current step for search")
        }

        val fieldType = currentStep.factActionField

        // Проверяем, прошло ли достаточно времени с момента последнего сканирования
        // и не совпадает ли текущий штрихкод с предыдущим
        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL && lastScannedBarcode == barcode) {
            Timber.d("Ignoring repeated barcode scan: $barcode (less than ${MIN_SCAN_INTERVAL}ms passed)")
            return BarcodeProcessResult.ignored()
        }

        // Проверяем, не выполняется ли уже запрос для данного типа поля
        val lastRequestTime = activeRequests[fieldType]
        if (lastRequestTime != null && currentTime - lastRequestTime < 5000) {
            Timber.d("Ignoring request for field $fieldType: previous request is still in progress")
            return BarcodeProcessResult.ignored()
        }

        val cacheKey = "${fieldType}_$barcode"
        val cachedResult = validationCache[cacheKey]
        if (cachedResult != null) {
            Timber.d("Using cached result for $cacheKey")
            return cachedResult
        }

        lastScanTime = currentTime
        lastScannedBarcode = barcode

        // Устанавливаем флаг активного запроса
        activeRequests[fieldType] = currentTime

        if (barcode.isBlank()) {
            return BarcodeProcessResult.error<Any>("Empty barcode").also {
                // Очищаем флаг активного запроса
                activeRequests.remove(fieldType)
            }
        }

        Timber.d("Processing barcode: $barcode for step: ${currentStep.name}")

        try {
            val handler = handlerFactory.createHandlerForStep(currentStep)
                ?: return BarcodeProcessResult.error<Any>("Unsupported step type: ${currentStep.factActionField}")
                    .also {
                        activeRequests.remove(fieldType)
                    }

            val result = handler.handleBarcode(barcode, state, currentStep)

            val finalResult = if (result.isSuccess()) {
                val data = result.getFoundData()
                    ?: return BarcodeProcessResult.error<Any>("Failed to get object data")
                BarcodeProcessResult.success(data)
            } else {
                BarcodeProcessResult.error(
                    result.getErrorMessage() ?: "Failed to find object by barcode: $barcode"
                )
            }

            // Сохраняем результат в кэш
            validationCache[cacheKey] = finalResult

            // Очищаем флаг активного запроса
            activeRequests.remove(fieldType)

            return finalResult
        } catch (e: Exception) {
            Timber.e(e, "Error processing barcode: $barcode")

            // Очищаем флаг активного запроса
            activeRequests.remove(fieldType)

            return BarcodeProcessResult.error<Any>("Error processing barcode: ${e.message}")
        }
    }

    fun clearCache() {
        validationCache.clear()
        activeRequests.clear()
    }
}