package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Сервис для поиска паллет
 */
class PalletLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardPalletRepository: WizardPalletRepository? = null
) : BaseBarcodeScanningService() {

    /**
     * Находит паллету по штрих-коду
     * @param barcode штрих-код паллеты
     * @param onResult обработчик результата
     * @param onError обработчик ошибок
     */
    override suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                // Сначала ищем в текущем задании
                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    // Ищем среди паллет в задании
                    val taskPallets = currentTask.plannedActions.mapNotNull {
                        listOfNotNull(it.storagePallet, it.placementPallet)
                    }.flatten().distinctBy { it.code }

                    val matchingPallet = taskPallets.firstOrNull { it.code == barcode }
                    if (matchingPallet != null) {
                        Timber.d("Паллета найдена в задании: ${matchingPallet.code}")
                        onResult(true, matchingPallet)
                        return@withContext
                    }
                }

                // Затем пробуем найти через репозиторий, если он предоставлен
                if (wizardPalletRepository != null) {
                    val pallet = wizardPalletRepository.getPalletByCode(barcode)
                    if (pallet != null) {
                        Timber.d("Паллета найдена через репозиторий: ${pallet.code}")
                        onResult(true, pallet)
                        return@withContext
                    }
                }

                // Если не найдена, создаем временную
                val temporaryPallet = createLocalPallet(barcode)
                Timber.d("Создана временная паллета: $barcode")
                onResult(true, temporaryPallet)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллеты по коду: $barcode")
            onError("Ошибка при поиске паллеты: ${e.message}")
        }
    }

    /**
     * Поиск паллет по текстовому запросу
     * @param query текстовый запрос для поиска
     * @return список паллет, соответствующих запросу
     */
    suspend fun searchPallets(query: String): List<Pallet> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<Pallet>()

                // Сначала ищем в текущем задании
                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    // Ищем среди паллет в задании
                    val taskPallets = currentTask.plannedActions.mapNotNull {
                        listOfNotNull(it.storagePallet, it.placementPallet)
                    }.flatten().distinctBy { it.code }

                    // Фильтруем по запросу, если он не пустой
                    val filteredTaskPallets = if (query.isNotEmpty()) {
                        taskPallets.filter { it.code.contains(query, ignoreCase = true) }
                    } else {
                        taskPallets
                    }

                    results.addAll(filteredTaskPallets)
                }

                // Если репозиторий доступен и результатов нет или мало,
                // дополнительно используем репозиторий
                if (wizardPalletRepository != null && results.size < 10) {
                    val repoResults = wizardPalletRepository.getPallets(query)

                    // Добавляем только те паллеты, которых еще нет в результатах
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

    /**
     * Создание новой паллеты через репозиторий
     * @return созданная паллета или null в случае ошибки
     */
    suspend fun createNewPallet(): Result<Pallet> {
        return withContext(Dispatchers.IO) {
            try {
                // Если репозиторий не предоставлен, создаем временную паллету
                if (wizardPalletRepository == null) {
                    val newCode = "PAL${System.currentTimeMillis()}"
                    val pallet = createLocalPallet(newCode)
                    return@withContext Result.success(pallet)
                }

                // Иначе используем репозиторий для создания
                wizardPalletRepository.createPallet()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании новой паллеты")
                Result.failure(e)
            }
        }
    }

    /**
     * Создание локальной паллеты (без сохранения в репозиторий)
     */
    private fun createLocalPallet(code: String): Pallet {
        return Pallet(
            code = code,
            isClosed = false
        )
    }
}