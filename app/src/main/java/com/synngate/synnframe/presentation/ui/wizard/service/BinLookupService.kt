package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class BinLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardBinRepository: WizardBinRepository? = null
) : BaseBarcodeScanningService() {

    override suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    val taskBins = currentTask.plannedActions.mapNotNull {
                        it.placementBin
                    }.distinctBy { it.code }

                    val matchingBin = taskBins.firstOrNull { it.code == barcode }
                    if (matchingBin != null) {
                        onResult(true, matchingBin)
                        return@withContext
                    }
                }

                if (wizardBinRepository != null) {
                    val bin = wizardBinRepository.getBinByCode(barcode)
                    if (bin != null) {
                        onResult(true, bin)
                        return@withContext
                    }
                }

                val temporaryBin = createLocalBin(barcode)
                onResult(true, temporaryBin)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячейки по коду: $barcode")
            onError("Ошибка при поиске ячейки: ${e.message}")
        }
    }

    suspend fun searchBins(query: String, zoneFilter: String? = null): List<BinX> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<BinX>()

                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    val taskBins = currentTask.plannedActions.mapNotNull {
                        it.placementBin
                    }.distinctBy { it.code }

                    val filteredTaskBins = taskBins.filter { bin ->
                        (query.isEmpty() || bin.code.contains(query, ignoreCase = true)) &&
                                (zoneFilter == null || bin.zone == zoneFilter)
                    }

                    results.addAll(filteredTaskBins)
                }

                if (wizardBinRepository != null && results.size < 10) {
                    val repoResults = wizardBinRepository.getBins(query, zoneFilter)

                    val existingCodes = results.map { it.code }.toSet()
                    val newBins = repoResults.filter { it.code !in existingCodes }

                    results.addAll(newBins)
                }

                results
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске ячеек: query=$query, zone=$zoneFilter")
                emptyList()
            }
        }
    }

    private fun createLocalBin(code: String): BinX {
        return BinX(
            code = code,
            zone = "Неизвестная зона"
        )
    }
}