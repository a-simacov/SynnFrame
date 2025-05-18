package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import timber.log.Timber

/**
 * Сервис для поиска паллет с использованием унифицированного интерфейса.
 * Обеспечивает поиск по штрихкоду и строковому запросу.
 */
class PalletLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardPalletRepository: WizardPalletRepository? = null
) : BaseLookupService<Pallet>() {

    /**
     * Создает новую паллету через репозиторий или генерирует временную.
     */
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
            Timber.d("PalletLookupService: задача не найдена в контексте")
            return null
        }

        // Собираем все паллеты из задачи
        val pallets = currentTask.plannedActions
            .flatMap {
                listOfNotNull(it.storagePallet, it.placementPallet)
            }
            .distinctBy { it.code }

        Timber.d("PalletLookupService: найдено ${pallets.size} паллет в контексте")

        // Ищем паллету с соответствующим кодом
        val result = pallets.firstOrNull { it.code == barcode }
        if (result != null) {
            Timber.d("PalletLookupService: найдена паллета в контексте с кодом ${result.code}")
        } else {
            Timber.d("PalletLookupService: паллета с кодом $barcode не найдена в контексте")
        }

        return result
    }

    override suspend fun findEntityInRepository(barcode: String): Pallet? {
        return wizardPalletRepository?.getPalletByCode(barcode)
    }

    override suspend fun createLocalEntity(barcode: String): Pallet {
        // Создаем временную локальную паллету
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

        // Собираем все паллеты из задачи
        val taskPallets = currentTask.plannedActions
            .flatMap {
                listOfNotNull(it.storagePallet, it.placementPallet)
            }
            .distinctBy { it.code }

        // Фильтруем по запросу
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