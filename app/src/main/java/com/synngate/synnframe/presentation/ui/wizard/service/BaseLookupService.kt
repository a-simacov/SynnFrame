package com.synngate.synnframe.presentation.ui.wizard.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class BaseLookupService<T> : EntityLookupService<T> {

    override suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String?,
        onResult: (found: Boolean, data: T?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            if (expectedBarcode != null && barcode != expectedBarcode) {
                onResult(false, null)
                return
            }

            withContext(Dispatchers.IO) {
                val entityFromContext = findEntityInContext(barcode)
                if (entityFromContext != null) {
                    onResult(true, entityFromContext)
                    return@withContext
                }

                val entityFromRepository = findEntityInRepository(barcode)
                if (entityFromRepository != null) {
                    onResult(true, entityFromRepository)
                    return@withContext
                }

                val localEntity = createLocalEntity(barcode)
                if (localEntity != null) {
                    onResult(true, localEntity)
                    return@withContext
                }

                onResult(false, null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
            onError("Ошибка при обработке штрихкода: ${e.message}")
        }
    }

    override suspend fun searchEntities(query: String, additionalParams: Map<String, Any>): List<T> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<T>()

                val entitiesFromContext = searchEntitiesInContext(query, additionalParams)
                results.addAll(entitiesFromContext)

                if (results.size >= getMaxSearchResults()) {
                    return@withContext results.take(getMaxSearchResults())
                }

                val entitiesFromRepository = searchEntitiesInRepository(query, additionalParams)

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

    protected abstract suspend fun findEntityInContext(barcode: String): T?

    protected abstract suspend fun findEntityInRepository(barcode: String): T?

    protected abstract suspend fun createLocalEntity(barcode: String): T?

    protected abstract suspend fun searchEntitiesInContext(
        query: String,
        additionalParams: Map<String, Any>
    ): List<T>

    protected abstract suspend fun searchEntitiesInRepository(
        query: String,
        additionalParams: Map<String, Any>
    ): List<T>

    protected abstract fun getEntityId(entity: T): String?

    protected open fun getMaxSearchResults(): Int = 20
}