package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandler
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис для поиска объектов по штрихкодам с улучшенной защитой от дублирующих запросов
 */
class ObjectSearchService(
    productUseCases: ProductUseCases,
    validationService: ValidationService
) {
    // Фабрика обработчиков полей
    private val handlerFactory = FieldHandlerFactory(validationService, productUseCases)

    // Переменные для отслеживания времени последнего сканирования
    private val MIN_SCAN_INTERVAL = 1000L // Интервал между сканированиями (миллисекунды)
    private var lastScanTime = 0L        // Время последнего сканирования
    private var lastScannedBarcode = ""  // Последний отсканированный штрихкод

    // Кэш результатов валидации для предотвращения повторных запросов
    private val validationCache = ConcurrentHashMap<String, Triple<Boolean, Any?, String?>>()

    // Флаг для отслеживания активных запросов по типам полей
    private val activeRequests = ConcurrentHashMap<FactActionField, Long>()

    /**
     * Обрабатывает штрих-код в контексте текущего шага
     * @return Тройка (успех операции, найденный объект или null, сообщение об ошибке или null)
     */
    suspend fun handleBarcode(state: ActionWizardState, barcode: String): Triple<Boolean, Any?, String?> {
        if (state.error != null) {
            Timber.d("Очистка кэша при наличии ошибки перед новым сканированием")
            validationCache.clear()
        }

        val currentTime = System.currentTimeMillis()
        val currentStep = state.getCurrentStep()

        // Выходим, если текущий шаг не определен
        if (currentStep == null) {
            Timber.w("Текущий шаг не определен для штрих-кода: $barcode")
            return Triple(false, null, "Не удалось определить текущий шаг для поиска")
        }

        val fieldType = currentStep.factActionField

        // Проверяем, прошло ли достаточно времени с момента последнего сканирования
        // и не совпадает ли текущий штрихкод с предыдущим
        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL && lastScannedBarcode == barcode) {
            Timber.d("Игнорирование повторного сканирования штрихкода: $barcode (прошло менее ${MIN_SCAN_INTERVAL}мс)")
            return Triple(false, null, null)
        }

        // Проверяем, не выполняется ли уже запрос для данного типа поля
        val lastRequestTime = activeRequests[fieldType]
        if (lastRequestTime != null && currentTime - lastRequestTime < 5000) {
            Timber.d("Игнорирование запроса для поля $fieldType: предыдущий запрос еще выполняется")
            return Triple(false, null, null)
        }

        // Проверяем кэш результатов валидации
        val cacheKey = "${fieldType}_$barcode"
        val cachedResult = validationCache[cacheKey]
        if (cachedResult != null) {
            Timber.d("Использование кешированного результата для $cacheKey")
            return cachedResult
        }

        // Обновляем информацию о последнем сканировании
        lastScanTime = currentTime
        lastScannedBarcode = barcode

        // Устанавливаем флаг активного запроса
        activeRequests[fieldType] = currentTime

        if (barcode.isBlank()) {
            return Triple(false, null, "Пустой штрихкод").also {
                // Очищаем флаг активного запроса
                activeRequests.remove(fieldType)
            }
        }

        Timber.d("Обработка штрих-кода: $barcode для шага: ${currentStep.name}")

        try {
            // Используем метод createHandlerForStep для получения обработчика для текущего шага
            val handler = handlerFactory.createHandlerForStep(currentStep)
                ?: return Triple(false, null, "Неподдерживаемый тип шага: ${currentStep.factActionField}").also {
                    activeRequests.remove(fieldType)
                }

            // Обрабатываем штрихкод с помощью обработчика
            val result = handler.handleBarcode(barcode, state, currentStep)

            val finalResult = if (result.first != null) {
                Triple(true, result.first, null)
            } else {
                Triple(false, null, result.second ?: "Не удалось найти объект по штрих-коду: $barcode")
            }

            // Сохраняем результат в кэш
            validationCache[cacheKey] = finalResult

            // Очищаем флаг активного запроса
            activeRequests.remove(fieldType)

            return finalResult
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")

            // Очищаем флаг активного запроса
            activeRequests.remove(fieldType)

            return Triple(false, null, "Ошибка при обработке штрих-кода: ${e.message}")
        }
    }

    /**
     * Валидирует объект с помощью соответствующего обработчика
     */
    suspend fun <T : Any> validateObject(obj: T, state: ActionWizardState): Pair<Boolean, String?> {
        val currentStep = state.getCurrentStep() ?: return Pair(false, "Шаг не найден")

        val handler = handlerFactory.createHandlerForObject(obj)
            ?: return Pair(false, "Неподдерживаемый тип объекта: ${obj.javaClass.simpleName}")

        @Suppress("UNCHECKED_CAST")
        return (handler as FieldHandler<T>).validateObject(obj, state, currentStep)
    }

    /**
     * Очищает кэш и состояние обработки
     */
    fun clearCache() {
        validationCache.clear()
        activeRequests.clear()
    }
}