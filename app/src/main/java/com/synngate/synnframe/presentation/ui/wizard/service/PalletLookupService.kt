package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import timber.log.Timber

class PalletLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardPalletRepository: WizardPalletRepository? = null
) : BaseLookupService<Pallet>() {

    suspend fun createNewPallet(): Result<Pallet> {
        try {
            if (wizardPalletRepository == null) {
                val newCode = "PAL${System.currentTimeMillis()}"
                val pallet = createLocalEntity(newCode)
                return Result.success(pallet)
            }

            return wizardPalletRepository.createPallet()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании новой паллеты")
            return Result.failure(e)
        }
    }

    override suspend fun findEntityInContext(barcode: String): Pallet? {
        val currentTask = taskContextManager.lastStartedTaskX.value
        if (currentTask == null) {
            return null
        }

        val pallets = currentTask.plannedActions
            .flatMap {
                listOfNotNull(it.storagePallet, it.placementPallet)
            }
            .distinctBy { it.code }


        val result = pallets.firstOrNull { it.code == barcode }

        return result
    }

    override suspend fun findEntityInRepository(barcode: String): Pallet? {
        return wizardPalletRepository?.getPalletByCode(barcode)
    }

    override suspend fun createLocalEntity(barcode: String): Pallet {
        return Pallet(
            code = barcode,
            isClosed = false
        )
    }

    override suspend fun searchEntitiesInContext(
        query: String,
        additionalParams: Map<String, Any>
    ): List<Pallet> {
        val currentTask = taskContextManager.lastStartedTaskX.value ?: return emptyList()

        val taskPallets = currentTask.plannedActions
            .flatMap {
                listOfNotNull(it.storagePallet, it.placementPallet)
            }
            .distinctBy { it.code }

        return if (query.isEmpty()) {
            taskPallets
        } else {
            taskPallets.filter { it.code.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun searchEntitiesInRepository(
        query: String,
        additionalParams: Map<String, Any>
    ): List<Pallet> {
        if (wizardPalletRepository == null) return emptyList()

        try {
            return wizardPalletRepository.getPallets(query)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллет: query=$query")
            return emptyList()
        }
    }

    override fun getEntityId(entity: Pallet): String? {
        return entity.code
    }
}