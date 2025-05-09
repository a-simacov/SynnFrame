package com.synngate.synnframe.presentation.ui.wizard.service

import timber.log.Timber

/**
 * Интерфейс сервиса для сканирования штрих-кодов
 */
interface BarcodeScanningService {
    /**
     * Обрабатывает отсканированный штрих-код
     * @param barcode отсканированный штрих-код
     * @param expectedBarcode ожидаемый штрих-код (опционально)
     * @param onResult обработчик результата (найдено/не найдено, данные)
     * @param onError обработчик ошибок
     */
    suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String? = null,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    )
}

/**
 * Базовая реализация сервиса сканирования штрих-кодов
 */
abstract class BaseBarcodeScanningService : BarcodeScanningService {
    override suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String?,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            // Проверяем соответствие ожидаемому штрих-коду, если он указан
            if (expectedBarcode != null && barcode != expectedBarcode) {
                Timber.w("Несоответствие штрихкода: ожидался $expectedBarcode, получен $barcode")
                onResult(false, null)
                return
            }

            // Делегируем конкретную логику поиска подклассам
            findItemByBarcode(barcode, onResult, onError)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
            onError("Ошибка при обработке штрихкода: ${e.message}")
        }
    }

    /**
     * Находит элемент по штрих-коду.
     * Подклассы должны реализовать этот метод для конкретной логики поиска.
     */
    protected abstract suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    )
}