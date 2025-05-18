package com.synngate.synnframe.presentation.ui.wizard.service

/**
 * Обобщенный интерфейс для сервисов поиска сущностей.
 * Позволяет искать сущности по штрихкоду или строке запроса.
 *
 * @param T тип сущности, с которой работает сервис
 */
interface EntityLookupService<T> {
    /**
     * Обрабатывает штрихкод и ищет соответствующую сущность.
     *
     * @param barcode Штрихкод для поиска
     * @param expectedBarcode Ожидаемый штрихкод (если указан, и не совпадает с barcode, вернет false)
     * @param onResult Обратный вызов с результатом поиска (найдено/не найдено, данные)
     * @param onError Обратный вызов при ошибке поиска
     */
    suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String? = null,
        onResult: (found: Boolean, data: T?) -> Unit,
        onError: (message: String) -> Unit
    )

    /**
     * Ищет сущности по строке запроса.
     *
     * @param query Строка запроса для поиска
     * @param additionalParams Дополнительные параметры для поиска, специфичные для реализации
     * @return Список найденных сущностей
     */
    suspend fun searchEntities(query: String, additionalParams: Map<String, Any> = emptyMap()): List<T>
}