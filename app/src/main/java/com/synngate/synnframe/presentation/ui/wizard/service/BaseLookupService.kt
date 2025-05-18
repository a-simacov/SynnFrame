package com.synngate.synnframe.presentation.ui.wizard.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Базовый класс для сервисов поиска сущностей.
 * Предоставляет общую реализацию для работы со штрихкодами и поиском.
 *
 * @param T тип сущности, с которой работает сервис
 */
abstract class BaseLookupService<T> : EntityLookupService<T> {

    /**
     * Обрабатывает штрихкод и ищет соответствующую сущность.
     */
    override suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String?,
        onResult: (found: Boolean, data: T?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            Timber.d("BaseLookupService: начало обработки штрихкода: $barcode, ожидаемый: $expectedBarcode")

            // Если указан ожидаемый штрихкод и он не совпадает с фактическим
            if (expectedBarcode != null && barcode != expectedBarcode) {
                Timber.d("BaseLookupService: штрихкод не соответствует ожидаемому")
                onResult(false, null)
                return
            }

            withContext(Dispatchers.IO) {
                // Сначала ищем в локальных данных задачи
                Timber.d("BaseLookupService: поиск в контексте")
                val entityFromContext = findEntityInContext(barcode)
                if (entityFromContext != null) {
                    Timber.d("BaseLookupService: найдено в контексте: ${entityFromContext.javaClass.simpleName}")
                    onResult(true, entityFromContext)
                    return@withContext
                }

                // Затем ищем в репозитории
                Timber.d("BaseLookupService: поиск в репозитории")
                val entityFromRepository = findEntityInRepository(barcode)
                if (entityFromRepository != null) {
                    Timber.d("BaseLookupService: найдено в репозитории: ${entityFromRepository.javaClass.simpleName}")
                    onResult(true, entityFromRepository)
                    return@withContext
                }

                // Если не нашли ни там, ни там, пробуем создать локальную сущность
                Timber.d("BaseLookupService: создание локальной сущности")
                val localEntity = createLocalEntity(barcode)
                if (localEntity != null) {
                    Timber.d("BaseLookupService: создана локальная сущность: ${localEntity.javaClass.simpleName}")
                    onResult(true, localEntity)
                    return@withContext
                }

                // Ничего не нашли
                Timber.d("BaseLookupService: сущность не найдена для штрихкода: $barcode")
                onResult(false, null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
            onError("Ошибка при обработке штрихкода: ${e.message}")
        }
    }

    /**
     * Ищет сущности по строке запроса.
     */
    override suspend fun searchEntities(query: String, additionalParams: Map<String, Any>): List<T> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<T>()

                // Сначала ищем в контексте текущей задачи
                val entitiesFromContext = searchEntitiesInContext(query, additionalParams)
                results.addAll(entitiesFromContext)

                // Если нашли достаточно, возвращаем результат
                if (results.size >= getMaxSearchResults()) {
                    return@withContext results.take(getMaxSearchResults())
                }

                // Иначе ищем в репозитории
                val entitiesFromRepository = searchEntitiesInRepository(query, additionalParams)

                // Отфильтровываем уже найденные сущности, чтобы избежать дубликатов
                val existingIds = results.mapNotNull { getEntityId(it) }.toSet()
                val newEntities = entitiesFromRepository.filter {
                    val id = getEntityId(it)
                    id == null || id !in existingIds
                }

                results.addAll(newEntities)

                return@withContext results.take(getMaxSearchResults())
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске: query=$query, params=$additionalParams")
                emptyList()
            }
        }
    }

    /**
     * Ищет сущность в контексте текущей задачи.
     */
    protected abstract suspend fun findEntityInContext(barcode: String): T?

    /**
     * Ищет сущность в репозитории.
     */
    protected abstract suspend fun findEntityInRepository(barcode: String): T?

    /**
     * Создает локальную сущность по штрихкоду.
     * Используется, когда сущность не найдена ни в контексте, ни в репозитории.
     */
    protected abstract suspend fun createLocalEntity(barcode: String): T?

    /**
     * Ищет сущности в контексте текущей задачи.
     */
    protected abstract suspend fun searchEntitiesInContext(
        query: String,
        additionalParams: Map<String, Any>
    ): List<T>

    /**
     * Ищет сущности в репозитории.
     */
    protected abstract suspend fun searchEntitiesInRepository(
        query: String,
        additionalParams: Map<String, Any>
    ): List<T>

    /**
     * Возвращает идентификатор сущности для предотвращения дубликатов.
     */
    protected abstract fun getEntityId(entity: T): String?

    /**
     * Возвращает максимальное количество результатов поиска.
     */
    protected open fun getMaxSearchResults(): Int = 20
}