package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import timber.log.Timber

class BinLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardBinRepository: WizardBinRepository? = null
) : BaseLookupService<BinX>() {

    override suspend fun findEntityInContext(barcode: String): BinX? {
        val currentTask = taskContextManager.lastStartedTaskX.value ?: return null

        return currentTask.plannedActions
            .mapNotNull { it.placementBin }
            .distinct()
            .firstOrNull { it.code == barcode }
    }

    override suspend fun findEntityInRepository(barcode: String): BinX? {
        return wizardBinRepository?.getBinByCode(barcode)
    }

    override suspend fun createLocalEntity(barcode: String): BinX {
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

        val taskBins = currentTask.plannedActions
            .mapNotNull { it.placementBin }
            .distinctBy { it.code }

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