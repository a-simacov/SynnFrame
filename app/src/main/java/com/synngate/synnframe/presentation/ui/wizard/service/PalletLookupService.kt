package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PalletLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardPalletRepository: WizardPalletRepository? = null
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
                    val taskPallets = currentTask.plannedActions.mapNotNull {
                        listOfNotNull(it.storagePallet, it.placementPallet)
                    }.flatten().distinctBy { it.code }

                    val matchingPallet = taskPallets.firstOrNull { it.code == barcode }
                    if (matchingPallet != null) {
                        onResult(true, matchingPallet)
                        return@withContext
                    }
                }

                if (wizardPalletRepository != null) {
                    val pallet = wizardPalletRepository.getPalletByCode(barcode)
                    if (pallet != null) {
                        onResult(true, pallet)
                        return@withContext
                    }
                }

                val temporaryPallet = createLocalPallet(barcode)
                onResult(true, temporaryPallet)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллеты по коду: $barcode")
            onError("Ошибка при поиске паллеты: ${e.message}")
        }
    }

    suspend fun searchPallets(query: String): List<Pallet> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<Pallet>()

                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    val taskPallets = currentTask.plannedActions.mapNotNull {
                        listOfNotNull(it.storagePallet, it.placementPallet)
                    }.flatten().distinctBy { it.code }

                    val filteredTaskPallets = if (query.isNotEmpty()) {
                        taskPallets.filter { it.code.contains(query, ignoreCase = true) }
                    } else {
                        taskPallets
                    }

                    results.addAll(filteredTaskPallets)
                }

                if (wizardPalletRepository != null && results.size < 10) {
                    val repoResults = wizardPalletRepository.getPallets(query)

                    val existingCodes = results.map { it.code }.toSet()
                    val newPallets = repoResults.filter { it.code !in existingCodes }

                    results.addAll(newPallets)
                }

                results
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске паллет: query=$query")
                emptyList()
            }
        }
    }

    suspend fun createNewPallet(): Result<Pallet> {
        return withContext(Dispatchers.IO) {
            try {
                if (wizardPalletRepository == null) {
                    val newCode = "PAL${System.currentTimeMillis()}"
                    val pallet = createLocalPallet(newCode)
                    return@withContext Result.success(pallet)
                }

                wizardPalletRepository.createPallet()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании новой паллеты")
                Result.failure(e)
            }
        }
    }

    private fun createLocalPallet(code: String): Pallet {
        return Pallet(
            code = code,
            isClosed = false
        )
    }
}