package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import timber.log.Timber

/**
 * Сервис для поиска ячеек с использованием унифицированного интерфейса.
 * Обеспечивает поиск по штрихкоду и строковому запросу.
 */
class BinLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardBinRepository: WizardBinRepository? = null
) : BaseLookupService<BinX>() {

    override suspend fun findEntityInContext(barcode: String): BinX? {
        val currentTask = taskContextManager.lastStartedTaskX.value ?: return null

        // Ищем ячейку в контексте текущей задачи
        return currentTask.plannedActions
            .mapNotNull { it.placementBin }
            .distinct()
            .firstOrNull { it.code == barcode }
    }

    override suspend fun findEntityInRepository(barcode: String): BinX? {
        return wizardBinRepository?.getBinByCode(barcode)
    }

    override suspend fun createLocalEntity(barcode: String): BinX {
        // Создаем временную локальную ячейку по коду
        return BinX(
            code = barcode,
            zone = "Неизвестная зона"
        )
    }

    override suspend fun searchEntitiesInContext(
        query: String,
        additionalParams: Map<String, Any>
    ): List<BinX> {
        val currentTask = taskContextManager.lastStartedTaskX.value ?: return emptyList()
        val zoneFilter = additionalParams["zoneFilter"] as? String

        // Получаем все ячейки из текущей задачи
        val taskBins = currentTask.plannedActions
            .mapNotNull { it.placementBin }
            .distinctBy { it.code }

        // Фильтруем по запросу и зоне
        return taskBins.filter { bin ->
            (query.isEmpty() || bin.code.contains(query, ignoreCase = true)) &&
                    (zoneFilter == null || bin.zone == zoneFilter)
        }
    }

    override suspend fun searchEntitiesInRepository(
        query: String,
        additionalParams: Map<String, Any>
    ): List<BinX> {
        if (wizardBinRepository == null) return emptyList()

        val zoneFilter = additionalParams["zoneFilter"] as? String

        try {
            return wizardBinRepository.getBins(query, zoneFilter)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячеек: query=$query, zone=$zoneFilter")
            return emptyList()
        }
    }

    override fun getEntityId(entity: BinX): String? {
        return entity.code
    }
}